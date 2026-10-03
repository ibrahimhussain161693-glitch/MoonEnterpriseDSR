package com.moonenterprise.dsrapp.sr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class ShopRegistrationActivity extends AppCompatActivity {

    private EditText etShopName, etOwnerName, etPhone, etAddress;
    private Button btnGetLocation, btnSaveShop;
    private TextView tvLocationDisplay;

    private double capturedLat = 0.0;
    private double capturedLng = 0.0;

    private SessionManager session;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_registration);

        session = new SessionManager(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        etShopName = findViewById(R.id.etShopName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSaveShop = findViewById(R.id.btnSaveShop);
        tvLocationDisplay = findViewById(R.id.tvLocationDisplay);

        btnGetLocation.setOnClickListener(v -> captureGpsLocation());
        btnSaveShop.setOnClickListener(v -> saveShopToBackend());

        // Auto attempt initial location fetch
        captureGpsLocation();
    }

    private void captureGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            return;
        }

        try {
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnown != null) {
                capturedLat = lastKnown.getLatitude();
                capturedLng = lastKnown.getLongitude();
                tvLocationDisplay.setText("Location Captured: Lat " + String.format("%.5f", capturedLat) + ", Lng " + String.format("%.5f", capturedLng));
            } else {
                tvLocationDisplay.setText("Fetching GPS Location...");
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        capturedLat = location.getLatitude();
                        capturedLng = location.getLongitude();
                        tvLocationDisplay.setText("Location Captured: Lat " + String.format("%.5f", capturedLat) + ", Lng " + String.format("%.5f", capturedLng));
                    }
                }, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvLocationDisplay.setText("GPS Location Error!");
        }
    }

    private void saveShopToBackend() {
        String name = etShopName.getText().toString().trim();
        String owner = etOwnerName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "দোকানের নাম ও ফোন নম্বর দিন!", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = session.getBaseUrl() + "/api/shops";

        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("owner_name", owner);
            body.put("phone", phone);
            body.put("address", address);
            body.put("lat", capturedLat);
            body.put("lng", capturedLng);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        Toast.makeText(ShopRegistrationActivity.this, "Shop Registered & Geo-Tagged Successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    },
                    error -> Toast.makeText(ShopRegistrationActivity.this, "Save failed! Check server connection.", Toast.LENGTH_SHORT).show()
            );
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
