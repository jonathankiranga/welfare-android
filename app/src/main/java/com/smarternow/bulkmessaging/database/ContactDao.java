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

    @Insert
    long insert(Contact contact);

    @Insert
    long[] insertAll(List<Contact> contacts);

    @Update
    void update(Contact contact);

    @Delete
    void delete(Contact contact);

    @Query("DELETE FROM contacts WHERE id = :contactId")
    void deleteById(long contactId);

    @Query("SELECT * FROM contacts WHERE groupId = :groupId ORDER BY name ASC")
    List<Contact> getContactsByGroup(long groupId);

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    List<Contact> getAllContacts();

    @Query("SELECT COUNT(*) FROM contacts WHERE groupId = :groupId")
    int getContactCountByGroup(long groupId);

    @Query("DELETE FROM contacts WHERE groupId = :groupId")
    void deleteContactsByGroup(long groupId);
}