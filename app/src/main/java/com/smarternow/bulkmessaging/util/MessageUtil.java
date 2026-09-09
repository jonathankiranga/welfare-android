package com.smarternow.bulkmessaging.util;

/**
 * Small helpers used across the app.
 */
public class MessageUtil {

    /**
     * Roughly detects whether a message contains non-GSM characters that
     * would be billed as unicode SMS segments.
     */
    public static boolean isUnicode(String message) {
        for (char c : message.toCharArray()) {
            if (c > 127) return true;
        }
        return false;
    }

    /**
     * Normalizes a phone number to E.164 international format required by
     * Africa's Talking (+2547XXXXXXXX). Mirrors the normalizePhone() function
     * in the PWA server.js so both sides produce identical output.
     *
     * Handles:
     *   +2547XXXXXXXX  → +2547XXXXXXXX  (already correct)
     *   2547XXXXXXXX   → +2547XXXXXXXX  (missing leading +)
     *   07XXXXXXXX     → +2547XXXXXXXX  (local Kenyan format)
     *   7XXXXXXXX      → +2547XXXXXXXX  (dropped leading 0)
     *
     * Any number that doesn't match a known pattern is returned unchanged so
     * Africa's Talking can surface the rejection rather than silently dropping it.
     */
    public static String normalizePhone(String raw) {
        if (raw == null || raw.trim().isEmpty()) return raw;

        // Strip everything except digits and a leading +
        String p = raw.replaceAll("[^0-9+]", "");
        if (p.isEmpty()) return raw;

        // Remove leading + for uniform processing, re-add at end
        if (p.startsWith("+")) p = p.substring(1);

        // Already full international: 2547XXXXXXXX (12 digits)
        if (p.startsWith("254") && p.length() >= 12) return "+" + p;

        // Local format: 07XXXXXXXX (10 digits)
        if (p.startsWith("0") && p.length() == 10) return "+254" + p.substring(1);

        // Shortened local: 7XXXXXXXX (9 digits)
        if (p.startsWith("7") && p.length() == 9) return "+254" + p;

        // Fallback — return unchanged so AT can reject with a clear error
        return raw;
    }
}