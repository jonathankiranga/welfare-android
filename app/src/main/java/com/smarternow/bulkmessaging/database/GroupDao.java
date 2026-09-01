package com.smarternow.bulkmessaging.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.model.GroupWithCount;

import java.util.List;

@Dao
public interface GroupDao {

    @Insert
    long insert(Group group);

    @Update
    void update(Group group);

    @Delete
    void delete(Group group);

    @Query("DELETE FROM groups WHERE id = :groupId")
    void deleteById(long groupId);

    @Query("SELECT * FROM groups ORDER BY name ASC")
    List<Group> getAllGroups();

    @Query("SELECT * FROM groups WHERE id = :groupId")
    Group getGroupById(long groupId);

    @Query("""
            SELECT g.id AS id, g.name AS name, g.description AS description,
                   COUNT(c.id) AS contactCount
            FROM groups g
            LEFT JOIN contacts c ON c.groupId = g.id
            GROUP BY g.id
            ORDER BY g.name ASC
            """)
    List<GroupWithCount> getGroupsWithContactCount();

    @Query("SELECT COUNT(*) FROM groups")
    int getGroupCount();

    @Query("SELECT COUNT(*) FROM contacts")
    int getTotalContacts();
}