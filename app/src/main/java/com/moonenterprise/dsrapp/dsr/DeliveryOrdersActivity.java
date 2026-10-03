package com.moonenterprise.dsrapp.dsr;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class DeliveryOrdersActivity extends AppCompatActivity {

    private LinearLayout llOrdersContainer;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_orders);

        session = new SessionManager(this);
        llOrdersContainer = findViewById(R.id.llOrdersContainer);

        loadTodayDeliveries();
    }

    private void loadTodayDeliveries() {
        String url = session.getBaseUrl() + "/api/orders/dsr/" + session.getUserId() + "/today";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    llOrdersContainer.removeAllViews();
                    if (response.length() == 0) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("আজকের দিনে আপনার কোনো ডেলিভারি অর্ডারের তালিকা নেই।");
                        tvEmpty.setTextSize(16);
                        tvEmpty.setPadding(16, 16, 16, 16);
                        llOrdersContainer.addView(tvEmpty);
                        return;
                    }

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject order = response.getJSONObject(i);
                            addOrderCard(order);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "Deliveries fetch failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void addOrderCard(JSONObject order) throws JSONException {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(4);
        card.setPadding(24, 24, 24, 24);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);

        String shopName = order.getString("shop_name");
        String shopAddress = order.optString("shop_address", "Tongi, Gazipur");
        double totalBill = order.getDouble("total_bill");
        double discount = order.getDouble("discount");
        double cash = order.getDouble("cash_collected");
        double due = order.getDouble("due_amount");
        double lat = order.optDouble("shop_lat", 23.8920);
        double lng = order.optDouble("shop_lng", 90.4020);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🏢 " + shopName);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(0xFF1A237E);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("📍 " + shopAddress + "\n" +
                "• Total Bill: Tk. " + totalBill + " (Disc: Tk. " + discount + ")\n" +
                "• Cash Collected: Tk. " + cash + "\n" +
                "• Remaining Due: Tk. " + due);
        tvDetails.setTextSize(14);
        tvDetails.setTextColor(0xFF333333);
        tvDetails.setPadding(0, 12, 0, 16);

        Button btnMap = new Button(this);
        btnMap.setText("🗺️ Open Google Maps Route");
        btnMap.setBackgroundColor(0xFF00838F);
        btnMap.setTextColor(0xFFFFFFFF);

        btnMap.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng));
                startActivity(webMapIntent);
            }
        });

        card.addView(tvTitle);
        card.addView(tvDetails);
        card.addView(btnMap);

        llOrdersContainer.addView(card);
    }
}
