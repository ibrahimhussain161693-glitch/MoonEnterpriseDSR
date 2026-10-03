package com.moonenterprise.dsrapp.utils;

import android.location.Location;

public class GeoFenceHelper {

    // Default maximum allowable distance from shop (50 meters)
    public static final float DEFAULT_MAX_RADIUS_METERS = 50.0f;

    /**
     * Calculates distance between user location and shop location in meters.
     */
    public static float calculateDistanceMeters(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0];
    }

    /**
     * Returns true if user is within radius (e.g. 50 meters) of shop.
     */
    public static boolean isWithinRadius(double userLat, double userLng, double shopLat, double shopLng, float radiusMeters) {
        if (shopLat == 0 && shopLng == 0) {
            return true; // Untagged shop bypass
        }
        float distance = calculateDistanceMeters(userLat, userLng, shopLat, shopLng);
        return distance <= radiusMeters;
    }
}
