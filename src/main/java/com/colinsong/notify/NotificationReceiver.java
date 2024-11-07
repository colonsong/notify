package com.colinsong.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
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

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

public class NotificationReceiver extends NotificationListenerService {
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "foreground_service_channel";
    private MutableLiveData<List<String>> notificationLiveData = new MutableLiveData<>();
    private MyDatabaseHelper dbHelper;
    private static List<String> notificationList;
    private static NotificationAdapter notificationAdapter;

    public NotificationReceiver() {
        // 空的無參數構造函數

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 建立 NotificationChannel 並啟動 Foreground Service
        createNotificationChannel();
        startForeground(ONGOING_NOTIFICATION_ID, createNotification("服務正在運行"));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "前景服務通知頻道",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("監聽通知服務")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification) // 確認有有效的圖標
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
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
            if (appName.equals("jp.naver.line.android")
                    || appName.equals("com.google.android.youtube")
                  //  || appName.equals("com.instagram.android")
            ) {
                return;
            }
            String notificationTitle =  sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            String notificationContent = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);

            if (TextUtils.isEmpty(notificationTitle) || TextUtils.isEmpty(notificationContent)) {
                return;
            }

            // 正則表達式，不論大小寫，匹配 "colin" 這個單詞
            Pattern pattern = Pattern.compile("colin", Pattern.CASE_INSENSITIVE);

            boolean containsTeams = appName.equals("com.google.android.gm")
                    && notificationTitle != null && (notificationTitle.contains("Martin")
                    || notificationTitle.contains("Zumi")
                    || notificationTitle.contains("Yuki")
                    || notificationTitle.contains("Christy")
                    || notificationTitle.contains("Nick")
                    || notificationTitle.contains("Ken")
                    || notificationTitle.contains("Chinsheng")
                    || notificationTitle.contains("YuHsiang")
                    || notificationTitle.contains("Ted")
                    || notificationTitle.contains("JianKai")
                    || notificationTitle.contains("David")
                    || notificationTitle.contains("Ben")
                    || notificationTitle.contains("Ted")
            );

            Matcher titleMatcher = pattern.matcher(notificationTitle);
            Matcher contentMatcher = pattern.matcher(notificationContent);

            // 檢查通知標題和內容是否匹配 "colin" 這個單詞
            boolean containsColin = titleMatcher.find() || contentMatcher.find();

            // 創建一個 SpannableString，用於設定字體樣式
            SpannableString spannableTitle = new SpannableString(notificationTitle);
            SpannableString spannableContent = new SpannableString(notificationContent);

            if (containsColin || containsTeams) {
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
             notificationTitle = appName + "\n" + timeStamp + "\n" + spannableTitle;
            String notificationInfo =  notificationTitle + "\n " + spannableContent;
            this.notificationList.add(0, notificationInfo);

            // 寫入資料庫
            writeToDatabase(appName, notificationTitle, notificationContent, timeStamp);


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
        notificationList.add(notificationInfo);
        notificationAdapter.notifyDataSetChanged();
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
    public MutableLiveData<List<String>> getNotificationLiveData() {
        return notificationLiveData;
    }
    private void writeToDatabase(String appName, String title, String content, String timeStamp) {
        // 創建資料庫的 Helper 類的實例
        dbHelper = MyDatabaseHelper.getInstance(this);

        // 取得可寫入資料庫的實例
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 將資料封裝成 ContentValues
        ContentValues values = new ContentValues();
        values.put("timestamp", timeStamp);
        values.put("packageName", appName);
        values.put("title", title);
        values.put("content", content);

        // 插入資料到 messages 資料表中
        long newRowId = db.insert("messages", null, values);

        // 釋放資源
        db.close();

        notificationAdapter.notifyDataSetChanged();
        // 在這裡你可以做一些錯誤處理或日誌紀錄等，確保資料正確寫入資料庫
    }

}