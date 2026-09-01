package com.smarternow.bulkmessaging.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.adapter.GroupAdapter;
import com.smarternow.bulkmessaging.model.GroupWithCount;
import com.smarternow.bulkmessaging.repository.GroupRepository;
import com.smarternow.bulkmessaging.repository.SmsRepository;
import com.smarternow.bulkmessaging.service.PwaSyncService;
import com.smarternow.bulkmessaging.util.PrefsManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dashboard / Home page shown after the splash gate.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvTotalContacts;
    private TextView tvTotalGroups;
    private TextView tvPendingMessages;
    private TextView tvTotalSent;
    private TextView tvApiStatus;
    private TextView tvEmptyState;
    private TextView tvLastSync;
    private RecyclerView rvGroups;
    private View loadingView;
    private com.google.android.material.button.MaterialButton btnSyncNow;

    private GroupAdapter groupAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GroupRepository groupRepo;
    private SmsRepository smsRepo;
    private PrefsManager prefs;
    private PwaSyncService pwaSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        groupRepo = new GroupRepository(this);
        smsRepo = new SmsRepository(this);
        prefs = new PrefsManager(this);
        pwaSync = new PwaSyncService(this);

        bindViews();
        setupRecycler();
        setupButtons();
        refreshDashboard();
        refreshLastSyncLabel();
    }

    private void bindViews() {
        tvTotalContacts = findViewById(R.id.tvTotalContacts);
        tvTotalGroups = findViewById(R.id.tvTotalGroups);
        tvPendingMessages = findViewById(R.id.tvPendingMessages);
        tvTotalSent = findViewById(R.id.tvTotalSent);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvLastSync = findViewById(R.id.tvLastSync);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        rvGroups = findViewById(R.id.rvGroups);
        loadingView = findViewById(R.id.loadingView);
    }

    private void setupRecycler() {
        groupAdapter = new GroupAdapter();
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupAdapter);
        groupAdapter.setOnGroupClickListener(group -> openGroup(group.getId()));
    }

    private void setupButtons() {
        CardView cvCompose = findViewById(R.id.cvCompose);
        CardView cvManageGroups = findViewById(R.id.cvManageGroups);
        CardView cvHistory = findViewById(R.id.cvHistory);
        CardView cvSettings = findViewById(R.id.cvSettings);

        cvCompose.setOnClickListener(v -> startActivity(new Intent(this, ComposeMessageActivity.class)));
        cvManageGroups.setOnClickListener(v -> startActivity(new Intent(this, GroupActivity.class)));
        cvHistory.setOnClickListener(v -> startActivity(new Intent(this, MessageHistoryActivity.class)));
        cvSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> doSyncNow());
        }
    }

    private void doSyncNow() {
        if (prefs.getPwaUrl().isEmpty()) {
            android.widget.Toast.makeText(this, getString(R.string.pwa_url_hint), android.widget.Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        btnSyncNow.setEnabled(false);
        btnSyncNow.setText(getString(R.string.drive_backing));
        pwaSync.pull(new PwaSyncService.SyncCallback() {
            @Override public void onSuccess(String msg) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setText(getString(R.string.sync_now));
                refreshLastSyncLabel();
                refreshDashboard();
                android.widget.Toast.makeText(MainActivity.this, msg, android.widget.Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(String err) {
                btnSyncNow.setEnabled(true);
                btnSyncNow.setText(getString(R.string.sync_now));
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.dialog_connection_failed))
                        .setMessage(getString(R.string.sync_failed, err))
                        .setPositiveButton(getString(R.string.dialog_positive_ok), null).show();
            }
        });
    }

    private void refreshLastSyncLabel() {
        if (tvLastSync == null) return;
        long ts = prefs.getLastPwaSync();
        if (ts == 0) tvLastSync.setText(getString(R.string.last_sync_never));
        else {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault());
            tvLastSync.setText(getString(R.string.last_sync_prefix, fmt.format(new java.util.Date(ts))));
        }
    }

    private void refreshDashboard() {
        loadingView.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            final int contacts = groupRepo.getTotalContacts();
            final int groups = groupRepo.getGroupCount();
            final int pending = smsRepo.getPendingCount();
            final int totalSent = smsRepo.getTotalMessageCount() - pending;
            final List<GroupWithCount> groupList = groupRepo.getGroupsWithContactCount();
            final boolean hasApi = !prefs.getApiKey().isEmpty();

            mainHandler.post(() -> {
                loadingView.setVisibility(View.GONE);
                tvTotalContacts.setText(String.valueOf(contacts));
                tvTotalGroups.setText(String.valueOf(groups));
                tvPendingMessages.setText(String.valueOf(pending));
                tvTotalSent.setText(String.valueOf(Math.max(0, totalSent)));
                tvApiStatus.setText(hasApi ? "Africa's Talking — connected"
                        : "Set up your Africa's Talking API key in Settings");
                tvApiStatus.setTextColor(getColor(
                        hasApi ? R.color.validation_green : R.color.validation_red));

                groupAdapter.submitGroups(groupList);
                if (groupList == null || groupList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvGroups.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvGroups.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void openGroup(long groupId) {
        Intent intent = new Intent(this, GroupDetailActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (groupRepo != null) {
            refreshDashboard();
            refreshLastSyncLabel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}