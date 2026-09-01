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
        indices = @Index("groupId")
)
public class Contact {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String phoneNumber;
    private long groupId;

    public Contact(String name, String phoneNumber, long groupId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.groupId = groupId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }
}