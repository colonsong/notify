package com.colinsong.notify;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

public class NotificationReceiver extends NotificationListenerService {
    private MutableLiveData<List<String>> notificationLiveData = new MutableLiveData<>();
    private MyDatabaseHelper dbHelper;
    private static List<String> notificationList;
    private static NotificationAdapter notificationAdapter;

    public NotificationReceiver() {

    }

    public NotificationReceiver(List<String> notificationList, NotificationAdapter notificationAdapter) {
        this.notificationList = notificationList;
        this.notificationAdapter = notificationAdapter;
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {


        if (this.notificationList != null && this.notificationAdapter != null) {

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            String appName =sbn.getPackageName();
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


            createNotificationChannel(notificationTitle, notificationContent);

        }


    }



    private void createNotificationChannel(String notificationTitle, String notificationContent)  {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "my_channel_id";
            CharSequence channelName = "My Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            // 創建一個意圖，指定要進入的Activity
            Intent intent = new Intent(this, MainActivity.class);
            // 將意圖設置為PendingIntent，並使用getActivity()方法
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);



            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            // Create a notification for the foreground service
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle(notificationTitle)
                    .setContentIntent(pendingIntent) // 設置PendingIntent
                    .setContentText(notificationContent)
                    .setSmallIcon(R.drawable.ic_home_black_24dp)
                    .build();

            // Start the service as a foreground service

            if (!isServiceRunningInForeground()) {
                // 如果服務還不是前台服務，則調用 startForeground()
                startForeground(1, notification);
            }

        }
    }

    private boolean isServiceRunningInForeground() {
        // 取得通知管理器
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 檢查是否存在與你的服務相關聯的通知
        if (manager != null) {
            for (StatusBarNotification notification : manager.getActiveNotifications()) {
                // 檢查通知是否與你的服務相關聯
                if (notification.getId() == 1) { // 替換 YOUR_NOTIFICATION_ID 為你的通知 ID
                    return true; // 如果存在相關聯的通知，則服務處於前台狀態
                }
            }
        }
        return false; // 如果沒有相關聯的通知，則服務不處於前台狀態
    }

    private String getAppNameFromPackage(String packageName) {



        PackageManager packageManager = getApplicationContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            // 返回默认值或者处理其他逻辑
            return "Unknown"; // 例如返回一个默认值
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