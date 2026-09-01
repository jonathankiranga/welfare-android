package com.smarternow.bulkmessaging.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_messages")
public class SmsMessage {

    /** Message status values */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    /** Target type values */
    public static final String TARGET_ALL = "ALL";
    public static final String TARGET_GROUP = "GROUP";

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String messageText;
    private String targetType;
    private long targetGroupId;
    private String targetGroupName;
    private int recipientCount;
    private String status;
    private long timestamp;
    private String response;

    public SmsMessage(String messageText, String targetType, long targetGroupId,
                      String targetGroupName, int recipientCount, String status) {
        this.messageText = messageText;
        this.targetType = targetType;
        this.targetGroupId = targetGroupId;
        this.targetGroupName = targetGroupName;
        this.recipientCount = recipientCount;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public long getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(long targetGroupId) { this.targetGroupId = targetGroupId; }

    public String getTargetGroupName() { return targetGroupName; }
    public void setTargetGroupName(String targetGroupName) { this.targetGroupName = targetGroupName; }

    public int getRecipientCount() { return recipientCount; }
    public void setRecipientCount(int recipientCount) { this.recipientCount = recipientCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}