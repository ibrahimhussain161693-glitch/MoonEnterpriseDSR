package com.moonenterprise.dsrapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.moonenterprise.dsrapp.dsr.DSRDashboardActivity;
import com.moonenterprise.dsrapp.sr.SRDashboardActivity;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private Button btnLogin;
    private TextView tvServerConfig;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);

        // Check Permissions
        checkPermissions();

        // Check existing login session
        if (session.isLoggedIn()) {
            routeUserByRole(session.getUserRole());
            return;
        }

        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvServerConfig = findViewById(R.id.tvServerConfig);

        btnLogin.setOnClickListener(v -> performLogin());
        tvServerConfig.setOnClickListener(v -> showServerConfigDialog());
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, 101);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, 101);
            }
        }
    }

    private void performLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "মোবাইল নম্বর ও পাসওয়ার্ড দিন!", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = session.getBaseUrl() + "/api/login";

        try {
            JSONObject body = new JSONObject();
            body.put("phone", phone);
            body.put("password", password);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject user = response.getJSONObject("user");
                                int id = user.getInt("id");
                                String name = user.getString("name");
                                String userPhone = user.getString("phone");
                                String role = user.getString("role");

                                session.saveUserSession(id, name, userPhone, role);
                                Toast.makeText(LoginActivity.this, "Login Successful! Welcome " + name, Toast.LENGTH_SHORT).show();
                                routeUserByRole(role);
                            } else {
                                Toast.makeText(LoginActivity.this, response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        String errorMsg = "কানেকশন এরর!\nURL: " + url + "\nমেসেজ: ";
                        if (error.networkResponse != null) {
                            errorMsg += "HTTP Code " + error.networkResponse.statusCode;
                        } else {
                            errorMsg += "সার্ভার রেসপন্স করছে না। ফোন ও ম্যাকবুক একই Wi-Fi তে আছে কিনা চেক করুন।";
                        }
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
            );
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void routeUserByRole(String role) {
        Intent intent;
        if ("SR".equalsIgnoreCase(role)) {
            intent = new Intent(this, SRDashboardActivity.class);
        } else if ("DSR".equalsIgnoreCase(role)) {
            intent = new Intent(this, DSRDashboardActivity.class);
        } else {
            intent = new Intent(this, SRDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MacBook Local Server IP");

        final EditText input = new EditText(this);
        input.setHint("e.g. 192.168.0.105 or 10.0.2.2");
        input.setText(session.getServerIp());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String ip = input.getText().toString().trim();
            if (!ip.isEmpty()) {
                session.setServerIp(ip);
                Toast.makeText(LoginActivity.this, "Server URL set to: " + session.getBaseUrl(), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
