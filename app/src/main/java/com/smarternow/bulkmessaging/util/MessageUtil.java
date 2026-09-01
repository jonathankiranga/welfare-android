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
}