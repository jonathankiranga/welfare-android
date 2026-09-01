package com.smarternow.bulkmessaging.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "groups")
public class Group {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String description;
    private long createdAt;

    public Group(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}