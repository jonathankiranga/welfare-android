package com.smarternow.bulkmessaging.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "groups", indices = {@Index(value = "remoteId", unique = true)})
public class Group {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String remoteId;
    private String name;
    private String description;
    private long createdAt;
    private long updatedAt;
    private boolean archived;

    public Group(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.remoteId = java.util.UUID.randomUUID().toString();
        this.archived = false;
    }

    // Constructor for sync (with remoteId)
    @androidx.room.Ignore
    public Group(String remoteId, String name, String description, long updatedAt, boolean archived) {
        this.remoteId = remoteId;
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = updatedAt;
        this.archived = archived;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getRemoteId() { return remoteId; }
    public void setRemoteId(String remoteId) { this.remoteId = remoteId; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}