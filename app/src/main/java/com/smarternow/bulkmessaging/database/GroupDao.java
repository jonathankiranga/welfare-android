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

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insert(Group group);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long[] insertAll(List<Group> groups);

    @Update
    void update(Group group);

    @Delete
    void delete(Group group);

    @Query("DELETE FROM groups WHERE id = :groupId")
    void deleteById(long groupId);

    @Query("SELECT * FROM groups WHERE archived = 0 ORDER BY name ASC")
    List<Group> getAllGroups();

    @Query("SELECT * FROM groups WHERE archived = 0 ORDER BY name ASC")
    List<Group> getAllActiveGroups();

    @Query("SELECT * FROM groups ORDER BY name ASC")
    List<Group> getAllGroupsIncludingArchived();

    @Query("SELECT * FROM groups WHERE id = :groupId")
    Group getGroupById(long groupId);

    @Query("SELECT * FROM groups WHERE remoteId = :remoteId")
    Group getGroupByRemoteId(String remoteId);

    @Query("UPDATE groups SET archived = :archived, updatedAt = :updatedAt WHERE id = :groupId")
    void setArchived(long groupId, boolean archived, long updatedAt);

    @Query("UPDATE groups SET remoteId = :remoteId, updatedAt = :updatedAt WHERE id = :groupId")
    void updateRemoteId(long groupId, String remoteId, long updatedAt);

    @Query("""
            SELECT g.id AS id, g.name AS name, g.description AS description,
                   COUNT(c.id) AS contactCount
            FROM groups g
            LEFT JOIN contacts c ON c.groupId = g.id AND c.archived = 0
            WHERE g.archived = 0
            GROUP BY g.id
            ORDER BY g.name ASC
            """)
    List<GroupWithCount> getGroupsWithContactCount();

    @Query("SELECT COUNT(*) FROM groups WHERE archived = 0")
    int getActiveGroupCount();

    @Query("SELECT COUNT(*) FROM contacts WHERE archived = 0")
    int getActiveContactCount();

    @Query("SELECT COUNT(*) FROM groups")
    int getGroupCount();

    @Query("SELECT COUNT(*) FROM contacts")
    int getTotalContacts();
}