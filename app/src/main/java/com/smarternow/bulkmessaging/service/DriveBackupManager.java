package com.smarternow.bulkmessaging.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.smarternow.bulkmessaging.database.AppDatabase;
import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.util.PrefsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.api.client.http.ByteArrayContent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DriveBackupManager — Google Drive backup for ALL contacts/groups
 * and Africa's Talking settings.
 *
 * - Saves to Drive appDataFolder (hidden, per-app, auto-counted to Drive quota)
 * - Also covered by Auto Backup (backup_rules.xml) but this gives explicit
 *   user-triggered backup/restore + cross-device manual restore.
 * - File: smarternow_backup.json in appDataFolder, single file overwritten each backup.
 */
public class DriveBackupManager {

    private static final String BACKUP_FILE_NAME = "smarternow_backup.json";
    private static final String BACKUP_MIME = "application/json";

    public interface BackupCallback {
        void onSuccess(String msg);
        void onFailure(String error);
    }

    private final Context ctx;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public DriveBackupManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    private Drive driveForAccount(String accountEmail) {
        GoogleAccountCredential cred = GoogleAccountCredential.usingOAuth2(
                ctx, Collections.singleton(DriveScopes.DRIVE_APPDATA));
        cred.setSelectedAccountName(accountEmail);
        return new Drive.Builder(new NetHttpTransport(), new GsonFactory(), cred)
                .setApplicationName("Smarternow Bulk Messaging")
                .build();
    }

    /** Build the JSON snapshot: settings + groups + contacts */
    private String buildSnapshotJson() throws Exception {
        AppDatabase db = AppDatabase.getInstance(ctx);
        List<Group> groups = db.groupDao().getAllGroups();
        List<Contact> contacts = db.contactDao().getAllContacts();
        PrefsManager prefs = new PrefsManager(ctx);

        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("timestamp", System.currentTimeMillis());

        JSONObject settings = new JSONObject();
        settings.put("username", prefs.getUsername());
        settings.put("apiKey", prefs.getApiKey());
        settings.put("senderId", prefs.getSenderId());
        settings.put("useSandbox", prefs.isUseSandbox());
        settings.put("autoSendOnStart", prefs.isAutoSendOnStart());
        root.put("settings", settings);

        JSONArray jGroups = new JSONArray();
        for (Group g : groups) {
            JSONObject jo = new JSONObject();
            jo.put("id", g.getId());
            jo.put("name", g.getName());
            jo.put("description", g.getDescription());
            jGroups.put(jo);
        }
        root.put("groups", jGroups);

        JSONArray jContacts = new JSONArray();
        for (Contact c : contacts) {
            JSONObject jo = new JSONObject();
            jo.put("id", c.getId());
            jo.put("name", c.getName());
            jo.put("phoneNumber", c.getPhoneNumber());
            jo.put("groupId", c.getGroupId());
            jContacts.put(jo);
        }
        root.put("contacts", jContacts);
        return root.toString(2);
    }

    public void backup(String accountEmail, BackupCallback cb) {
        exec.execute(() -> {
            try {
                String json = buildSnapshotJson();
                Drive drive = driveForAccount(accountEmail);

                // Find existing file in appDataFolder
                String existingId = null;
                com.google.api.services.drive.model.FileList list = drive.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name='" + BACKUP_FILE_NAME + "'")
                        .setFields("files(id,name)")
                        .execute();
                if (list.getFiles() != null && !list.getFiles().isEmpty()) {
                    existingId = list.getFiles().get(0).getId();
                }

                byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ByteArrayContent content = new ByteArrayContent(BACKUP_MIME, bytes);
                com.google.api.services.drive.model.File meta = new com.google.api.services.drive.model.File();
                meta.setName(BACKUP_FILE_NAME);
                meta.setParents(Collections.singletonList("appDataFolder"));

                if (existingId != null) {
                    drive.files().update(existingId, meta, content).execute();
                } else {
                    drive.files().create(meta, content)
                            .setFields("id")
                            .execute();
                }

                // Also save timestamp locally for offline UI
                ctx.getSharedPreferences("smarternow_prefs", Context.MODE_PRIVATE)
                        .edit().putLong("last_drive_backup", System.currentTimeMillis()).apply();

                postSuccess(cb, "Backup saved to Google Drive (" + bytes.length + " bytes)");
            } catch (Exception e) {
                postFailure(cb, "Backup failed: " + e.getMessage());
            }
        });
    }

