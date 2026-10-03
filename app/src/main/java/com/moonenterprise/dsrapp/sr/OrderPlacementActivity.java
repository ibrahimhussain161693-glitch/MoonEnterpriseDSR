package com.moonenterprise.dsrapp.sr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.moonenterprise.dsrapp.R;
import com.moonenterprise.dsrapp.utils.GeoFenceHelper;
import com.moonenterprise.dsrapp.utils.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OrderPlacementActivity extends AppCompatActivity {

    private Spinner spShops, spDsrs;
    private EditText etConeQty, etMalaiQty, etStrawberryQty, etDiscount, etCash;
    private TextView tvOfferText, tvGeoStatus;
    private Button btnSubmitOrder;

    private SessionManager session;
    private LocationManager locationManager;

    private List<JSONObject> shopList = new ArrayList<>();
    private List<JSONObject> dsrList = new ArrayList<>();

    private double currentSrLat = 0.0;
    private double currentSrLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_placement);

        session = new SessionManager(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        spShops = findViewById(R.id.spShops);
        spDsrs = findViewById(R.id.spDsrs);

        etConeQty = findViewById(R.id.etConeQty);
        etMalaiQty = findViewById(R.id.etMalaiQty);
        etStrawberryQty = findViewById(R.id.etStrawberryQty);
        etDiscount = findViewById(R.id.etDiscount);
        etCash = findViewById(R.id.etCash);

        tvOfferText = findViewById(R.id.tvOfferText);
        tvGeoStatus = findViewById(R.id.tvGeoStatus);
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder);

        captureSrLocation();

        loadShops();
        loadDsrs();
        loadDiscounts();

        btnSubmitOrder.setOnClickListener(v -> submitOrder());
    }

    private void captureSrLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                currentSrLat = loc.getLatitude();
                currentSrLng = loc.getLongitude();
                tvGeoStatus.setText("GPS Ready (Lat: " + String.format("%.4f", currentSrLat) + ", Lng: " + String.format("%.4f", currentSrLng) + ")");
            }
        }
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
                            shopNames.add(shop.getString("name") + " (" + shop.getString("address") + ")");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shopNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spShops.setAdapter(adapter);
                },
                error -> Toast.makeText(this, "Shops loading failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void loadDsrs() {
        String url = session.getBaseUrl() + "/api/dsrs";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    dsrList.clear();
                    List<String> dsrNames = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject dsr = response.getJSONObject(i);
                            dsrList.add(dsr);
                            dsrNames.add(dsr.getString("name"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dsrNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spDsrs.setAdapter(adapter);
                },
                error -> Toast.makeText(this, "DSRs loading failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }

    private void loadDiscounts() {
        String url = session.getBaseUrl() + "/api/discounts";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() > 0) {
                        try {
                            JSONObject disc = response.getJSONObject(0);
                            tvOfferText.setText("Active Offer: " + disc.getString("title") + " (" + disc.getString("gift_description") + ")");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        tvOfferText.setText("Active Offer: No special offer today.");
                    }
                },
                error -> tvOfferText.setText("Active Offer: Standard pricing.")
        );
        queue.add(req);
    }

    private void submitOrder() {
        if (shopList.isEmpty() || dsrList.isEmpty()) {
            Toast.makeText(this, "Shop or DSR data not loaded yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        int shopIndex = spShops.getSelectedItemPosition();
        int dsrIndex = spDsrs.getSelectedItemPosition();

        JSONObject selectedShop = shopList.get(shopIndex);
        JSONObject selectedDsr = dsrList.get(dsrIndex);

        int coneQty = etConeQty.getText().toString().isEmpty() ? 0 : Integer.parseInt(etConeQty.getText().toString());
        int malaiQty = etMalaiQty.getText().toString().isEmpty() ? 0 : Integer.parseInt(etMalaiQty.getText().toString());
        int strawberryQty = etStrawberryQty.getText().toString().isEmpty() ? 0 : Integer.parseInt(etStrawberryQty.getText().toString());

        double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString());
        double cash = etCash.getText().toString().isEmpty() ? 0 : Double.parseDouble(etCash.getText().toString());

        double totalBill = (coneQty * 35) + (malaiQty * 25) + (strawberryQty * 120);

        if (totalBill <= 0) {
            Toast.makeText(this, "কমপক্ষে ১টি আইটেমের পরিমাণ দিন!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int shopId = selectedShop.getInt("id");
            double shopLat = selectedShop.getDouble("lat");
            double shopLng = selectedShop.getDouble("lng");
            int dsrId = selectedDsr.getInt("id");

            // Geo-Fence Check (50 meters)
            float distMeters = GeoFenceHelper.calculateDistanceMeters(currentSrLat, currentSrLng, shopLat, shopLng);

            JSONObject body = new JSONObject();
            body.put("shop_id", shopId);
            body.put("sr_id", session.getUserId());
            body.put("sr_name", session.getUserName());
            body.put("dsr_id", dsrId);
            body.put("total_bill", totalBill);
            body.put("discount", discount);
            body.put("free_gifts", totalBill >= 5000 ? "5 Free Cones" : "None");
            body.put("cash_collected", cash);
            body.put("sr_lat", currentSrLat);
            body.put("sr_lng", currentSrLng);

            JSONArray items = new JSONArray();

            if (coneQty > 0) {
                JSONObject item1 = new JSONObject();
                item1.put("product_id", 1);
                item1.put("product_name", "Cone Ice Cream");
                item1.put("cartons", coneQty / 24);
                item1.put("pieces", coneQty);
                item1.put("price", 35);
                items.put(item1);
            }
            if (malaiQty > 0) {
                JSONObject item2 = new JSONObject();
                item2.put("product_id", 2);
                item2.put("product_name", "Malai Choco Bar");
                item2.put("cartons", malaiQty / 30);
                item2.put("pieces", malaiQty);
                item2.put("price", 25);
                items.put(item2);
            }
            if (strawberryQty > 0) {
                JSONObject item3 = new JSONObject();
                item3.put("product_id", 3);
                item3.put("product_name", "Strawberry Box 500ml");
                item3.put("cartons", strawberryQty / 12);
                item3.put("pieces", strawberryQty);
                item3.put("price", 120);
                items.put(item3);
            }

            body.put("items", items);

            String url = session.getBaseUrl() + "/api/orders";
            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        try {
                            String msg = response.getString("message");
                            Toast.makeText(OrderPlacementActivity.this, msg, Toast.LENGTH_LONG).show();
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Toast.makeText(OrderPlacementActivity.this, "Order placement failed! Check server connection.", Toast.LENGTH_SHORT).show()
            );
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
