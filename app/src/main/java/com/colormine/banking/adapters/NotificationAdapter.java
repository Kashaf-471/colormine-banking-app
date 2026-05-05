package com.colormine.banking.adapters;

import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
import com.colormine.banking.SendMoneyActivity;
import com.colormine.banking.models.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        if (notification != null) {
            holder.tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
            holder.tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");
            
            long timestamp = notification.getTimestamp();
            if (timestamp > 0) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        timestamp, 
                        System.currentTimeMillis(), 
                        DateUtils.MINUTE_IN_MILLIS);
                holder.tvTime.setText(relativeTime);
            } else {
                holder.tvTime.setText("Recently");
            }

            holder.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            if ("request".equals(notification.getType())) {
                // Style for clickable request notifications
                holder.tvActionHint.setVisibility(View.VISIBLE);
                holder.tvIcon.setText("💰");
                holder.tvIcon.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.bg_icon_purple));
                
                holder.itemView.setOnClickListener(v -> {
                    if (notification.getSenderEmail() != null) {
                        Intent intent = new Intent(v.getContext(), SendMoneyActivity.class);
                        intent.putExtra("request_recipient_email", notification.getSenderEmail());
                        intent.putExtra("request_amount", notification.getAmount());
                        v.getContext().startActivity(intent);
                    }
                });
            } else {
                // Style for standard notifications
                holder.tvActionHint.setVisibility(View.GONE);
                holder.tvIcon.setText("🔔");
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvMessage, tvIcon, tvActionHint;
        View unreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvIcon = itemView.findViewById(R.id.tv_notif_icon);
            tvActionHint = itemView.findViewById(R.id.tv_notif_action_hint);
            unreadDot = itemView.findViewById(R.id.notif_unread_dot);
        }
    }
}
