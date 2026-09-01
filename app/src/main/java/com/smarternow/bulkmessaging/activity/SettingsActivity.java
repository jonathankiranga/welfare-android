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
import com.smarternow.bulkmessaging.service.DriveAuthHelper;
import com.smarternow.bulkmessaging.service.DriveBackupManager;
import com.smarternow.bulkmessaging.util.PrefsManager;

/**
 * Gateway configuration for Africa's Talking.
 * Requires: username + API key (+ optional Sender ID).
 * Also toggles sandbox vs production and auto-send behaviour.
 *
 * Lock rule: after setup is saved AND test connection succeeds,
 * fields are locked (read-only) until the user taps Edit.
 * Any edit after unlocking clears the validated flag until
 * Test is passed again.
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etApiKey;
    private EditText etSenderId;
    private MaterialSwitch swSandbox;
    private MaterialSwitch swAutoSend;
    private MaterialButton btnSave;
    private MaterialButton btnTest;
    private MaterialButton btnEdit;
    private MaterialButton btnDriveBackup;
    private MaterialButton btnDriveRestore;
    private TextView tvLockStatus;
    private TextView tvLastBackup;

    private PrefsManager prefs;
    private DriveBackupManager driveBackup;
    private String pendingDriveAction; // "backup" or "restore"
    private boolean isLocked;
    private boolean suppressWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsManager(this);

        etUsername = findViewById(R.id.etUsername);
        etApiKey = findViewById(R.id.etApiKey);
        etSenderId = findViewById(R.id.etSenderId);
        swSandbox = findViewById(R.id.swSandbox);
        swAutoSend = findViewById(R.id.swAutoSend);
        btnSave = findViewById(R.id.btnSaveSettings);
        btnTest = findViewById(R.id.btnTestConnection);
        btnEdit = findViewById(R.id.btnEditSettings);
        btnDriveBackup = findViewById(R.id.btnDriveBackup);
        btnDriveRestore = findViewById(R.id.btnDriveRestore);
        tvLockStatus = findViewById(R.id.tvLockStatus);
        tvLastBackup = findViewById(R.id.tvLastBackup);
        driveBackup = new DriveBackupManager(this);

        loadCurrentValues();
        setupCredentialWatchers();
        applyLockState();
        refreshLastBackupLabel();

        btnTest.setOnClickListener(v -> testConnection());
        btnDriveBackup.setOnClickListener(v -> startDriveAction("backup"));
        btnDriveRestore.setOnClickListener(v -> startDriveAction("restore"));

        btnEdit.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_edit_credentials))
                    .setMessage(getString(R.string.dialog_edit_credentials_msg))
                    .setPositiveButton(getString(R.string.dialog_positive_unlock), (d, w) -> unlockForEdit())
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
        });

        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String apiKey = etApiKey.getText().toString().trim();
            String senderId = etSenderId.getText().toString().trim();

            if (username.isEmpty() || apiKey.isEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.username_api_required), android.widget.Toast.LENGTH_LONG).show();
                return;
            }

            prefs.saveUsername(username);
            prefs.saveApiKey(apiKey);
            prefs.saveSenderId(senderId);
            prefs.setAutoSendOnStart(swAutoSend.isChecked());
            prefs.setUseSandbox(swSandbox.isChecked());
            prefs.setSetupDone(true);

            if (prefs.isConnectionValidated()) {
                android.widget.Toast.makeText(this, getString(R.string.saved_locked),
                        android.widget.Toast.LENGTH_LONG).show();
            } else {
                android.widget.Toast.makeText(this,
                        getString(R.string.saved_tap_test,
                                swSandbox.isChecked()
                                ? "Using SANDBOX — no real SMS will be billed."
                                : "Using PRODUCTION gateway."),
                        android.widget.Toast.LENGTH_LONG).show();
            }
            applyLockState();
            if (prefs.isLocked()) {
                // stay on screen to show locked state, don't auto-finish
                return;
            }
            finish();
        });
    }

    private void loadCurrentValues() {
        suppressWatcher = true;
        etUsername.setText(prefs.getUsername());
        etApiKey.setText(prefs.getApiKey());
        etSenderId.setText(prefs.getSenderId());
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
                if (prefs.isConnectionValidated()) {
                    prefs.setConnectionValidated(false);
                }
            }
        };
        etUsername.addTextChangedListener(invalidateWatcher);
        etApiKey.addTextChangedListener(invalidateWatcher);
        etSenderId.addTextChangedListener(invalidateWatcher);
        swSandbox.setOnCheckedChangeListener((btn, checked) -> {
            if (suppressWatcher || isLocked) return;
            if (prefs.isConnectionValidated()) {
                prefs.setConnectionValidated(false);
            }
        });
    }

    private void applyLockState() {
        isLocked = prefs.isLocked();
        etUsername.setEnabled(!isLocked);
        etApiKey.setEnabled(!isLocked);
        etSenderId.setEnabled(!isLocked);
        swSandbox.setEnabled(!isLocked);
        // swAutoSend is not credential-related, keep editable even when locked
        btnSave.setEnabled(!isLocked);
        btnTest.setEnabled(!isLocked);
        tvLockStatus.setVisibility(isLocked ? android.view.View.VISIBLE : android.view.View.GONE);
        btnEdit.setVisibility(isLocked ? android.view.View.VISIBLE : android.view.View.GONE);
        if (isLocked) {
            tvLockStatus.setText(getString(R.string.locked_validated));
        }
    }

    private void unlockForEdit() {
        prefs.setConnectionValidated(false);
        applyLockState();
        android.widget.Toast.makeText(this,
                getString(R.string.unlocked_edit),
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void refreshLastBackupLabel() {
        long ts = getSharedPreferences("smarternow_prefs", MODE_PRIVATE).getLong("last_drive_backup", 0);
        if (ts == 0) {
            tvLastBackup.setText(getString(R.string.drive_no_backup));
        } else {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault());
            tvLastBackup.setText(getString(R.string.last_backup_prefix, fmt.format(new java.util.Date(ts))));
        }
    }

    private void startDriveAction(String action) {
        pendingDriveAction = action;
        String existing = DriveAuthHelper.lastSignedInEmail(this);
        if (existing != null) {
            if ("backup".equals(action)) doDriveBackup(existing);
            else doDriveRestore(existing);
        } else {
            DriveAuthHelper.signIn(this);
        }
    }

    private void doDriveBackup(String email) {
        btnDriveBackup.setEnabled(false);
        btnDriveBackup.setText(getString(R.string.drive_backing));
        driveBackup.backup(email, new DriveBackupManager.BackupCallback() {
            @Override public void onSuccess(String msg) {
                btnDriveBackup.setEnabled(true);
                btnDriveBackup.setText(SettingsActivity.this.getString(R.string.settings_drive_backup));
                refreshLastBackupLabel();
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_drive_backup_complete))
                        .setMessage(msg + "\n\nIncludes all groups, contacts and Africa's Talking settings.")
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
            @Override public void onFailure(String err) {
                btnDriveBackup.setEnabled(true);
                btnDriveBackup.setText(SettingsActivity.this.getString(R.string.settings_drive_backup));
                new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(getString(R.string.dialog_backup_failed)).setMessage(err)
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }

    private void doDriveRestore(String email) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_restore_drive))
                .setMessage(getString(R.string.dialog_restore_drive_msg))
                .setPositiveButton(getString(R.string.dialog_positive_restore), (d, w) -> {
                    btnDriveRestore.setEnabled(false);
                    btnDriveRestore.setText(getString(R.string.drive_restoring));
                    driveBackup.restore(email, new DriveBackupManager.BackupCallback() {
                        @Override public void onSuccess(String msg) {
                            btnDriveRestore.setEnabled(true);
                            btnDriveRestore.setText(SettingsActivity.this.getString(R.string.settings_drive_restore));
                            loadCurrentValues();
                            applyLockState();
                            new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                                    .setTitle(getString(R.string.dialog_restore_complete)).setMessage(msg)
                                    .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
                        }
                        @Override public void onFailure(String err) {
                            btnDriveRestore.setEnabled(true);
                            btnDriveRestore.setText(SettingsActivity.this.getString(R.string.settings_drive_restore));
                            new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                                    .setTitle(getString(R.string.dialog_restore_failed)).setMessage(err)
                                    .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
                        }
                    });
                })
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DriveAuthHelper.RC_SIGN_IN) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String email = DriveAuthHelper.accountEmailFromResult(data);
                    if (email == null) throw new Exception("No account");
                    if ("backup".equals(pendingDriveAction)) doDriveBackup(email);
                    else if ("restore".equals(pendingDriveAction)) doDriveRestore(email);
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, getString(R.string.signin_failed, e.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                }
            } else {
                android.widget.Toast.makeText(this, getString(R.string.signin_cancelled), android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void testConnection() {
        String username = etUsername.getText().toString().trim();
        String apiKey = etApiKey.getText().toString().trim();

        if (username.isEmpty() || apiKey.isEmpty()) {
            android.widget.Toast.makeText(this,
                    getString(R.string.enter_username_api),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        btnTest.setEnabled(false);
        btnTest.setText(getString(R.string.testing_connection));

        AfricaTalkingService service = new AfricaTalkingService();
        service.useSandbox(swSandbox.isChecked());

        service.testConnection(apiKey, username,
                new AfricaTalkingService.ConnectionCallback() {
                    @Override
                    public void onSuccess(String accountMessage) {
                        prefs.setConnectionValidated(true);
                        btnTest.setEnabled(true);
                        btnTest.setText(getString(R.string.settings_test));
                        applyLockState();
                        new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(getString(R.string.dialog_connection_ok_locked))
                                .setMessage(accountMessage + "\n\nCredentials are now locked. Tap Edit to change again.")
                                .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                                .show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        prefs.setConnectionValidated(false);
                        btnTest.setEnabled(true);
                        btnTest.setText(getString(R.string.settings_test));
                        applyLockState();
                        new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this)
                                .setTitle(getString(R.string.dialog_connection_failed))
                                .setMessage(errorMessage)
                                .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                                .show();
                    }
                });
    }
}
