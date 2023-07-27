package com.colinsong.notify;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.media.MediaPlayer;
import android.text.TextUtils;

import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
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

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            String appName =getAppNameFromPackage(sbn.getPackageName());
            if (appName.equals("jp.naver.line.android") || appName.equals("com.google.android.youtube")) {
                return;
            }
            String notificationTitle = appName + "\n" + timeStamp + "\n" + sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            String notificationContent = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);

            if (TextUtils.isEmpty(notificationTitle) || TextUtils.isEmpty(notificationContent)) {
                return;
            }

            // 正則表達式，不論大小寫，匹配 "colin" 這個單詞
            Pattern pattern = Pattern.compile("colin", Pattern.CASE_INSENSITIVE);
            Matcher titleMatcher = pattern.matcher(notificationTitle);
            Matcher contentMatcher = pattern.matcher(notificationContent);

            // 檢查通知標題和內容是否匹配 "colin" 這個單詞
            boolean containsColin = titleMatcher.find() || contentMatcher.find();

            // 創建一個 SpannableString，用於設定字體樣式
            SpannableString spannableTitle = new SpannableString(notificationTitle);
            SpannableString spannableContent = new SpannableString(notificationContent);

            if (containsColin) {
                // 如果通知中包含 "colin" 這個單詞，將字體設定為紅色和粗體
                int colinColor = Color.RED;
                StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(colinColor);

                // 設定通知標題和內容的字體樣式
                if (notificationTitle != null) {
                    spannableTitle.setSpan(boldSpan, 0, notificationTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    spannableTitle.setSpan(colorSpan, 0, notificationTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (notificationContent != null) {
                    spannableContent.setSpan(boldSpan, 0, notificationContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    spannableContent.setSpan(colorSpan, 0, notificationContent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }

                triggerRingtone();
            }

            // 組合通知標題和內容，並加入到通知列表
            String notificationInfo =  spannableTitle + "\n " + spannableContent;
            notificationList.add(notificationInfo);
            Collections.reverse(notificationList);
            notificationAdapter.notifyDataSetChanged();
        }
    }

    private String getAppNameFromPackage(String packageName) {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName;
        }
    }
    public static void addNotification(String notificationInfo) {
        notificationList.add(0,notificationInfo);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal logic if needed
    }

    private void triggerRingtone() {
        // 在這裡觸發手機響鈴，例如使用Ringtone或MediaPlayer播放鈴聲音頻
        // 你可以自定義觸發響鈴的邏輯，這裡只是一個簡單示例
        // 注意：在實際開發中，需要處理權限和不同手機型號的兼容性
        // 獲取系統的預設響鈴音效
        // 獲取系統的預設響鈴音效
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.windows);

        // 播放鈴聲音頻
        if (mediaPlayer != null) {
            mediaPlayer.start();
            // 監聽音頻播放完成，釋放MediaPlayer
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
        }

    }

}