package com.colormine.banking.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
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
        holder.tvTitle.setText(notification.getTitle());
        
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp(), 
                System.currentTimeMillis(), 
                DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(relativeTime);

        holder.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        if ("request".equals(notification.getType())) {
            holder.itemView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), com.colormine.banking.SendMoneyActivity.class);
                intent.putExtra("request_recipient_email", notification.getSenderEmail());
                intent.putExtra("request_amount", notification.getAmount());
                v.getContext().startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
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
        TextView tvTitle, tvTime;
        View unreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            unreadDot = itemView.findViewById(R.id.notif_unread_dot);
        }
    }
}
