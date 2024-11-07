package com.colinsong.notify;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.widget.TextView;
import android.view.View;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;


public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<String> notificationList;
    private List<Integer> backgroundColors;
    public NotificationAdapter(List<String> notificationList) {
        this.notificationList = notificationList;


    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    // 產生隨機半透明顏色的方法
    private int getRandomColor() {
        Random random = new Random();
        int alpha = 32; // 設定 alpha 值為 128，使顏色半透明
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String notificationContent = notificationList.get(position);
        SpannableString spannableNotification = new SpannableString(notificationContent);

        // 設定背景顏色
        // 設定隨機背景顏色
        int randomColor = getRandomColor();
        holder.itemView.setBackgroundColor(randomColor);



        // 檢查通知內容是否包含 "colin" 這個單詞，如果包含，設置字體顏色
        if (notificationContent.toLowerCase().contains("colin")) {
            int colinColor = Color.RED;
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(colinColor);
            spannableNotification.setSpan(colorSpan, 0, notificationContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        // 設置Item間距，這裡使用RecyclerView的ItemDecoration來添加間距
        if (position != 0) {
            int spacingInPixels = 16; // 設置間距大小（單位：像素）
            holder.itemView.setPadding(0, spacingInPixels, 0, 0);
        }

        holder.notificationTextView.setText(spannableNotification);
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
