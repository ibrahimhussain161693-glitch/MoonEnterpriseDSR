package com.moonenterprise.dsrapp.sr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.moonenterprise.dsrapp.LoginActivity;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SRDashboardActivity extends AppCompatActivity {

    private TextView tvSrInfo, tvStockList;
    private Button btnRegisterShop, btnPlaceOrder, btnLogout;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sr_dashboard);

        session = new SessionManager(this);

        tvSrInfo = findViewById(R.id.tvSrInfo);
        tvStockList = findViewById(R.id.tvStockList);
        btnRegisterShop = findViewById(R.id.btnRegisterShop);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnLogout = findViewById(R.id.btnLogout);

        tvSrInfo.setText("SR: " + session.getUserName() + " (" + session.getUserPhone() + ")");

        btnRegisterShop.setOnClickListener(v -> startActivity(new Intent(this, ShopRegistrationActivity.class)));
        btnPlaceOrder.setOnClickListener(v -> startActivity(new Intent(this, OrderPlacementActivity.class)));
        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadFactoryStock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFactoryStock();
    }

    private void loadFactoryStock() {
        String url = session.getBaseUrl() + "/api/products";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject item = response.getJSONObject(i);
                            sb.append("• ").append(item.getString("name"))
                              .append(": ").append(item.getInt("stock_cartons")).append(" Cartons, ")
                              .append(item.getInt("stock_pieces")).append(" Pieces\n");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    tvStockList.setText(sb.length() > 0 ? sb.toString() : "No product stock available.");
                },
                error -> Toast.makeText(SRDashboardActivity.this, "Stock fetch failed! IP: " + session.getServerIp(), Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }
}
