package com.smarternow.bulkmessaging.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.model.SmsMessage;

@Database(
        entities = {Group.class, Contact.class, SmsMessage.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract GroupDao groupDao();
    public abstract ContactDao contactDao();
    public abstract SmsMessageDao smsMessageDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smarternow_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}