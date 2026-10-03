package com.moonenterprise.dsrapp.dsr;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DigitalLedgerActivity extends AppCompatActivity {

    private Spinner spShops;
    private RadioGroup rgFilters;
    private TextView tvCurrentDue, tvLedgerHistory;
    private EditText etCollectAmount;
    private Button btnSaveCollection, btnPrintReceipt;

    private SessionManager session;
    private List<JSONObject> shopList = new ArrayList<>();
    private String currentFilter = "ALL";
    private int selectedShopId = -1;
    private String selectedShopName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_ledger);

        session = new SessionManager(this);

        spShops = findViewById(R.id.spShops);
        rgFilters = findViewById(R.id.rgFilters);
        tvCurrentDue = findViewById(R.id.tvCurrentDue);
        tvLedgerHistory = findViewById(R.id.tvLedgerHistory);
        etCollectAmount = findViewById(R.id.etCollectAmount);
        btnSaveCollection = findViewById(R.id.btnSaveCollection);
        btnPrintReceipt = findViewById(R.id.btnPrintReceipt);

        loadShops();

        spShops.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < shopList.size()) {
                    try {
                        JSONObject shop = shopList.get(position);
                        selectedShopId = shop.getInt("id");
                        selectedShopName = shop.getString("name");
                        loadLedgerHistory();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        rgFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPurchase) currentFilter = "PURCHASE";
            else if (checkedId == R.id.rbPayment) currentFilter = "PAYMENT";
            else if (checkedId == R.id.rbDiscount) currentFilter = "DISCOUNT";
            else currentFilter = "ALL";

            loadLedgerHistory();
        });

        btnSaveCollection.setOnClickListener(v -> saveCollection());
        btnPrintReceipt.setOnClickListener(v -> printThermalReceipt());
    }

    private void loadShops() {
        String url = session.getBaseUrl() + "/api/shops";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    shopList.clear();
                    List<String> shopNames = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject shop = response.getJSONObject(i);
                            shopList.add(shop);
                            shopNames.add(shop.getString("name"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shopNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spShops.setAdapter(adapter);
                },
                error -> Toast.makeText(this, "Shops fetch failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void loadLedgerHistory() {
        if (selectedShopId == -1) return;

        String url = session.getBaseUrl() + "/api/shops/" + selectedShopId + "/ledger?filter=" + currentFilter;
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double currentDue = response.getDouble("current_due");
                        tvCurrentDue.setText("Current Due: Tk. " + currentDue);

                        JSONArray txs = response.getJSONArray("transactions");
                        if (txs.length() == 0) {
                            tvLedgerHistory.setText("No transactions found for this filter.");
                            return;
                        }

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < txs.length(); i++) {
                            JSONObject tx = txs.getJSONObject(i);
                            String type = tx.getString("type");
                            double amount = tx.getDouble("amount");
                            String date = tx.getString("date");

                            if ("PURCHASE".equalsIgnoreCase(type)) {
                                double disc = tx.optDouble("discount", 0);
                                sb.append("🛒 PURCHASE | ").append(date).append("\n")
                                  .append("   Amount: Tk. ").append(amount)
                                  .append(" (Discount: Tk. ").append(disc).append(")\n\n");
                            } else if ("PAYMENT".equalsIgnoreCase(type)) {
                                sb.append("💵 PAYMENT | ").append(date).append("\n")
                                  .append("   Amount Paid: Tk. ").append(amount).append("\n\n");
                            }
                        }
                        tvLedgerHistory.setText(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Ledger fetch failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void saveCollection() {
        String amountStr = etCollectAmount.getText().toString().trim();
        if (selectedShopId == -1 || amountStr.isEmpty()) {
            Toast.makeText(this, "দোকান সিলেক্ট করুন ও জমার পরিমাণ লিখুন!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String url = session.getBaseUrl() + "/api/collections";

        try {
            JSONObject body = new JSONObject();
            body.put("shop_id", selectedShopId);
            body.put("dsr_id", session.getUserId());
            body.put("dsr_name", session.getUserName());
            body.put("amount", amount);
            body.put("payment_type", "CASH");

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        Toast.makeText(DigitalLedgerActivity.this, "Payment collected & saved!", Toast.LENGTH_SHORT).show();
                        etCollectAmount.setText("");
                        loadLedgerHistory();
                    },
                    error -> Toast.makeText(DigitalLedgerActivity.this, "Collection save failed!", Toast.LENGTH_SHORT).show()
            );
            queue.add(req);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void printThermalReceipt() {
        try {
            EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());

            String receiptText =
                    "[C]<b>MOON ENTERPRISE</b>\n" +
                            "[C]172, Shar-Bangla Road, Modho Arichpur Tongi-Gazipur\n" +
                            "[C]Cell: 01950000909\n" +
                            "[C]--------------------------------\n" +
                            "[L]Date: " + date + "\n" +
                            "[L]DSR: " + session.getUserName() + "\n" +
                            "[L]Shop: " + selectedShopName + "\n" +
                            "[C]--------------------------------\n" +
                            "[L]<b>" + tvCurrentDue.getText().toString() + "</b>\n" +
                            "[C]--------------------------------\n" +
                            "[C]Thank You For Your Business!\n\n\n";

            printer.printFormattedText(receiptText);

        } catch (Exception e) {
            Toast.makeText(this, "প্রিন্টার কানেক্ট করা নেই বা পেয়ার করা হয়নি!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
