package com.moonenterprise.dsrapp.dsr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.moonenterprise.dsrapp.LoginActivity;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.sr.OrderPlacementActivity;
import com.moonenterprise.dsrapp.utils.SessionManager;

public class DSRDashboardActivity extends AppCompatActivity {

    private TextView tvDsrInfo;
    private Button btnLoadSheet, btnDeliveryOrders, btnDirectMemo, btnDigitalLedger, btnLogout;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dsr_dashboard);

        session = new SessionManager(this);

        tvDsrInfo = findViewById(R.id.tvDsrInfo);
        btnLoadSheet = findViewById(R.id.btnLoadSheet);
        btnDeliveryOrders = findViewById(R.id.btnDeliveryOrders);
        btnDirectMemo = findViewById(R.id.btnDirectMemo);
        btnDigitalLedger = findViewById(R.id.btnDigitalLedger);
        btnLogout = findViewById(R.id.btnLogout);

        tvDsrInfo.setText("DSR: " + session.getUserName() + " (" + session.getUserPhone() + ")");

        btnLoadSheet.setOnClickListener(v -> startActivity(new Intent(this, LoadSheetActivity.class)));
        btnDeliveryOrders.setOnClickListener(v -> startActivity(new Intent(this, DeliveryOrdersActivity.class)));
        btnDirectMemo.setOnClickListener(v -> startActivity(new Intent(this, OrderPlacementActivity.class)));
        btnDigitalLedger.setOnClickListener(v -> startActivity(new Intent(this, DigitalLedgerActivity.class)));

        btnLogout.setOnClickListener(v -> {
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
