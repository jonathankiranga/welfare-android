package com.smarternow.bulkmessaging.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.model.SmsMessage;
import com.smarternow.bulkmessaging.repository.GroupRepository;
import com.smarternow.bulkmessaging.repository.SmsRepository;
import com.smarternow.bulkmessaging.service.AfricaTalkingService;
import com.smarternow.bulkmessaging.util.MessageValidator;
import com.smarternow.bulkmessaging.util.PermissionManager;
import com.smarternow.bulkmessaging.util.PrefsManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Splash / gate screen.
 *
 * Before the dashboard opens, this screen:
 *   1. Auto-picks the oldest PENDING message from the database.
 *   2. Runs the message through the validator (length, spam, placeholders).
 *   3. Marks valid messages as ready, flags invalid ones with a reason.
 *   4. Recounts contacts per chosen target (specific group or all groups).
 *   5. Only then navigates to the dashboard / home page.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int MIN_SPLASH_MS = 1800;

    private TextView tvAutoPickLabel;
    private TextView tvPickedMessage;
    private TextView tvValidationStatus;
    private TextView tvRecipientSummary;
    private ImageView ivLogo;
    private ProgressBar progressBar;
    private View validationPanel;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        bindViews();

        // Fade + scale bounce in on the logo
        animateLogoIn();

        // Permission gate: request runtime permissions before any DB/network work
        if (PermissionManager.launchIfNeeded(this)) {
            tvValidationStatus.setText(getString(R.string.perm_required));
            tvValidationStatus.setTextColor(getColor(R.color.validation_amber));
            return;
        }

        long startTime = System.currentTimeMillis();
        runAutoPickValidation(startTime);
    }

    private void bindViews() {
        ivLogo = findViewById(R.id.ivSplashLogo);
        tvAutoPickLabel = findViewById(R.id.tvAutoPickLabel);
        tvPickedMessage = findViewById(R.id.tvPickedMessage);
        tvValidationStatus = findViewById(R.id.tvValidationStatus);
        tvRecipientSummary = findViewById(R.id.tvRecipientSummary);
        progressBar = findViewById(R.id.progressBar);
        validationPanel = findViewById(R.id.validationPanel);
    }

    private void animateLogoIn() {
        ivLogo.setAlpha(0f);
        ivLogo.setScaleX(0.6f);
        ivLogo.setScaleY(0.6f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f);
        alpha.setDuration(700);
        alpha.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.6f, 1f);
        scaleX.setDuration(700);
        scaleX.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.6f, 1f);
        scaleY.setDuration(700);
        scaleY.setInterpolator(new DecelerateInterpolator());

        scaleX.start();
        scaleY.start();
        alpha.start();

        ObjectAnimator fadePanel = ObjectAnimator.ofFloat(validationPanel, "alpha", 0f, 1f);
        fadePanel.setDuration(900);
        fadePanel.setStartDelay(350);
        fadePanel.start();
    }

    /**
     * Background work: fetch pending messages, validate ALL of them, count recipients.
     * Invalid messages are marked REJECTED here; valid messages remain PENDING and
     * are dispatched via startAutoSendContinuation if auto-send is enabled.
     */
    private void runAutoPickValidation(long startTime) {
        executor.execute(() -> {
            SmsRepository smsRepo = new SmsRepository(this);
            PrefsManager prefs = new PrefsManager(this);

            List<SmsMessage> pending = smsRepo.getPendingMessages();

            if (pending.isEmpty()) {
                mainHandler.post(() -> {
                    tvAutoPickLabel.setText(getString(R.string.splash_autopick));
                    tvPickedMessage.setText(getString(R.string.splash_no_queue));
                    tvValidationStatus.setText(getString(R.string.splash_ok));
                    tvValidationStatus.setTextColor(getColor(R.color.validation_green));
                    tvRecipientSummary.setText(getString(R.string.splash_ready));
                });
            } else {
                // Validate every pending message; reject invalid ones immediately.
                int validCount = 0;
                int rejectedCount = 0;
                SmsMessage firstValid = null;
                for (SmsMessage msg : pending) {
                    MessageValidator.ValidationResult result =
                            MessageValidator.validate(msg.getMessageText());
                    if (result.isValid()) {
                        validCount++;
                        if (firstValid == null) firstValid = msg;
                    } else {
                        smsRepo.updateMessageStatus(msg.getId(), SmsMessage.STATUS_REJECTED);
                        rejectedCount++;
                    }
                }

                final SmsMessage displayMsg = firstValid;
                final int finalValid = validCount;
                final int finalRejected = rejectedCount;
                final int remainingPending = smsRepo.getPendingCount();

                mainHandler.post(() -> {
                    tvAutoPickLabel.setText(getString(R.string.splash_picked));
                    if (displayMsg != null) {
                        tvPickedMessage.setText(displayMsg.getMessageText());
                        tvValidationStatus.setText(
                                "VALID — " + finalValid + " message(s) ready to send."
                                + (finalRejected > 0 ? " " + finalRejected + " rejected." : ""));
                        tvValidationStatus.setTextColor(getColor(R.color.validation_green));
                        tvRecipientSummary.setText(
                                "Target: " + displayMsg.getTargetGroupName()
                                + " | Recipients: " + displayMsg.getRecipientCount()
                                + " | Pending queue: " + remainingPending);
                    } else {
                        tvPickedMessage.setText(getString(R.string.splash_no_queue));
                        tvValidationStatus.setText("FLAGGED — all " + finalRejected + " message(s) rejected.");
                        tvValidationStatus.setTextColor(getColor(R.color.validation_red));
                        tvRecipientSummary.setText(getString(R.string.splash_ready));
                    }
                });
            }

            mainHandler.post(() -> {
                progressBar.setAlpha(0f);
                ObjectAnimator.ofFloat(progressBar, "alpha", 1f, 0f)
                        .setDuration(300).start();
            });

            // Respect minimum splash time so the design is clearly seen.
            long elapsed = System.currentTimeMillis() - startTime;
            long delay = Math.max(0, MIN_SPLASH_MS - elapsed);

            mainHandler.postDelayed(() -> navigateNext(prefs, smsRepo), delay);
        });
    }

    private void navigateNext(PrefsManager prefs, SmsRepository smsRepo) {
        Intent intent;
        // Force the dashboard through a splash-gate: if API keys are missing,
        // the user is routed to settings from the dashboard.
        intent = new Intent(SplashActivity.this, MainActivity.class);

        // Optionally auto-send any remaining ready messages before entering.
        if (prefs.isAutoSendOnStart()) {
            startAutoSendContinuation();
        }

        fadeTransitionTo(intent);
    }

    private void startAutoSendContinuation() {
        // Non-blocking: dispatch all PENDING messages through Africa's Talking.
        executor.execute(() -> {
            SmsRepository repo = new SmsRepository(this);
            PrefsManager prefs = new PrefsManager(this);

            String apiKey = prefs.getApiKey();
            String username = prefs.getUsername();
            String senderId = prefs.getSenderId();

            if (apiKey.isEmpty()) {
                // No credentials — cannot auto-send; messages stay PENDING.
                return;
            }

            AfricaTalkingService service = new AfricaTalkingService();
            service.useSandbox(prefs.isUseSandbox());

            GroupRepository groupRepo = new GroupRepository(this);
            List<SmsMessage> pending = repo.getPendingMessages();

            for (SmsMessage message : pending) {
                // Resolve actual phone numbers for this message's target.
                List<com.smarternow.bulkmessaging.model.Contact> contacts;
                if (SmsMessage.TARGET_ALL.equals(message.getTargetType())) {
                    contacts = groupRepo.getAllContacts();
                } else {
                    contacts = groupRepo.getContactsByGroup(message.getTargetGroupId());
                }

                if (contacts.isEmpty()) {
                    repo.updateMessageStatusAndResponse(message.getId(),
                            SmsMessage.STATUS_FAILED, "No contacts found for target.");
                    continue;
                }

                List<String> numbers = new ArrayList<>();
                for (com.smarternow.bulkmessaging.model.Contact c : contacts) {
                    numbers.add(c.getPhoneNumber());
                }

                final long msgId = message.getId();
                service.sendBulkSms(apiKey, username, senderId, numbers,
                        message.getMessageText(), new AfricaTalkingService.SmsCallback() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                repo.updateMessageStatusAndResponse(
                                        msgId, SmsMessage.STATUS_ACCEPTED, response.toString());
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                repo.updateMessageStatusAndResponse(
                                        msgId, SmsMessage.STATUS_FAILED, errorMessage);
                            }
                        });
            }
        });
    }

private void fadeTransitionTo(Intent intent) {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> {
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 400);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.REQUEST_CODE) {
            if (PermissionManager.isGrantedResult(grantResults)) {
                runAutoPickValidation(System.currentTimeMillis());
            } else {
                tvValidationStatus.setText(getString(R.string.perm_denied));
                tvValidationStatus.setTextColor(getColor(R.color.validation_red));
                tvValidationStatus.setOnClickListener(v -> {
                    tvValidationStatus.setOnClickListener(null);
                    if (!PermissionManager.launchIfNeeded(this)) {
                        runAutoPickValidation(System.currentTimeMillis());
                    }
                });
                // Still continue to dashboard after delay even if denied
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> navigateNext(new PrefsManager(this), new SmsRepository(this)), 1200);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}