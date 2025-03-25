package com.colinsong.notify;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Typeface;
import android.widget.TextView;
import android.view.View;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import android.util.Log;


public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private static final String TAG = "NotificationAdapter";
    private List<NotificationItem> notificationList;

    public NotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            NotificationItem item = notificationList.get(position);

            // 設置 ID 和應用名稱
            holder.appNameTextView.setText(String.format("%d. %s", item.getId(), item.getAppName()));

            // 設置時間戳
            holder.timeTextView.setText(item.getTimestamp());

            // 設置標題和內容
            if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                holder.titleTextView.setVisibility(View.VISIBLE);
                holder.titleTextView.setText(item.getTitle());
            } else {
                holder.titleTextView.setVisibility(View.GONE);
            }

            if (item.getContent() != null) {
                holder.contentTextView.setText(item.getContent());
            } else {
                holder.contentTextView.setText("");
            }

            // 設置隨機背景色 (淺色)
            int color = getRandomPastelColor();
            if (holder.cardView != null) {
                holder.cardView.setCardBackgroundColor(color);
            }

            // 檢查是否包含特定關鍵字
            boolean containsKeyword = false;
            if (item.getTitle() != null && item.getTitle().toLowerCase().contains("colin")) {
                containsKeyword = true;
            }
            if (item.getContent() != null && item.getContent().toLowerCase().contains("colin")) {
                containsKeyword = true;
            }

            // 如果包含特定關鍵字，設置高亮顯示
            if (containsKeyword) {
                if (holder.cardView != null) {
                    holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF4E5")); // 溫暖的背景色
                }
                holder.titleTextView.setTextColor(Color.RED);
                holder.contentTextView.setTextColor(Color.RED);
                holder.contentTextView.setTypeface(holder.contentTextView.getTypeface(), Typeface.BOLD);
            } else {
                holder.titleTextView.setTextColor(Color.parseColor("#333333"));
                holder.contentTextView.setTextColor(Color.parseColor("#666666"));
                holder.contentTextView.setTypeface(null, Typeface.NORMAL);
            }
        } catch (Exception e) {
            Log.e(TAG, "綁定視圖時出錯: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return notificationList != null ? notificationList.size() : 0;
    }

    // 產生隨機柔和顏色
    private int getRandomPastelColor() {
        // 產生較淺的顏色
        Random random = new Random();
        final int baseRed = 230;   // 基礎紅色值 (較高意味著顏色較淺)
        final int baseGreen = 230; // 基礎綠色值
        final int baseBlue = 230;  // 基礎藍色值
        final int range = 25;      // 顏色變化範圍

        int red = baseRed - random.nextInt(range);
        int green = baseGreen - random.nextInt(range);
        int blue = baseBlue - random.nextInt(range);

        return Color.rgb(red, green, blue);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameTextView;
        TextView timeTextView;
        TextView titleTextView;
        TextView contentTextView;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            try {
                // 嘗試兩種方式：
                // 1. 如果 itemView 本身是 CardView
                if (itemView instanceof CardView) {
                    cardView = (CardView) itemView;
                } else {
                    // 2. 在 itemView 中查找 CardView
                    cardView = itemView.findViewById(R.id.cardView);
                }

                appNameTextView = itemView.findViewById(R.id.appNameTextView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
            } catch (Exception e) {
                Log.e("NotificationAdapter", "ViewHolder初始化錯誤: " + e.getMessage());
            }
        }
    }
}