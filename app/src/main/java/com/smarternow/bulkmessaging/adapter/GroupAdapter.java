package com.smarternow.bulkmessaging.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.model.GroupWithCount;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(GroupWithCount group);
    }

    public interface OnGroupLongClickListener {
        void onGroupLongClick(long groupId);
    }

    private final List<GroupWithCount> groups = new ArrayList<>();
    private OnGroupClickListener clickListener;
    private OnGroupLongClickListener longClickListener;

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnGroupLongClickListener(OnGroupLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void submitGroups(List<GroupWithCount> newGroups) {
        groups.clear();
        if (newGroups != null) groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    public String getGroupName(long groupId) {
        for (GroupWithCount g : groups) {
            if (g.getId() == groupId) return g.getName();
        }
        return "";
    }

    public String getGroupDescription(long groupId) {
        for (GroupWithCount g : groups) {
            if (g.getId() == groupId) return g.getDescription();
        }
        return "";
    }

    public GroupWithCount getGroupById(long groupId) {
        for (GroupWithCount g : groups) {
            if (g.getId() == groupId) return g;
        }
        return null;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupWithCount group = groups.get(position);
        holder.bind(group);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onGroupClick(group);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onGroupLongClick(group.getId());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvDescription;
        private final TextView tvCount;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGroupName);
            tvDescription = itemView.findViewById(R.id.tvGroupDescription);
            tvCount = itemView.findViewById(R.id.tvGroupCount);
        }

        void bind(GroupWithCount group) {
            tvName.setText(group.getName());
            tvDescription.setText(group.getDescription());
            tvCount.setText(itemView.getContext().getString(R.string.contacts_suffix, group.getContactCount()));
        }
    }
}