    public void restore(String accountEmail, BackupCallback cb) {
        exec.execute(() -> {
            try {
                Drive drive = driveForAccount(accountEmail);
                com.google.api.services.drive.model.FileList list = drive.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name='" + BACKUP_FILE_NAME + "'")
                        .setFields("files(id,name,modifiedTime)")
                        .execute();
                if (list.getFiles() == null || list.getFiles().isEmpty()) {
                    postFailure(cb, "No backup found in Drive appDataFolder.");
                    return;
                }
                String fileId = list.getFiles().get(0).getId();
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                drive.files().get(fileId).executeMediaAndDownloadTo(out);
                String json = out.toString(java.nio.charset.StandardCharsets.UTF_8.name());

                JSONObject root = new JSONObject(json);
                JSONObject settings = root.optJSONObject("settings");
                JSONArray jGroups = root.optJSONArray("groups");
                JSONArray jContacts = root.optJSONArray("contacts");

                AppDatabase db = AppDatabase.getInstance(ctx);
                PrefsManager prefs = new PrefsManager(ctx);

                // Restore settings
                if (settings != null) {
                    if (settings.has("username")) prefs.saveUsername(settings.optString("username"));
                    if (settings.has("apiKey")) prefs.saveApiKey(settings.optString("apiKey"));
                    if (settings.has("senderId")) prefs.saveSenderId(settings.optString("senderId"));
                    if (settings.has("useSandbox")) prefs.setUseSandbox(settings.optBoolean("useSandbox"));
                    if (settings.has("autoSendOnStart")) prefs.setAutoSendOnStart(settings.optBoolean("autoSendOnStart"));
                    // require re-validation after restore
                    prefs.setConnectionValidated(false);
                }

                // Restore groups/contacts: wipe and re-insert preserving IDs via raw insert
                // Simplest: clear then re-create (IDs will be new but contacts remapped)
                db.contactDao().getAllContacts(); // ensure DB open
                // We cannot preserve old IDs with autoGenerate, so remap groupId for contacts
                java.util.Map<Long, Long> idMap = new java.util.HashMap<>();
                // Clear existing
                db.groupDao().getAllGroups().forEach(g -> db.groupDao().delete(g));

                if (jGroups != null) {
                    for (int i = 0; i < jGroups.length(); i++) {
                        JSONObject jo = jGroups.getJSONObject(i);
                        long oldId = jo.optLong("id", -1);
                        String name = jo.optString("name", "");
                        String desc = jo.optString("description", "");
                        long newId = db.groupDao().insert(new Group(name, desc));
                        if (oldId != -1) idMap.put(oldId, newId);
                    }
                }
                if (jContacts != null) {
                    for (int i = 0; i < jContacts.length(); i++) {
                        JSONObject jo = jContacts.getJSONObject(i);
                        String name = jo.optString("name", "");
                        String phone = jo.optString("phoneNumber", "");
                        long oldGroupId = jo.optLong("groupId", -1);
                        long newGroupId = idMap.getOrDefault(oldGroupId, oldGroupId);
                        if (newGroupId != -1) {
                            db.contactDao().insert(new Contact(name, phone, newGroupId));
                        }
                    }
                }

                postSuccess(cb, "Restored " + (jGroups != null ? jGroups.length() : 0) + " groups, "
                        + (jContacts != null ? jContacts.length() : 0) + " contacts + settings. Re-test connection to lock.");
            } catch (Exception e) {
                postFailure(cb, "Restore failed: " + e.getMessage());
            }
        });
    }

    private void postSuccess(BackupCallback cb, String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onSuccess(msg));
    }
    private void postFailure(BackupCallback cb, String err) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onFailure(err));
    }
}