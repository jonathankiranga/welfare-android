package com.smarternow.bulkmessaging.model;

/**
 * Model that pairs a Group with its contact count.
 * Used by the dashboard for display purposes.
 */
public class GroupWithCount {
    private long id;
    private String name;
    private String description;
    private int contactCount;

    public GroupWithCount(long id, String name, String description, int contactCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contactCount = contactCount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getContactCount() { return contactCount; }
    public void setContactCount(int contactCount) { this.contactCount = contactCount; }
}