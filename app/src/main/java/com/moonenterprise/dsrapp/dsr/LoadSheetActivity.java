package com.moonenterprise.dsrapp.dsr;

import android.os.Bundle;
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

public class LoadSheetActivity extends AppCompatActivity {

    private TextView tvLoadSheetDetails;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_sheet);

        session = new SessionManager(this);
        tvLoadSheetDetails = findViewById(R.id.tvLoadSheetDetails);

        loadAggregatedLoadSheet();
    }

    private void loadAggregatedLoadSheet() {
        String url = session.getBaseUrl() + "/api/orders/dsr/" + session.getUserId() + "/load-sheet";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 0) {
                        tvLoadSheetDetails.setText("আজকের দিনে আপনার কোনো ডেলিভারির লোড শিট পাওয়া যায়নি।");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("📋 আজ ভ্যানে লোড করার মোট মালামাল:\n\n");

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject item = response.getJSONObject(i);
                            String pName = item.getString("product_name");
                            int cartons = item.optInt("total_cartons", 0);
                            int pieces = item.optInt("total_pieces", 0);

                            sb.append("🔹 ").append(pName).append(":\n")
                              .append("   - Total Pieces: ").append(pieces).append(" টি\n")
                              .append("   - Approx Cartons: ").append(cartons).append(" কার্টন\n\n");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    tvLoadSheetDetails.setText(sb.toString());
                },
                error -> Toast.makeText(this, "Load sheet fetch failed!", Toast.LENGTH_SHORT).show()
        );
        queue.add(req);
    }
}
