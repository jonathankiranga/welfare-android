package com.smarternow.bulkmessaging.service;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client for Africa's Talking SMS Gateway.
 *
 * Endpoint (production):  https://api.africastalking.com/version1/messaging
 * Endpoint (sandbox):     https://api.sandbox.africastalking.com/version1/messaging
 *
 * POST form fields:
 *   username   - Africa's Talking account username
 *   to         - comma separated international phone numbers (+2547...)
 *   message    - SMS body
 *   from       - (optional) Sender ID registered on the account
 *
 * Header:
 *   apiKey     - Africa's Talking API key
 *   Accept     - application/json
 *
 * All recipients are sent in ONE request (single API call).
 */
public class AfricaTalkingService {

    public static final String API_PRODUCTION = "https://api.africastalking.com/version1/messaging";
    public static final String API_SANDBOX = "https://api.sandbox.africastalking.com/version1/messaging";

    public interface SmsCallback {
        void onSuccess(JSONObject response);
        void onFailure(String errorMessage);
    }

    /** Callback used by the settings "Test Connection" check. */
    public interface ConnectionCallback {
        void onSuccess(String accountMessage);
        void onFailure(String errorMessage);
    }

    private final OkHttpClient client;
    private String endpoint = API_PRODUCTION;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AfricaTalkingService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public void useSandbox(boolean sandbox) {
        endpoint = sandbox ? API_SANDBOX : API_PRODUCTION;
    }

    public boolean isSandboxEndpoint() {
        return endpoint.equals(API_SANDBOX);
    }

    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sends one SMS to a comma-separated string of recipients in a single API call.
     */
    public void sendSms(String apiKey, String username, String senderId,
                        String recipients, String message, SmsCallback callback) {

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("username", username)
                .add("to", recipients)
                .add("message", message);

        if (senderId != null && !senderId.trim().isEmpty()) {
            formBuilder.add("from", senderId.trim());
        }

        RequestBody body = formBuilder.build();

        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .header("apiKey", apiKey)
                .header("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverFailure(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(raw);
                    if (response.isSuccessful()) {
                        deliverSuccess(callback, json);
                    } else {
                        JSONObject sms = json.optJSONObject("SMSMessageData");
                        String msg = sms != null ? sms.optString("Message") : null;
                        if (msg == null) msg = "HTTP " + response.code() + ": " + raw;
                        deliverFailure(callback, msg);
                    }
                } catch (Exception e) {
                    deliverFailure(callback, "Could not parse response: " + raw);
                }
            }
        });
    }

    /**
     * Sends ALL phone numbers in ONE single API call.
     * All numbers are joined into a comma-separated string and fired
     * in one request. No chunking.
     */
    public void sendBulkSms(String apiKey, String username, String senderId,
                            List<String> phoneNumbers, String message,
                            SmsCallback callback) {

        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            deliverFailure(callback, "No phone numbers to send to.");
            return;
        }

        String allRecipients = phoneNumbers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));

        sendSms(apiKey, username, senderId, allRecipients, message, callback);
    }

    /**
     * Validates Africa's Talking credentials without sending a real SMS.
     *
     * Uses the user/account-token check against the production or sandbox
     * gateway. A successful response confirms the username + apiKey are
     * registered; an error (401/403) surfaces the exact rejection.
     */
    public void testConnection(String apiKey, String username,
                               ConnectionCallback callback) {

        String tokenUrl;
        if (endpoint.equals(API_SANDBOX)) {
            tokenUrl = "https://api.sandbox.africastalking.com/version1/user?username=" + username;
        } else {
            tokenUrl = "https://api.africastalking.com/version1/user?username=" + username;
        }

        Request request = new Request.Builder()
                .url(tokenUrl)
                .get()
                .header("apiKey", apiKey)
                .header("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                deliverConnectionFailure(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String raw = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    String balance = extractBalance(raw);
                    deliverConnectionSuccess(callback,
                            "Connected. Credentials valid."
                                    + (balance != null ? "\nBalance: " + balance : "")
                                    + "\nGateway: "
                                    + (endpoint.equals(API_SANDBOX) ? "SANDBOX" : "PRODUCTION"));
                } else if (response.code() == 401 || response.code() == 403) {
                    deliverConnectionFailure(callback,
                            "Authentication failed (HTTP " + response.code()
                                    + "). Check your username and API key, and ensure they\n"
                                    + "match the chosen mode (sandbox vs production).");
                } else {
                    deliverConnectionFailure(callback,
                            "Error " + response.code() + ": " + raw);
                }
            }
        });
    }

    private String extractBalance(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            JSONObject account = json.optJSONObject("UserData");
            if (account == null) return null;
            String balance = account.optString("balance");
            return balance.isEmpty() ? null : balance;
        } catch (Exception e) {
            return null;
        }
    }

    private void deliverConnectionSuccess(ConnectionCallback callback, String message) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onSuccess(message));
    }

    private void deliverConnectionFailure(ConnectionCallback callback, String message) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onFailure(message));
    }

    private void deliverSuccess(SmsCallback callback, JSONObject json) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onSuccess(json));
    }

    private void deliverFailure(SmsCallback callback, String message) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onFailure(message));
    }
}
