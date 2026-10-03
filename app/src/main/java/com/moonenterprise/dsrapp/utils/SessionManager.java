package com.moonenterprise.dsrapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "MoonDsrSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role"; // ADMIN, SR, DSR
    private static final String KEY_SERVER_IP = "server_ip";

    // Fixed Production Cloud Server URL
    public static final String CLOUD_SERVER_URL = "https://moonenterprisedsr.onrender.com";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUserSession(int id, String name, String phone, String role) {
        editor.putInt(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public void setServerIp(String ip) {
        if (ip != null) {
            ip = ip.trim();
        }
        editor.putString(KEY_SERVER_IP, ip);
        editor.apply();
    }

    public String getServerIp() {
        return pref.getString(KEY_SERVER_IP, CLOUD_SERVER_URL);
    }

    public String getBaseUrl() {
        return CLOUD_SERVER_URL;
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "User");
    }

    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, "");
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "");
    }

    public boolean isLoggedIn() {
        return getUserId() != -1;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
