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
        return pref.getString(KEY_SERVER_IP, "moon-dsr.onrender.com");
    }

    public String getBaseUrl() {
        String raw = getServerIp();
        if (raw == null || raw.trim().isEmpty()) {
            return "https://moon-dsr.onrender.com";
        }
        raw = raw.trim();

        // Full URL entered (e.g. https://moon-dsr.onrender.com)
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            if (raw.endsWith("/")) {
                raw = raw.substring(0, raw.length() - 1);
            }
            return raw;
        }

        // Domain name entered (e.g. moon-dsr.onrender.com or mydomain.com)
        if (raw.contains(".com") || raw.contains(".org") || raw.contains(".net") || raw.contains(".app") || raw.contains(".render") || raw.contains("onrender")) {
            if (raw.endsWith("/")) {
                raw = raw.substring(0, raw.length() - 1);
            }
            return "https://" + raw;
        }

        // Local IP address entered (e.g. 192.168.0.105 or 10.0.2.2)
        if (raw.contains(":")) {
            raw = raw.split(":")[0];
        }
        if (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return "http://" + raw + ":3000";
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
