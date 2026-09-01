package com.smarternow.bulkmessaging.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "contacts",
        foreignKeys = @ForeignKey(
                entity = Group.class,
                parentColumns = "id",
                childColumns = "groupId",
                onDelete = CASCADE
        ),
        indices = {@Index("groupId"), @Index(value = "remoteId", unique = true), @Index(value = "phoneNumber", unique = true)}
)
public class Contact {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String remoteId;
    private String name;
    private String phoneNumber;
    private long groupId;
    private String groupRemoteId;
    private long updatedAt;
    private boolean archived;

    public Contact(String name, String phoneNumber, long groupId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.groupId = groupId;
        this.remoteId = java.util.UUID.randomUUID().toString();
        this.updatedAt = System.currentTimeMillis();
        this.archived = false;
    }

    @androidx.room.Ignore
    public Contact(String remoteId, String name, String phoneNumber, long groupId, String groupRemoteId, long updatedAt, boolean archived) {
        this.remoteId = remoteId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.groupId = groupId;
        this.groupRemoteId = groupRemoteId;
        this.updatedAt = updatedAt;
        this.archived = archived;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public String getGroupRemoteId() { return groupRemoteId; }
    public void setGroupRemoteId(String groupRemoteId) { this.groupRemoteId = groupRemoteId; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }
}