package com.colinsong.notify;

import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.widget.TextView;
import android.view.View;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import android.view.LayoutInflater;
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<String> notificationList;

    public NotificationAdapter(List<String> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String notificationContent = notificationList.get(position);
        holder.notificationTextView.setText(notificationContent);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView notificationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            notificationTextView = itemView.findViewById(R.id.notificationTextView);
        }
    }
}
