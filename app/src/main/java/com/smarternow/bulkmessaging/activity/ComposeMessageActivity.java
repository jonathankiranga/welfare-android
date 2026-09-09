package com.smarternow.bulkmessaging.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.model.GroupWithCount;
import com.smarternow.bulkmessaging.model.SmsMessage;
import com.smarternow.bulkmessaging.repository.GroupRepository;
import com.smarternow.bulkmessaging.repository.SmsRepository;
import com.smarternow.bulkmessaging.service.AfricaTalkingService;
import com.smarternow.bulkmessaging.util.MessageValidator;
import com.smarternow.bulkmessaging.util.PrefsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Compose + send screen.
 *
 * The user picks a target (a specific group or ALL groups/contacts),
 * types the message, and either sends straight away through Africa's
 * Talking or queues it as a PENDING message. Pending messages are
 * auto-picked and validated again on the splash screen before the next
 * dashboard visit.
 */
public class ComposeMessageActivity extends AppCompatActivity {

    private static final long TARGET_ALL_GROUP_ID = -1;

    private Spinner spTarget;
    private EditText etMessage;
    private TextView tvCharCount;
    private TextView tvValidation;
    private TextView tvRecipientPreview;
    private Button btnSendNow;
    private Button btnQueue;

    private GroupRepository groupRepo;
    private SmsRepository smsRepo;
    private PrefsManager prefs;
    private AfricaTalkingService africaTalkingService;

    private List<GroupWithCount> availableGroups = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean sendInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        groupRepo = new GroupRepository(this);
        smsRepo = new SmsRepository(this);
        prefs = new PrefsManager(this);
        africaTalkingService = new AfricaTalkingService();
        africaTalkingService.useSandbox(prefs.isUseSandbox());

