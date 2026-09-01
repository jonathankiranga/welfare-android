package com.smarternow.bulkmessaging.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores Africa's Talking gateway credentials and app preferences
 * in encrypted-friendly SharedPreferences.
 */
public class PrefsManager {

    private static final String PREFS_NAME = "smarternow_prefs";

    private static final String KEY_API_KEY = "at_api_key";
    private static final String KEY_USERNAME = "at_username";
    private static final String KEY_SENDER_ID = "at_sender_id";
    private static final String KEY_IS_SETUP = "is_setup_done";
    private static final String KEY_BLOCKED_WORD_LIST = "blocked_words";
    private static final String KEY_AUTO_SEND_ON_START = "auto_send_on_start";
    private static final String KEY_USE_SANDBOX = "use_sandbox";
    private static final String KEY_CONNECTION_VALIDATED = "connection_validated";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username.trim()).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "sandbox");
    }

    public void saveSenderId(String senderId) {
        prefs.edit().putString(KEY_SENDER_ID, senderId.trim()).apply();
    }

    public String getSenderId() {
        return prefs.getString(KEY_SENDER_ID, "");
    }

    public void setSetupDone(boolean done) {
        prefs.edit().putBoolean(KEY_IS_SETUP, done).apply();
    }

    public boolean isSetupDone() {
        return prefs.getBoolean(KEY_IS_SETUP, false);
    }

    public void saveBlockedWords(Set<String> words) {
        prefs.edit().putStringSet(KEY_BLOCKED_WORD_LIST, words).apply();
    }

    public Set<String> getBlockedWords() {
        return prefs.getStringSet(KEY_BLOCKED_WORD_LIST, new HashSet<>());
    }

    public void setAutoSendOnStart(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SEND_ON_START, enabled).apply();
    }

    public boolean isAutoSendOnStart() {
        return prefs.getBoolean(KEY_AUTO_SEND_ON_START, false);
    }

    public void setUseSandbox(boolean sandbox) {
        prefs.edit().putBoolean(KEY_USE_SANDBOX, sandbox).apply();
    }

    public boolean isUseSandbox() {
        return prefs.getBoolean(KEY_USE_SANDBOX, false);
    }

    public void setConnectionValidated(boolean validated) {
        prefs.edit().putBoolean(KEY_CONNECTION_VALIDATED, validated).apply();
    }

    public boolean isConnectionValidated() {
        return prefs.getBoolean(KEY_CONNECTION_VALIDATED, false);
    }

    public boolean isLocked() {
        return isSetupDone() && isConnectionValidated();
    }
}