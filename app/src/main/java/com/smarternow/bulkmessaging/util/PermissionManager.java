package com.smarternow.bulkmessaging.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionManager — launches all runtime permissions the app needs
 * while running entirely from the phone.
 *
 * Normal permissions (INTERNET, ACCESS_NETWORK_STATE) are granted at install.
 * This manager handles the Android 13+ runtime permission:
 *  - POST_NOTIFICATIONS (for backup/sync status)
 *
 * Call {@link #launchIfNeeded(Activity)} from SplashActivity before
 * auto-pick validation so the dashboard never opens without required grants.
 */
public class PermissionManager {

    public static final int REQUEST_CODE = 9101;

    /** Permissions that need runtime grant */
    public static String[] requiredPermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return perms.toArray(new String[0]);
    }

    public static boolean allGranted(Context ctx) {
        for (String p : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if a system dialog was shown (caller should wait for callback) */
    public static boolean launchIfNeeded(Activity activity) {
        List<String> missing = new ArrayList<>();
        for (String p : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, p)) {
                    // show rationale is handled by caller if needed
                }
                missing.add(p);
            }
        }
        if (missing.isEmpty()) return false;
        ActivityCompat.requestPermissions(activity,
                missing.toArray(new String[0]), REQUEST_CODE);
        return true;
    }

    public static boolean isGrantedResult(int[] grantResults) {
        if (grantResults == null || grantResults.length == 0) return false;
        for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }
}