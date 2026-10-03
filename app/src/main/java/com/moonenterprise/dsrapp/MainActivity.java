package com.moonenterprise.dsrapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etShopName, etShopPhone, etProductsSummary, etTotalBill, etDiscount, etFreeGifts, etCashCollected;
    private TextView tvGpsStatus, tvAdminOfferNote;
    private Button btnSubmit, btnPrintReceipt, btnSendWhatsapp;

    private static final String COMPANY_NAME = "MOON ENTERPRISE";
    private static final String COMPANY_ADDRESS = "172, Shar-Bangla Road, Modho Arichpur Tongi-Gazipur";
    private static final String COMPANY_PHONE = "01950000909";

    private String dsrName = "Rahim Ahmed (DSR)";
    private String dsrPhone = "01800000000";

    private static final String SERVER_URL = "http://192.168.0.105:3000/api/checkout";
    private static final String API_KEY = "MOON_DSR_MOBILE_APP_KEY_8899";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etShopName = findViewById(R.id.etShopName);
        etShopPhone = findViewById(R.id.etShopPhone);
        etProductsSummary = findViewById(R.id.etProductsSummary);
        etTotalBill = findViewById(R.id.etTotalBill);
        etDiscount = findViewById(R.id.etDiscount);
        etFreeGifts = findViewById(R.id.etFreeGifts);
        etCashCollected = findViewById(R.id.etCashCollected);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvAdminOfferNote = findViewById(R.id.tvAdminOfferNote);

        btnSubmit = findViewById(R.id.btnSubmit);
        btnPrintReceipt = findViewById(R.id.btnPrintReceipt);
        btnSendWhatsapp = findViewById(R.id.btnSendWhatsapp);

        checkPermissions();

        btnSubmit.setOnClickListener(v -> submitMemo());
        btnPrintReceipt.setOnClickListener(v -> printThermalReceipt());
        btnSendWhatsapp.setOnClickListener(v -> sendWhatsAppReceipt());
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 100);
        }
    }

    private void submitMemo() {
        String shopName = etShopName.getText().toString().trim();
        String shopPhone = etShopPhone.getText().toString().trim();
        String billStr = etTotalBill.getText().toString().trim();
        String cashStr = etCashCollected.getText().toString().trim();
        String discountStr = etDiscount.getText().toString().trim();

        if (shopName.isEmpty() || billStr.isEmpty() || cashStr.isEmpty()) {
            Toast.makeText(this, "সব প্রয়োজনীয় তথ্য পূরণ করুন!", Toast.LENGTH_SHORT).show();
            return;
        }

        double bill = Double.parseDouble(billStr);
        double discount = discountStr.isEmpty() ? 0 : Double.parseDouble(discountStr);
        double cash = Double.parseDouble(cashStr);
        double due = (bill - discount) - cash;

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("dsr_id", 1);
            jsonBody.put("shop_name", shopName);
            jsonBody.put("shop_phone", shopPhone);
            jsonBody.put("total_bill", bill);
            jsonBody.put("discount", discount);
            jsonBody.put("cash_collected", cash);
            jsonBody.put("new_due", due);

            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER_URL, jsonBody,
                    response -> Toast.makeText(MainActivity.this, "মেমো সেভ হয়েছে! নতুন বাকি: " + due + " টাকা", Toast.LENGTH_LONG).show(),
                    error -> Toast.makeText(MainActivity.this, "সার্ভার এরর! IP চেক করুন।", Toast.LENGTH_SHORT).show()
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-api-key", API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void printThermalReceipt() {
        try {
            EscPosPrinter printer = new EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32);
            String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());

            double bill = Double.parseDouble(etTotalBill.getText().toString().trim());
            double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString().trim());
            double cash = Double.parseDouble(etCashCollected.getText().toString().trim());
            double due = (bill - discount) - cash;

            String receiptText =
                    "[C]<b>" + COMPANY_NAME + "</b>\n" +
                            "[C]" + COMPANY_ADDRESS + "\n" +
                            "[C]Cell: " + COMPANY_PHONE + "\n" +
                            "[C]--------------------------------\n" +
                            "[L]Date: " + date + "\n" +
                            "[L]DSR: " + dsrName + " (" + dsrPhone + ")\n" +
                            "[L]Shop: " + etShopName.getText().toString() + "\n" +
                            "[C]--------------------------------\n" +
                            "[L]Items: " + etProductsSummary.getText().toString() + "\n" +
                            "[L]Gift: " + etFreeGifts.getText().toString() + "\n" +
                            "[C]--------------------------------\n" +
                            "[L]Total Bill: [R]Tk. " + bill + "\n" +
                            "[L]Discount: [R]Tk. " + discount + "\n" +
                            "[L]Paid Cash: [R]Tk. " + cash + "\n" +
                            "[L]<b>Current Due: [R]Tk. " + due + "</b>\n" +
                            "[C]--------------------------------\n" +
                            "[C]Thank You For Your Business!\n\n\n";

            printer.printFormattedText(receiptText);

        } catch (Exception e) {
            Toast.makeText(this, "প্রিন্টার কানেক্ট করা নেই বা পেয়ার করা হয়নি!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void sendWhatsAppReceipt() {
        String shopPhone = etShopPhone.getText().toString().trim();
        if (shopPhone.isEmpty()) {
            Toast.makeText(this, "দোকানদারের হোয়াটসঅ্যাপ নম্বর দিন!", Toast.LENGTH_SHORT).show();
            return;
        }

        double bill = Double.parseDouble(etTotalBill.getText().toString().trim());
        double discount = etDiscount.getText().toString().isEmpty() ? 0 : Double.parseDouble(etDiscount.getText().toString().trim());
        double cash = Double.parseDouble(etCashCollected.getText().toString().trim());
        double due = (bill - discount) - cash;

        String msg = "*" + COMPANY_NAME + "*\n" +
                "📍 " + COMPANY_ADDRESS + "\n" +
                "📞 Helpline: " + COMPANY_PHONE + "\n\n" +
                "DSR: " + dsrName + "\n" +
                "Shop: " + etShopName.getText().toString() + "\n" +
                "------------------------------\n" +
                "Items: " + etProductsSummary.getText().toString() + "\n" +
                "Offer: " + etFreeGifts.getText().toString() + "\n" +
                "------------------------------\n" +
                "Total Bill: Tk. " + bill + "\n" +
                "Discount: Tk. " + discount + "\n" +
                "Paid Cash: Tk. " + cash + "\n" +
                "*Current Due: Tk. " + due + "*\n\n" +
                "Thank you!";

        try {
            if (!shopPhone.startsWith("+88")) shopPhone = "+88" + shopPhone;
            Intent i = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + shopPhone + "&text=" + URLEncoder.encode(msg, "UTF-8");
            i.setData(Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "ফোনটিতে WhatsApp ইনস্টল করা নেই!", Toast.LENGTH_SHORT).show();
        }
    }
}