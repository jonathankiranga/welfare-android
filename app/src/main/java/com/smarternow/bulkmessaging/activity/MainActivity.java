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
    private RecyclerView rvGroups;
    private View loadingView;

    private GroupAdapter groupAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GroupRepository groupRepo;
    private SmsRepository smsRepo;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        groupRepo = new GroupRepository(this);
        smsRepo = new SmsRepository(this);
        prefs = new PrefsManager(this);

        bindViews();
        setupRecycler();
        setupButtons();
        refreshDashboard();
    }

    private void bindViews() {
        tvTotalContacts = findViewById(R.id.tvTotalContacts);
        tvTotalGroups = findViewById(R.id.tvTotalGroups);
        tvPendingMessages = findViewById(R.id.tvPendingMessages);
        tvTotalSent = findViewById(R.id.tvTotalSent);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        tvEmptyState = findViewById(R.id.tvEmptyState);
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

        cvCompose.setOnClickListener(v -> startActivity(
                new Intent(this, ComposeMessageActivity.class)));
        cvManageGroups.setOnClickListener(v -> startActivity(
                new Intent(this, GroupActivity.class)));
        cvHistory.setOnClickListener(v -> startActivity(
                new Intent(this, MessageHistoryActivity.class)));
        cvSettings.setOnClickListener(v -> startActivity(
                new Intent(this, SettingsActivity.class)));
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}