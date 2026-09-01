package com.smarternow.bulkmessaging.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.smarternow.bulkmessaging.model.SmsMessage;

import java.util.List;

@Dao
public interface SmsMessageDao {

    @Insert
    long insert(SmsMessage smsMessage);

    @Update
    void update(SmsMessage smsMessage);

    @Query("SELECT * FROM sms_messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    List<SmsMessage> getPendingMessages();

    @Query("SELECT * FROM sms_messages WHERE status = :status ORDER BY timestamp ASC")
    List<SmsMessage> getMessagesByStatus(String status);

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    List<SmsMessage> getAllMessages();

    @Query("SELECT COUNT(*) FROM sms_messages")
    int getMessageCount();

    @Query("SELECT COUNT(*) FROM sms_messages WHERE status = 'PENDING'")
    int getPendingCount();

    @Query("DELETE FROM sms_messages")
    void clearAll();

    @Query("DELETE FROM sms_messages WHERE id = :messageId")
    void deleteById(long messageId);
}