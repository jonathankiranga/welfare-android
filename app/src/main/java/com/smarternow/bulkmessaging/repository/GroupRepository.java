package com.smarternow.bulkmessaging.repository;

import android.content.Context;

import com.smarternow.bulkmessaging.database.AppDatabase;
import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.model.GroupWithCount;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all group and contact persistence logic.
 */
public class GroupRepository {

    private final AppDatabase db;

    public GroupRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public long createGroup(String name, String description) {
        Group group = new Group(name, description);
        return db.groupDao().insert(group);
    }

    public void updateGroup(long groupId, String name, String description) {
        Group group = db.groupDao().getGroupById(groupId);
        if (group != null) {
            group.setName(name);
            group.setDescription(description);
            db.groupDao().update(group);
        }
    }

    public void deleteGroup(long groupId) {
        db.groupDao().deleteById(groupId);
    }

    public List<Group> getAllGroups() {
        return db.groupDao().getAllGroups();
    }

    public List<GroupWithCount> getGroupsWithContactCount() {
        return db.groupDao().getGroupsWithContactCount();
    }

    public Group getGroupById(long groupId) {
        return db.groupDao().getGroupById(groupId);
    }

    public long addContact(String name, String phoneNumber, long groupId) {
        Contact contact = new Contact(name, phoneNumber, groupId);
        return db.contactDao().insert(contact);
    }

    public void bulkAddContacts(long groupId, List<String[]> rows) {
        List<Contact> contacts = new ArrayList<>();
        for (String[] row : rows) {
            contacts.add(new Contact(row[0], row[1], groupId));
        }
        db.contactDao().insertAll(contacts);
    }

    public List<Contact> getContactsByGroup(long groupId) {
        return db.contactDao().getContactsByGroup(groupId);
    }

    public List<Contact> getAllContacts() {
        return db.contactDao().getAllContacts();
    }

    public int getContactCountByGroup(long groupId) {
        return db.contactDao().getContactCountByGroup(groupId);
    }

    public int getTotalContacts() {
        return db.groupDao().getTotalContacts();
    }

    public int getGroupCount() {
        return db.groupDao().getGroupCount();
    }

    public void deleteContact(long contactId) {
        db.contactDao().deleteById(contactId);
    }

    public void deleteAllContactsInGroup(long groupId) {
        db.contactDao().deleteContactsByGroup(groupId);
    }
}