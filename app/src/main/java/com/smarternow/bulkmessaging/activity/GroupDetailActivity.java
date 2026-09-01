package com.smarternow.bulkmessaging.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.adapter.ContactAdapter;
import com.smarternow.bulkmessaging.model.Contact;
import com.smarternow.bulkmessaging.model.Group;
import com.smarternow.bulkmessaging.repository.GroupRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lists every contact inside a group and lets the user add contacts
 * one at a time or paste many rows at once.
 *
 * Accepted number formats: 254712345678, 0712345678, +254712345678,
 * 00254712345678. Numbers are normalised to +254 format before saving so
 * Africa's Talking always receives international numbers.
 */
public class GroupDetailActivity extends AppCompatActivity {

    private long groupId;
    private TextView tvGroupTitle;
    private TextView tvGroupSubtitle;
    private RecyclerView rvContacts;
    private TextView tvEmptyState;

    private GroupRepository groupRepo;
    private ContactAdapter contactAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        groupId = getIntent().getLongExtra("GROUP_ID", -1);
        if (groupId < 0) {
            Toast.makeText(this, getString(R.string.group_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        groupRepo = new GroupRepository(this);

        tvGroupTitle = findViewById(R.id.tvGroupTitle);
        tvGroupSubtitle = findViewById(R.id.tvGroupSubtitle);
        rvContacts = findViewById(R.id.rvContacts);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        contactAdapter = new ContactAdapter(contact -> confirmDeleteContact(contact.getId()));
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(contactAdapter);

        FloatingActionButton fabAddContact = findViewById(R.id.fabAddContact);
        fabAddContact.setOnClickListener(v -> promptAddContactMenu());

        loadGroupAndContacts();
    }

    private void loadGroupAndContacts() {
        executor.execute(() -> {
            final Group group = groupRepo.getGroupById(groupId);
            final List<Contact> contacts = groupRepo.getContactsByGroup(groupId);
            runOnUiThread(() -> {
                if (group != null) {
                    tvGroupTitle.setText(group.getName());
                    tvGroupSubtitle.setText(group.getDescription() == null ? "" : group.getDescription());
                }
                contactAdapter.submitContacts(contacts);
                if (contacts == null || contacts.isEmpty()) {
                    tvEmptyState.setVisibility(android.view.View.VISIBLE);
                    rvContacts.setVisibility(android.view.View.INVISIBLE);
                } else {
                    tvEmptyState.setVisibility(android.view.View.GONE);
                    rvContacts.setVisibility(android.view.View.VISIBLE);
                }
            });
        });
    }

    private void promptAddContactMenu() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_add_contacts))
                .setItems(new String[]{getString(R.string.dialog_add_one), getString(R.string.dialog_paste_many)},
                        (dialog, which) -> {
                            if (which == 0) promptSingleContact();
                            else promptBulkPaste();
                        })
                .show();
    }

    private void promptSingleContact() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText etName = new EditText(this);
        etName.setHint(getString(R.string.hint_contact_name));

        EditText etPhone = new EditText(this);
        etPhone.setHint(getString(R.string.hint_contact_phone));
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        layout.addView(etName);
        layout.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_contact))
                .setView(layout)
                .setPositiveButton(getString(R.string.common_add), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    saveSingleContact(name, phone);
                })
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void saveSingleContact(String name, String phone) {
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.contact_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            Toast.makeText(this, getString(R.string.invalid_phone), Toast.LENGTH_LONG).show();
            return;
        }
        executor.execute(() -> {
            groupRepo.addContact(name, normalized, groupId);
            loadGroupAndContacts();
        });
    }

    private void promptBulkPaste() {
        EditText etBulk = new EditText(this);
        etBulk.setHint(getString(R.string.hint_bulk_paste));
        etBulk.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etBulk.setMinLines(8);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);
        layout.addView(etBulk);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_paste_contacts))
                .setView(layout)
                .setPositiveButton(getString(R.string.common_import), (dialog, which) ->
                        importBulk(etBulk.getText().toString()))
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void importBulk(String text) {
        List<String[]> rows = new ArrayList<>();
        String[] lines = text.split("\n");
        int skipped = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("[,;]");
            if (parts.length < 2) { skipped++; continue; }

            String name = parts[0].trim();
            String phone = normalizePhone(parts[parts.length - 1].trim());
            if (name.isEmpty() || phone == null) { skipped++; continue; }

            rows.add(new String[]{name, phone});
        }

        if (rows.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_valid_rows), Toast.LENGTH_LONG).show();
            return;
        }

        final int skippedCount = skipped;
        executor.execute(() -> {
            groupRepo.bulkAddContacts(groupId, rows);
            loadGroupAndContacts();
            runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.imported_contacts, rows.size())
                            + (skippedCount > 0 ? " (" + skippedCount + " rows skipped)." : "."),
                    Toast.LENGTH_LONG).show());
        });
    }

    private void confirmDeleteContact(long contactId) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_remove_contact))
                .setPositiveButton(getString(R.string.dialog_positive_remove), (dialog, which) -> executor.execute(() -> {
                    groupRepo.deleteContact(contactId);
                    loadGroupAndContacts();
                }))
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    /**
     * Converts any common Kenyan phone format into +254 international format.
     * Returns null when the number cannot be parsed.
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String p = raw.replaceAll("[^0-9+]", "");

        if (p.isEmpty()) return null;

        if (p.startsWith("+")) p = p.substring(1);

        if (p.startsWith("254") && p.length() >= 12) return "+" + p;

        if (p.startsWith("0") && p.length() == 10) return "+254" + p.substring(1);

        if (p.startsWith("7") && p.length() == 9) return "+254" + p;

        if (p.length() == 12 && p.startsWith("254")) return "+" + p;

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}