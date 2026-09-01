package com.smarternow.bulkmessaging.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.adapter.MessageHistoryAdapter;
import com.smarternow.bulkmessaging.model.SmsMessage;
import com.smarternow.bulkmessaging.repository.SmsRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows every SMS record: accepted, sent, rejected, failed and pending.
 * Tapping a message reveals its full body and the gateway response.
 */
public class MessageHistoryActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextView tvEmptyState;
    private MaterialButton btnClearAll;

    private SmsRepository smsRepo;
    private MessageHistoryAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_history);

        smsRepo = new SmsRepository(this);

        rvMessages = findViewById(R.id.rvMessages);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnClearAll = findViewById(R.id.btnClearHistory);

        adapter = new MessageHistoryAdapter(this::showMessageDetail);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        loadMessages();

        btnClearAll.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear message history?")
                    .setMessage("All records will be removed permanently.")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        executor.execute(() -> {
                            smsRepo.clearHistory();
                            loadMessages();
                        });
                    })
                    .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                    .show();
        });
    }

    private void loadMessages() {
        executor.execute(() -> {
            final List<SmsMessage> messages = smsRepo.getAllMessages();
            runOnUiThread(() -> {
                adapter.submitMessages(messages);
                if (messages == null || messages.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvMessages.setVisibility(View.GONE);
                    btnClearAll.setEnabled(false);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvMessages.setVisibility(View.VISIBLE);
                    btnClearAll.setEnabled(true);
                }
            });
        });
    }

    private void showMessageDetail(SmsMessage message) {
        StringBuilder body = new StringBuilder();
        body.append("Message:\n").append(message.getMessageText()).append("\n\n");
        body.append("Status: ").append(message.getStatus()).append("\n");
        body.append("Target: ").append(message.getTargetGroupName()).append("\n");
        body.append("Recipients: ").append(message.getRecipientCount()).append("\n\n");
        if (message.getResponse() != null && !message.getResponse().isEmpty()) {
            body.append("Gateway response:\n").append(message.getResponse());
        } else {
            body.append("No gateway response recorded.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Message detail")
                .setMessage(body.toString())
                .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}