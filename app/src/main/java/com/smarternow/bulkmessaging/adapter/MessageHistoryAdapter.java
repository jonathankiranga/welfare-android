package com.smarternow.bulkmessaging.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarternow.bulkmessaging.R;
import com.smarternow.bulkmessaging.model.SmsMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageHistoryAdapter extends RecyclerView.Adapter<MessageHistoryAdapter.MessageViewHolder> {

    public interface OnMessageClickListener {
        void onMessageClick(SmsMessage message);
    }

    private final List<SmsMessage> messages = new ArrayList<>();
    private final OnMessageClickListener listener;
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public MessageHistoryAdapter(OnMessageClickListener listener) {
        this.listener = listener;
    }

    public void submitMessages(List<SmsMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        SmsMessage message = messages.get(position);
        holder.bind(message);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(message);
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final CardView card;
        private final TextView tvMessage;
        private final TextView tvTarget;
        private final TextView tvStatus;
        private final TextView tvTime;
        private final TextView tvRecipients;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardMessage);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
            tvTarget = itemView.findViewById(R.id.tvMessageTarget);
            tvStatus = itemView.findViewById(R.id.tvMessageStatus);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
            tvRecipients = itemView.findViewById(R.id.tvMessageRecipients);
        }

        void bind(SmsMessage message) {
            tvMessage.setText(message.getMessageText());
            tvTarget.setText(message.getTargetGroupName());
            tvRecipients.setText(itemView.getContext().getString(R.string.recipients_suffix, message.getRecipientCount()));
            tvTime.setText(dateFormat.format(new Date(message.getTimestamp())));
            tvStatus.setText(message.getStatus());

            int bg;
            switch (message.getStatus()) {
                case SmsMessage.STATUS_SENT:
                case SmsMessage.STATUS_ACCEPTED:
                    bg = Color.parseColor("#1B5E20");
                    break;
                case SmsMessage.STATUS_REJECTED:
                case SmsMessage.STATUS_FAILED:
                    bg = Color.parseColor("#B71C1C");
                    break;
                default:
                    bg = Color.parseColor("#F57F17");
                    break;
            }
            tvStatus.setBackgroundColor(bg);
        }
    }
}