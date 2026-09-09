package com.smarternow.bulkmessaging.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.service.AfricaTalkingService;
import com.smarternow.bulkmessaging.service.PwaSyncService;
import com.smarternow.bulkmessaging.util.DeviceBindingManager;
import com.smarternow.bulkmessaging.util.PrefsManager;

/**
 * Gateway + PWA + Device lock configuration.
 * PWA at welfare-pwa.onrender.com is source of truth, Android is single APK lock
 * via device binding (OTP self-service), archive-only, TiDB MySQL.
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etUsername, etApiKey, etSenderId;
    private EditText etPwaUrl, etOtp;
    private MaterialSwitch swSandbox, swAutoSend;
    private MaterialButton btnSave, btnTest, btnEdit;
    private MaterialButton btnSyncNow, btnPushToPwa;
    private MaterialButton btnDeviceRegister, btnDeviceRequestOtp, btnDeviceTransfer;
    private TextView tvLockStatus, tvLastSync, tvDeviceStatus;

    private PrefsManager prefs;
    private DeviceBindingManager deviceBinding;
    private PwaSyncService pwaSync;
    private boolean isLocked;
    private boolean suppressWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsManager(this);
        deviceBinding = new DeviceBindingManager(this);
        pwaSync = new PwaSyncService(this);

        etUsername = findViewById(R.id.etUsername);
        etApiKey = findViewById(R.id.etApiKey);
        etSenderId = findViewById(R.id.etSenderId);
        etPwaUrl = findViewById(R.id.etPwaUrl);
        etOtp = findViewById(R.id.etOtp);
        swSandbox = findViewById(R.id.swSandbox);
        swAutoSend = findViewById(R.id.swAutoSend);
        btnSave = findViewById(R.id.btnSaveSettings);
        btnTest = findViewById(R.id.btnTestConnection);
        btnEdit = findViewById(R.id.btnEditSettings);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        btnPushToPwa = findViewById(R.id.btnPushToPwa);
        btnDeviceRegister = findViewById(R.id.btnDeviceRegister);
        btnDeviceRequestOtp = findViewById(R.id.btnDeviceRequestOtp);
        btnDeviceTransfer = findViewById(R.id.btnDeviceTransfer);
        tvLockStatus = findViewById(R.id.tvLockStatus);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvDeviceStatus = findViewById(R.id.tvDeviceStatus);

        loadCurrentValues();
        setupCredentialWatchers();
        applyLockState();
        refreshLastSyncLabel();
        refreshDeviceStatus();

        btnTest.setOnClickListener(v -> testConnection());
        btnSyncNow.setOnClickListener(v -> doSyncNow());
        btnPushToPwa.setOnClickListener(v -> doPushToPwa());
        btnDeviceRegister.setOnClickListener(v -> doDeviceRegister());
        btnDeviceRequestOtp.setOnClickListener(v -> doRequestOtp());
        btnDeviceTransfer.setOnClickListener(v -> doTransfer());

        btnEdit.setOnClickListener(v ->
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_edit_credentials))
                    .setMessage(getString(R.string.dialog_edit_credentials_msg))
                    .setPositiveButton(getString(R.string.dialog_positive_unlock), (d, w) -> unlockForEdit())
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show()
        );

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String apiKey   = etApiKey.getText().toString().trim();
            String senderId = etSenderId.getText().toString().trim();
            String pwaUrl   = etPwaUrl.getText().toString().trim();
            if (username.isEmpty() || apiKey.isEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.username_api_required), android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            prefs.saveUsername(username);
            prefs.saveApiKey(apiKey);
            prefs.saveSenderId(senderId);
            prefs.setAutoSendOnStart(swAutoSend.isChecked());
            prefs.setUseSandbox(swSandbox.isChecked());
            prefs.setPwaUrl(pwaUrl.isEmpty() ? "https://welfare-pwa.onrender.com/api" : pwaUrl);
            prefs.setSetupDone(true);
            if (prefs.isConnectionValidated()) {
                android.widget.Toast.makeText(this, getString(R.string.saved_locked), android.widget.Toast.LENGTH_LONG).show();
            } else {
                android.widget.Toast.makeText(this, getString(R.string.saved_tap_test,
                        swSandbox.isChecked() ? "Using SANDBOX — no real SMS will be billed." : "Using PRODUCTION gateway."),
                        android.widget.Toast.LENGTH_LONG).show();
            }
            applyLockState();
            refreshDeviceStatus();
            if (prefs.isLocked()) return;
            finish();
        });
    }

    private void loadCurrentValues() {
        suppressWatcher = true;
        etUsername.setText(prefs.getUsername());
        etApiKey.setText(prefs.getApiKey());
        etSenderId.setText(prefs.getSenderId());
        etPwaUrl.setText(prefs.getPwaUrl());
        swSandbox.setChecked(prefs.isUseSandbox());
        swAutoSend.setChecked(prefs.isAutoSendOnStart());
        suppressWatcher = false;
    }

    private void setupCredentialWatchers() {
        TextWatcher invalidateWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressWatcher || isLocked) return;
                if (prefs.isConnectionValidated()) prefs.setConnectionValidated(false);
            }
        };
        etUsername.addTextChangedListener(invalidateWatcher);
        etApiKey.addTextChangedListener(invalidateWatcher);
        etSenderId.addTextChangedListener(invalidateWatcher);
        etPwaUrl.addTextChangedListener(invalidateWatcher);
        swSandbox.setOnCheckedChangeListener((btn, checked) -> {
            if (suppressWatcher || isLocked) return;
            if (prefs.isConnectionValidated()) prefs.setConnectionValidated(false);
        });
    }

    private void applyLockState() {
        isLocked = prefs.isLocked();
        etUsername.setEnabled(!isLocked);
        etApiKey.setEnabled(!isLocked);
        etSenderId.setEnabled(!isLocked);
        etPwaUrl.setEnabled(!isLocked);
        swSandbox.setEnabled(!isLocked);
        btnSave.setEnabled(!isLocked);
        btnTest.setEnabled(!isLocked);
        tvLockStatus.setVisibility(isLocked ? android.view.View.VISIBLE : android.view.View.GONE);
        btnEdit.setVisibility(isLocked ? android.view.View.VISIBLE : android.view.View.GONE);
        if (isLocked) tvLockStatus.setText(getString(R.string.locked_validated));
    }

    private void unlockForEdit() {
        prefs.setConnectionValidated(false);
        applyLockState();
        android.widget.Toast.makeText(this, getString(R.string.unlocked_edit), android.widget.Toast.LENGTH_SHORT).show();
    }

    private void refreshLastSyncLabel() {
        long ts = prefs.getLastPwaSync();
        if (ts == 0) tvLastSync.setText(getString(R.string.last_sync_never));
        else {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault());
            tvLastSync.setText(getString(R.string.last_sync_prefix, fmt.format(new java.util.Date(ts))));
        }
    }

    private void refreshDeviceStatus() {
        if (prefs.isDeviceBound() && !prefs.getDeviceToken().isEmpty()) {
            tvDeviceStatus.setText(getString(R.string.device_status_bound));
            tvDeviceStatus.setTextColor(getColor(R.color.validation_green));
        } else if (!prefs.getDeviceId().isEmpty()) {
            tvDeviceStatus.setText(getString(R.string.device_status_already_bound));
            tvDeviceStatus.setTextColor(getColor(R.color.validation_amber));
        } else {
            tvDeviceStatus.setText(getString(R.string.device_status_unbound));
            tvDeviceStatus.setTextColor(getColor(R.color.validation_red));
        }
    }

    private void doSyncNow() {
        btnSyncNow.setEnabled(false);
        btnSyncNow.setText(getString(R.string.drive_backing));
        pwaSync.pull(new PwaSyncService.SyncCallback() {
            @Override public void onSuccess(String msg) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setText(getString(R.string.sync_now));
                refreshLastSyncLabel();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_ok))
                        .setMessage(msg)
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onFailure(String err) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setText(getString(R.string.sync_now));
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_failed))
                        .setMessage(getString(R.string.sync_failed, err))
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }

    private void doPushToPwa() {
        btnPushToPwa.setEnabled(false);
        btnPushToPwa.setText(getString(R.string.drive_backing));
        pwaSync.pushAll(new PwaSyncService.SyncCallback() {
            @Override public void onSuccess(String msg) {
                btnPushToPwa.setEnabled(true);
                btnPushToPwa.setText(getString(R.string.push_to_pwa));
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_ok))
                        .setMessage(msg)
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onFailure(String err) {
                btnPushToPwa.setEnabled(true);
                btnPushToPwa.setText(getString(R.string.push_to_pwa));
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_failed))
                        .setMessage(getString(R.string.push_failed, err))
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }

    private void doDeviceRegister() {
        btnDeviceRegister.setEnabled(false);
        deviceBinding.register(new DeviceBindingManager.BindingCallback() {
            @Override public void onBound(String t) {
                btnDeviceRegister.setEnabled(true);
                refreshDeviceStatus();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.device_status_bound))
                        .setMessage(getString(R.string.device_transferred))
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onAlreadyBound() {
                btnDeviceRegister.setEnabled(true);
                refreshDeviceStatus();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.device_status_already_bound))
                        .setMessage(getString(R.string.device_status_already_bound))
                        .setPositiveButton(getString(R.string.device_request_otp), (d, w) -> doRequestOtp())
                        .setNegativeButton(getString(R.string.dialog_negative_cancel), null).show();
            }
            @Override public void onFailure(String e) {
                btnDeviceRegister.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_failed))
                        .setMessage(e)
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }

    private void doRequestOtp() {
        btnDeviceRequestOtp.setEnabled(false);
        deviceBinding.requestOtp(new DeviceBindingManager.OtpCallback() {
            @Override public void onSent() {
                btnDeviceRequestOtp.setEnabled(true);
                android.widget.Toast.makeText(SettingsActivity.this, getString(R.string.otp_sent), android.widget.Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(String e) {
                btnDeviceRequestOtp.setEnabled(true);
                android.widget.Toast.makeText(SettingsActivity.this, e, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void doTransfer() {
        String otp = etOtp.getText().toString().trim();
        if (otp.length() != 6) {
            android.widget.Toast.makeText(this, getString(R.string.otp_invalid), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        btnDeviceTransfer.setEnabled(false);
        deviceBinding.transfer(otp, new DeviceBindingManager.TransferCallback() {
            @Override public void onTransferred(String t) {
                btnDeviceTransfer.setEnabled(true);
                refreshDeviceStatus();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.device_status_bound))
                        .setMessage(getString(R.string.device_transferred))
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onFailure(String e) {
                btnDeviceTransfer.setEnabled(true);
                android.widget.Toast.makeText(SettingsActivity.this,
                        getString(R.string.otp_invalid) + ": " + e, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void testConnection() {
        String username = etUsername.getText().toString().trim();
        String apiKey   = etApiKey.getText().toString().trim();
        if (username.isEmpty() || apiKey.isEmpty()) {
            android.widget.Toast.makeText(this, getString(R.string.enter_username_api), android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        btnTest.setEnabled(false);
        btnTest.setText(getString(R.string.testing_connection));
        AfricaTalkingService service = new AfricaTalkingService();
        service.useSandbox(swSandbox.isChecked());
        service.testConnection(apiKey, username, new AfricaTalkingService.ConnectionCallback() {
            @Override public void onSuccess(String accountMessage) {
                prefs.setConnectionValidated(true);
                btnTest.setEnabled(true);
                btnTest.setText(getString(R.string.settings_test));
                applyLockState();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_ok_locked))
                        .setMessage(accountMessage + "\n\nCredentials are now locked. Tap Edit to change again.")
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onFailure(String errorMessage) {
                prefs.setConnectionValidated(false);
                btnTest.setEnabled(true);
                btnTest.setText(getString(R.string.settings_test));
                applyLockState();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_connection_failed))
                        .setMessage(errorMessage)
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }
}
