package com.smarternow.bulkmessaging.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    private final List<Contact> contacts = new ArrayList<>();
    private final OnContactClickListener listener;

    public ContactAdapter(OnContactClickListener listener) {
        this.listener = listener;
    }

    public void submitContacts(List<Contact> newContacts) {
        contacts.clear();
        if (newContacts != null) contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.bind(contact);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onContactClick(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvPhone;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactPhone);
        }

        void bind(Contact contact) {
            tvName.setText(contact.getName());
            tvPhone.setText(contact.getPhoneNumber());
        }
    }
}