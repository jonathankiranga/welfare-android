package com.smarternow.bulkmessaging.util;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Validates SMS content before it is queued or sent through Africa's Talking.
 * Checks message length, empty content, spam words and prohibited content.
 */
public class MessageValidator {

    private static final int MAX_SMS_LENGTH = 160;
    private static final int MAX_UNICODE_LENGTH = 70;

    private static final List<String> SPAM_WORDS = Arrays.asList(
            "free money", "casino", "betting bonus", "click here now",
            "buy now", "urgent claim", "wire transfer", "million nairas",
            "lottery winner", "prize"
    );

    private static final List<String> INVALID_TEMPLATES = Arrays.asList(
            "{{", "}}", "%%phone%%"
    );

    public static final class ValidationResult {
        private final boolean valid;
        private final String reason;

        ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
    }

    public static ValidationResult validate(String message) {
        String content = message == null ? "" : message.trim();

        if (TextUtils.isEmpty(content)) {
            return new ValidationResult(false, "Message is empty. Nothing to send.");
        }

        if (content.length() > MAX_SMS_LENGTH) {
            return new ValidationResult(false,
                    "Message exceeds " + MAX_SMS_LENGTH + " characters. It has " + content.length()
                            + ". Trim or shorten it.");
        }

        if (MessageUtil.isUnicode(content) && content.length() > MAX_UNICODE_LENGTH) {
            return new ValidationResult(false,
                    "Unicode message exceeds " + MAX_UNICODE_LENGTH + " characters.");
        }

        if (containsDigitsOnly(content)) {
            return new ValidationResult(false, "Message is digits only. Add readable text.");
        }

        String lower = content.toLowerCase(Locale.US);
        for (String word : SPAM_WORDS) {
            if (lower.contains(word)) {
                return new ValidationResult(false,
                        "Message appears to contain promotional/spam content: \"" + word + "\".");
            }
        }

        for (String tpl : INVALID_TEMPLATES) {
            if (content.contains(tpl)) {
                return new ValidationResult(false,
                        "Message contains unresolved placeholder \"" + tpl + "\".");
            }
        }

        if (hasBrokenPlaceholders(content)) {
            return new ValidationResult(false,
                    "Message has mismatched braces { } or unresolved {name} placeholders.");
        }

        return new ValidationResult(true, "Message is valid.");
    }

    private static boolean containsDigitsOnly(String s) {
        int digits = 0, letters = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) digits++;
            else if (Character.isLetter(c)) letters++;
        }
        return digits > 0 && letters == 0;
    }

    private static boolean hasBrokenPlaceholders(String s) {
        int open = 0;
        for (char c : s.toCharArray()) {
            if (c == '{') open++;
            if (c == '}') open--;
            if (open < 0) return true;
        }
        return open != 0;
    }
}