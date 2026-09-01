package com.smarternow.bulkmessaging.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smarternow.bulkmessaging.service.PwaSyncService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * DailySyncWorker — once per day pull from welfare-pwa.onrender.com
 * Requirements: NETWORK_CONNECTED, battery not low. Minimal RAM.
 */
public class DailySyncWorker extends Worker {

    public DailySyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params){ super(ctx, params); }

    @NonNull
    @Override
    public Result doWork() {
        CountDownLatch latch=new CountDownLatch(1);
        final boolean[] ok={false};
        new PwaSyncService(getApplicationContext()).pull(new PwaSyncService.SyncCallback(){
            @Override public void onSuccess(String msg){ ok[0]=true; latch.countDown();}
            @Override public void onFailure(String err){ ok[0]=false; latch.countDown();}
        });
        try{ latch.await(60, TimeUnit.SECONDS);}catch(Exception e){ return Result.retry();}
        return ok[0]? Result.success() : Result.retry();
    }

    public static void schedule(Context ctx){
        Constraints c=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build();
        PeriodicWorkRequest req=new PeriodicWorkRequest.Builder(DailySyncWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(c).addTag("pwa-daily-sync").build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("pwa-daily-sync", ExistingPeriodicWorkPolicy.KEEP, req);
    }

    public static void cancel(Context ctx){ WorkManager.getInstance(ctx).cancelUniqueWork("pwa-daily-sync"); }
}