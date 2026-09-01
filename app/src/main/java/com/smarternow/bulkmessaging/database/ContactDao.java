package com.smarternow.bulkmessaging.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.smarternow.bulkmessaging.model.Contact;

import java.util.List;

@Dao
public interface ContactDao {

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insert(Contact contact);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long[] insertAll(List<Contact> contacts);

    @Update
    void update(Contact contact);

    @Delete
    void delete(Contact contact);

    @Query("DELETE FROM contacts WHERE id = :contactId")
    void deleteById(long contactId);

    @Query("SELECT * FROM contacts WHERE groupId = :groupId AND archived = 0 ORDER BY name ASC")
    List<Contact> getContactsByGroup(long groupId);

    @Query("SELECT * FROM contacts WHERE archived = 0 ORDER BY name ASC")
    List<Contact> getAllContacts();

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    List<Contact> getAllContactsIncludingArchived();

    @Query("SELECT * FROM contacts WHERE remoteId = :remoteId")
    Contact getContactByRemoteId(String remoteId);

    @Query("UPDATE contacts SET archived = :archived, updatedAt = :updatedAt WHERE id = :contactId")
    void setArchived(long contactId, boolean archived, long updatedAt);

    @Query("UPDATE contacts SET archived = :archived, updatedAt = :updatedAt WHERE remoteId = :remoteId")
    void setArchivedByRemoteId(String remoteId, boolean archived, long updatedAt);

    @Query("SELECT COUNT(*) FROM contacts WHERE groupId = :groupId AND archived = 0")
    int getContactCountByGroup(long groupId);

    @Query("DELETE FROM contacts WHERE groupId = :groupId")
    void deleteContactsByGroup(long groupId);
}