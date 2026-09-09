package com.smarternow.bulkmessaging.repository;

import android.content.Context;

import com.smarternow.bulkmessaging.database.AppDatabase;
import com.smarternow.bulkmessaging.model.SmsMessage;

import java.util.List;

/**
 * Persistence for SMS message records, including the pending queue
 * that the splash screen auto-picks from before the dashboard opens.
 */
public class SmsRepository {

    private final AppDatabase db;

    public SmsRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public long queuePendingMessage(String messageText, String targetType,
                                    long groupId, String groupName, int recipientCount) {
        SmsMessage message = new SmsMessage(
                messageText, targetType, groupId, groupName,
                recipientCount, SmsMessage.STATUS_PENDING);
        return db.smsMessageDao().insert(message);
    }

    public long saveMessage(String messageText, String targetType,
                            long groupId, String groupName,
                            int recipientCount, String status) {
        SmsMessage message = new SmsMessage(
                messageText, targetType, groupId, groupName,
                recipientCount, status);
        return db.smsMessageDao().insert(message);
    }

    public List<SmsMessage> getPendingMessages() {
        return db.smsMessageDao().getPendingMessages();
    }

    public List<SmsMessage> getMessagesByStatus(String status) {
        return db.smsMessageDao().getMessagesByStatus(status);
    }

    public List<SmsMessage> getAllMessages() {
        return db.smsMessageDao().getAllMessages();
    }

    public int getPendingCount() {
        return db.smsMessageDao().getPendingCount();
    }

    public int getTotalMessageCount() {
        return db.smsMessageDao().getMessageCount();
    }

    public void updateMessageStatus(long messageId, String status) {
        db.smsMessageDao().updateStatus(messageId, status);
    }

    public void updateMessageStatusAndResponse(long messageId, String status, String response) {
        db.smsMessageDao().updateStatusAndResponse(messageId, status, response);
    }

    public void clearHistory() {
        db.smsMessageDao().clearAll();
    }

    public void deleteMessage(long messageId) {
        db.smsMessageDao().deleteById(messageId);
    }
}