        bindViews();
        setupTargetSpinner();
        setupMessageEditor();
        setupButtons();
        loadGroups();
    }

    private void bindViews() {
        spTarget = findViewById(R.id.spTarget);
        etMessage = findViewById(R.id.etMessage);
        tvCharCount = findViewById(R.id.tvCharCount);
        tvValidation = findViewById(R.id.tvValidation);
        tvRecipientPreview = findViewById(R.id.tvRecipientPreview);
        btnSendNow = findViewById(R.id.btnSendNow);
        btnQueue = findViewById(R.id.btnQueue);
    }

    private void setupTargetSpinner() {
        spTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRecipientPreview(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupMessageEditor() {
        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
                int len = editable.length();
                tvCharCount.setText(getString(R.string.char_count_format, len));
                tvCharCount.setTextColor(getColor(
                        len > 160 ? R.color.validation_red : R.color.text_secondary));

                MessageValidator.ValidationResult result =
                        MessageValidator.validate(editable.toString());
                if (editable.length() == 0) {
                    tvValidation.setText("");
                } else if (result.isValid()) {
                    tvValidation.setText(getString(R.string.compose_ready));
                    tvValidation.setTextColor(getColor(R.color.validation_green));
                } else {
                    tvValidation.setText(result.getReason());
                    tvValidation.setTextColor(getColor(R.color.validation_red));
                }
            }
        });
    }

    private void loadGroups() {
        executor.execute(() -> {
            final List<GroupWithCount> groups = groupRepo.getGroupsWithContactCount();
            final int totalContacts = groupRepo.getTotalContacts();
            runOnUiThread(() -> {
                availableGroups = groups == null ? new ArrayList<>() : groups;

                List<String> labels = new ArrayList<>();
                labels.add("ALL CONTACTS — " + totalContacts + " people");
                for (GroupWithCount g : availableGroups) {
                    labels.add(g.getName() + " — " + g.getContactCount());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_dropdown_item, labels);
                spTarget.setAdapter(adapter);
                updateRecipientPreview(spTarget.getSelectedItemPosition());
            });
        });
    }

    private void updateRecipientPreview(int position) {
        if (position < 0) { tvRecipientPreview.setText(getString(R.string.compose_recipients_zero)); return; }

        if (position == 0) {
            executor.execute(() -> {
                final int total = groupRepo.getTotalContacts();
                runOnUiThread(() -> tvRecipientPreview.setText(getString(R.string.recipients_all, total)));
            });
            return;
        }

        GroupWithCount selected = availableGroups.get(position - 1);
        tvRecipientPreview.setText(getString(R.string.recipients_group, selected.getContactCount(), selected.getName()));
    }

    private void setupButtons() {
        btnSendNow.setOnClickListener(v -> handleSend(false));
        btnQueue.setOnClickListener(v -> handleSend(true));
    }

    private void handleSend(boolean queueOnly) {
        if (sendInProgress) {
            Toast.makeText(this, getString(R.string.send_in_progress), Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = etMessage.getText().toString().trim();
        int position = spTarget.getSelectedItemPosition();

        MessageValidator.ValidationResult result = MessageValidator.validate(messageText);
        if (!result.isValid()) {
            Toast.makeText(this, getString(R.string.cannot_send, result.getReason()), Toast.LENGTH_LONG).show();
            return;
        }

        long targetGroupId;
        String targetGroupName;
        if (position <= 0) {
            targetGroupId = TARGET_ALL_GROUP_ID;
            targetGroupName = "All contacts";
        } else {
            GroupWithCount selected = availableGroups.get(position - 1);
            targetGroupId = selected.getId();
            targetGroupName = selected.getName();
        }

        btnSendNow.setEnabled(false);
        btnQueue.setEnabled(false);
        sendInProgress = true;

        executor.execute(() -> {
            List<Contact> recipients = resolveRecipients(targetGroupId);
            int count = recipients.size();

            if (queueOnly) {
                smsRepo.queuePendingMessage(messageText,
                        targetGroupId == TARGET_ALL_GROUP_ID ? SmsMessage.TARGET_ALL : SmsMessage.TARGET_GROUP,
                        targetGroupId, targetGroupName, count);
                runOnUiThread(() -> {
                    sendInProgress = false;
                    btnSendNow.setEnabled(true);
                    btnQueue.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.queued_pending),
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Send now via Africa's Talking
            if (count == 0) {
                runOnUiThread(() -> {
                    sendInProgress = false;
                    btnSendNow.setEnabled(true);
                    btnQueue.setEnabled(true);
                    Toast.makeText(this, getString(R.string.no_contacts_target), Toast.LENGTH_LONG).show();
                });
                return;
            }

            if (prefs.getApiKey().isEmpty()) {
                runOnUiThread(() -> {
                    sendInProgress = false;
                    btnSendNow.setEnabled(true);
                    btnQueue.setEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.setup_api_key),
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Persist a PENDING record which the gateway will confirm.
            long messageId = smsRepo.saveMessage(messageText,
                    targetGroupId == TARGET_ALL_GROUP_ID ? SmsMessage.TARGET_ALL : SmsMessage.TARGET_GROUP,
                    targetGroupId, targetGroupName, count, SmsMessage.STATUS_PENDING);

            List<String> numbers = new ArrayList<>();
            for (Contact c : recipients) numbers.add(c.getPhoneNumber());

            String apiKey = prefs.getApiKey();
            String username = prefs.getUsername();
            String senderId = prefs.getSenderId();
            long finalMessageId = messageId;

            africaTalkingService.sendBulkSms(apiKey, username, senderId,
                    numbers, messageText, new AfricaTalkingService.SmsCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            int sent = parseSentCount(response);
                            smsRepo.updateMessageStatusAndResponse(finalMessageId,
                                    sent > 0 ? SmsMessage.STATUS_ACCEPTED : SmsMessage.STATUS_SENT,
                                    response.toString());
                            runOnUiThread(() -> {
                                sendInProgress = false;
                                btnSendNow.setEnabled(true);
                                btnQueue.setEnabled(true);
                                Toast.makeText(ComposeMessageActivity.this,
                                        ComposeMessageActivity.this.getString(R.string.at_accepted, sent), Toast.LENGTH_LONG).show();
                                etMessage.setText("");
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            smsRepo.updateMessageStatusAndResponse(finalMessageId,
                                    SmsMessage.STATUS_FAILED, errorMessage);
                            runOnUiThread(() -> {
                                sendInProgress = false;
                                btnSendNow.setEnabled(true);
                                btnQueue.setEnabled(true);
                                new AlertDialog.Builder(ComposeMessageActivity.this)
                                        .setTitle(getString(R.string.dialog_send_failed))
                                        .setMessage(errorMessage)
                                        .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                                        .show();
                            });
                        }
                    });
        });
    }

    private List<Contact> resolveRecipients(long targetGroupId) {
        if (targetGroupId == TARGET_ALL_GROUP_ID) {
            return groupRepo.getAllContacts();
        }
        return groupRepo.getContactsByGroup(targetGroupId);
    }

    private int parseSentCount(JSONObject response) {
        try {
            JSONObject data = response.optJSONObject("SMSMessageData");
            if (data == null) return 0;
            // "Recipients" under SMSMessageData is a JSONArray, not a JSONObject.
            JSONArray arr = data.optJSONArray("Recipients");
            if (arr == null) return 0;
            return arr.length();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}