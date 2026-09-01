package com.smarternow.bulkmessaging.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import com.smarternow.bulkmessaging.service.AfricaTalkingService;

import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * DeviceBindingManager — enforces single-APK lock (one user, one device).
 * First install that POSTs to /api/device/register with X-API-Key binds.
 * Later clones get 403 DEVICE_ALREADY_BOUND and must do self-service OTP.
 */
public class DeviceBindingManager {

    public interface BindingCallback {
        void onBound(String deviceToken);
        void onAlreadyBound();
        void onFailure(String err);
    }
    public interface OtpCallback {
        void onSent();
        void onFailure(String err);
    }
    public interface TransferCallback {
        void onTransferred(String newToken);
        void onFailure(String err);
    }

    private final Context ctx;
    private final PrefsManager prefs;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Handler main = new Handler(Looper.getMainLooper());

    public DeviceBindingManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.prefs = new PrefsManager(ctx);
    }

    @SuppressLint("HardwareIds")
    public String getOrCreateDeviceId() {
        String existing = prefs.getDeviceId();
        if (existing != null && !existing.isEmpty()) return existing;
        String androidId;
        try {
            androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) { androidId = "unknown"; }
        String id = UUID.randomUUID().toString() + "-" + androidId;
        prefs.setDeviceId(id);
        return id;
    }

    public void register(BindingCallback cb) {
        String url = prefs.getPwaUrl().replaceAll("/api/?$", "") + "/api/device/register";
        String deviceId = getOrCreateDeviceId();
        RequestBody body = RequestBody.create("{\"device_id\":\""+deviceId+"\"}", MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(body)
                .header("Content-Type", "application/json").build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) { postFail(cb, e.getMessage()); }
            @Override public void onResponse(Call c, Response r) throws IOException {
                String raw = r.body()!=null? r.body().string() : "";
                if (r.isSuccessful()) {
                    try {
                        String token = new JSONObject(raw).optString("device_token");
                        prefs.setDeviceToken(token); prefs.setDeviceBound(true);
                        postBound(cb, token);
                    } catch (Exception e){ postFail(cb, e.getMessage()); }
                } else if (r.code()==403) {
                    postAlreadyBound(cb);
                } else postFail(cb, raw);
            }
        });
    }

    public void requestOtp(OtpCallback cb){
        String url = prefs.getPwaUrl().replaceAll("/api/?$", "") + "/api/device/request-otp";
        String deviceId = getOrCreateDeviceId();
        RequestBody body = RequestBody.create("{\"device_id\":\""+deviceId+"\"}", MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(body)
                .header("Content-Type","application/json").build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e){ postOtpFail(cb,e.getMessage());}
            @Override public void onResponse(Call c, Response r) throws IOException{
                if(r.isSuccessful()) postOtpSent(cb); else postOtpFail(cb, r.body()!=null?r.body().string():"error");
            }
        });
    }

    public void transfer(String otp, TransferCallback cb){
        String url = prefs.getPwaUrl().replaceAll("/api/?$", "") + "/api/device/transfer";
        String deviceId = getOrCreateDeviceId();
        RequestBody body = RequestBody.create("{\"device_id\":\""+deviceId+"\",\"otp\":\""+otp+"\"}", MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(body)
                .header("Content-Type","application/json").build();
        client.newCall(req).enqueue(new Callback(){
            @Override public void onFailure(Call c, IOException e){ postTransferFail(cb,e.getMessage());}
            @Override public void onResponse(Call c, Response r) throws IOException{
                String raw=r.body()!=null?r.body().string():"";
                if(r.isSuccessful()){
                    try{ String t=new JSONObject(raw).optString("device_token"); prefs.setDeviceToken(t); prefs.setDeviceBound(true); postTransferred(cb,t);}catch(Exception e){postTransferFail(cb,e.getMessage());}
                } else postTransferFail(cb, raw);
            }
        });
    }

    private void postBound(BindingCallback cb,String t){ main.post(()->cb.onBound(t));}
    private void postAlreadyBound(BindingCallback cb){ main.post(cb::onAlreadyBound);}
    private void postFail(BindingCallback cb,String e){ main.post(()->cb.onFailure(e));}
    private void postOtpSent(OtpCallback cb){ main.post(cb::onSent);}
    private void postOtpFail(OtpCallback cb,String e){ main.post(()->cb.onFailure(e));}
    private void postTransferred(TransferCallback cb,String t){ main.post(()->cb.onTransferred(t));}
    private void postTransferFail(TransferCallback cb,String e){ main.post(()->cb.onFailure(e));}
}