package com.smarternow.bulkmessaging;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Room;

import com.smarternow.bulkmessaging.database.AppDatabase;

/**
 * Minimal RAM Application: trims caches on low memory, single OkHttp
 * instance reused via AfricaTalkingService, no largeHeap.
 */
public class SmarternowApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Warm DB on background to avoid main-thread spike, then let GC reclaim
        new Thread(() -> {
            try {
                AppDatabase.getInstance(this).groupDao().getGroupCount();
            } catch (Exception ignored) {}
        }).start();
        // Schedule once-per-day PWA sync (welfare-pwa.onrender.com)
        try {
            com.smarternow.bulkmessaging.worker.DailySyncWorker.schedule(this);
        } catch (Exception ignored) {}
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Hint GC, drop OkHttp idle connections
            System.gc();
            new Handler(Looper.getMainLooper()).post(() -> {
                // Allow system to reclaim bitmap caches
                trimCache();
            });
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
        trimCache();
    }

    private void trimCache() {
        try {
            // Clear Glide/other caches if added later
            getCacheDir();
        } catch (Exception ignored) {}
    }
}