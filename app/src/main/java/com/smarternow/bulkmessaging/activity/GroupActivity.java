package com.smarternow.bulkmessaging.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.adapter.GroupAdapter;
import com.smarternow.bulkmessaging.model.GroupWithCount;
import com.smarternow.bulkmessaging.repository.GroupRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screens for managing groups: create, rename, delete and open a group's
 * contact list. Contacts are what the bulk SMS targets.
 */
public class GroupActivity extends AppCompatActivity {

    private RecyclerView rvGroups;
    private GroupAdapter groupAdapter;
    private GroupRepository groupRepo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        groupRepo = new GroupRepository(this);

        rvGroups = findViewById(R.id.rvGroups);
        groupAdapter = new GroupAdapter();
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupAdapter);

        groupAdapter.setOnGroupClickListener(group -> startGroupDetail(group.getId()));

        groupAdapter.setOnGroupLongClickListener(groupId -> {
            new AlertDialog.Builder(this)
                    .setTitle(groupAdapter.getGroupName(groupId))
                    .setItems(new String[]{getString(R.string.action_rename), getString(R.string.action_add_edit_contacts), getString(R.string.dialog_positive_delete)},
                            (dialog, which) -> {
                                switch (which) {
                                    case 0: promptRename(groupId); break;
                                    case 1: startGroupDetail(groupId); break;
                                    case 2: confirmDelete(groupId); break;
                                }
                            })
                    .show();
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAddGroup);
        fabAdd.setOnClickListener(v -> promptCreate());

        loadGroups();
    }

    private void loadGroups() {
        executor.execute(() -> {
            final List<GroupWithCount> groups = groupRepo.getGroupsWithContactCount();
            runOnUiThread(() -> groupAdapter.submitGroups(groups));
        });
    }

    private void promptCreate() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, 0);

        EditText etName = new EditText(this);
        etName.setHint(getString(R.string.hint_group_name));

        EditText etDesc = new EditText(this);
        etDesc.setHint(getString(R.string.hint_group_desc));

        layout.addView(etName);
        layout.addView(etDesc);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_new_group))
                .setView(layout)
                .setPositiveButton(getString(R.string.common_create), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.group_name_required), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    executor.execute(() -> {
                        groupRepo.createGroup(name, etDesc.getText().toString().trim());
                        loadGroups();
                    });
                })
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void promptRename(long groupId) {
        EditText etName = new EditText(this);
        etName.setText(groupAdapter.getGroupName(groupId));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_rename_group))
                .setView(etName)
                .setPositiveButton(getString(R.string.common_save), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.name_empty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    executor.execute(() -> {
                        groupRepo.updateGroup(groupId, name,
                                groupAdapter.getGroupDescription(groupId));
                        loadGroups();
                    });
                })
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void confirmDelete(long groupId) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_group))
                .setMessage(getString(R.string.dialog_delete_group_msg))
                .setPositiveButton(getString(R.string.dialog_positive_delete), (dialog, which) -> executor.execute(() -> {
                    groupRepo.deleteGroup(groupId);
                    loadGroups();
                }))
                .setNegativeButton(getString(R.string.dialog_negative_cancel), null)
                .show();
    }

    private void startGroupDetail(long groupId) {
        startActivity(new android.content.Intent(this, GroupDetailActivity.class)
                .putExtra("GROUP_ID", groupId));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}