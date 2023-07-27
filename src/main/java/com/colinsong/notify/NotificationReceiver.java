package com.colinsong.notify;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;
public class NotificationReceiver extends NotificationListenerService {

    private static List<String> notificationList;
    private static NotificationAdapter notificationAdapter;
    public NotificationReceiver() {
        // 空的無參數構造函數

    }

    public NotificationReceiver(List<String> notificationList, NotificationAdapter notificationAdapter) {
        this.notificationList = notificationList;
        this.notificationAdapter = notificationAdapter;
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (this.notificationList != null && this.notificationAdapter != null) {
            String notificationTitle = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            String notificationContent = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
            String notificationInfo = "Title: " + notificationTitle + "\nContent: " + notificationContent;

            notificationList.add(notificationInfo);
            notificationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal logic if needed
    }

}