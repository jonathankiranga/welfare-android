package com.smarternow.bulkmessaging.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.model.SmsMessage;

@Database(
        entities = {Group.class, Contact.class, SmsMessage.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract GroupDao groupDao();
    public abstract ContactDao contactDao();
    public abstract SmsMessageDao smsMessageDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE groups ADD COLUMN remoteId TEXT");
            db.execSQL("ALTER TABLE groups ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE groups ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_groups_remoteId ON groups(remoteId)");
            db.execSQL("ALTER TABLE contacts ADD COLUMN remoteId TEXT");
            db.execSQL("ALTER TABLE contacts ADD COLUMN groupRemoteId TEXT");
            db.execSQL("ALTER TABLE contacts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE contacts ADD COLUMN archived INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contacts_remoteId ON contacts(remoteId)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contacts_phoneNumber ON contacts(phoneNumber)");
            db.execSQL("UPDATE groups SET remoteId = 'local-' || id, updatedAt = createdAt WHERE remoteId IS NULL");
            db.execSQL("UPDATE contacts SET remoteId = 'local-' || id, updatedAt = strftime('%s','now')*1000 WHERE remoteId IS NULL");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smarternow_db"
                    ).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}