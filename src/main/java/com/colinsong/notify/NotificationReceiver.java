package com.colinsong.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationReceiver extends NotificationListenerService {
    private static final String TAG = "NotificationReceiver";
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "foreground_service_channel";
    private MutableLiveData<List<String>> notificationLiveData = new MutableLiveData<>();
    private MyDatabaseHelper dbHelper;
    // 從MainActivity直接訪問的靜態變量
    public static List<String> notificationList;
    public static NotificationAdapter notificationAdapter;
    private PowerManager.WakeLock wakeLock;

    // 無參數構造函數，系統將使用此構造函數來創建服務
    public NotificationReceiver() {
        Log.i(TAG, "NotificationReceiver 無參構造函數被調用");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        currentInstance = this;
        Log.i(TAG, "NotificationReceiver onCreate 開始");

        // 初始化資料庫 Helper
        dbHelper = MyDatabaseHelper.getInstance(this);

        // 取得喚醒鎖，防止服務被系統休眠
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "NotificationReceiver::WakeLockTag");
        wakeLock.acquire(30*60*1000L); // 獲取30分鐘的喚醒鎖

        // 建立 NotificationChannel 並啟動 Foreground Service
        createNotificationChannel();
        startForeground(ONGOING_NOTIFICATION_ID, createNotification("服務正在運行"));

        // 檢查靜態引用狀態
        checkStaticReferences();

        // 嘗試從MainActivity中拿到列表和適配器
        if (notificationList == null) {
            notificationList = MainActivity.notificationList;
            Log.d(TAG, "從MainActivity獲取notificationList: " + (notificationList != null));
        }

        if (notificationAdapter == null) {
            notificationAdapter = MainActivity.notificationAdapter;
            Log.d(TAG, "從MainActivity獲取notificationAdapter: " + (notificationAdapter != null));
        }

        // 測試服務是否正常運行
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "延遲測試靜態引用: notificationList=" + (notificationList != null) +
                        ", notificationAdapter=" + (notificationAdapter != null));
            }
        }, 3000);

        Log.i(TAG, "NotificationReceiver onCreate 完成");
    }

    // 檢查靜態引用狀態
    private void checkStaticReferences() {
        Log.d(TAG, "檢查靜態引用: notificationList=" + (notificationList != null) +
                ", notificationAdapter=" + (notificationAdapter != null));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "前景服務通知頻道",
                    NotificationManager.IMPORTANCE_HIGH // 高優先級
            );
            channel.setDescription("用於保持通知監聽服務運行的通知");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
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
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 提高優先級
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "接收到 onStartCommand");

        // 如果 notificationList 或 notificationAdapter 為 null，嘗試從 MainActivity 獲取
        if (notificationList == null) {
            notificationList = MainActivity.notificationList;
            Log.d(TAG, "onStartCommand: 從MainActivity獲取notificationList: " + (notificationList != null));
        }

        if (notificationAdapter == null) {
            notificationAdapter = MainActivity.notificationAdapter;
            Log.d(TAG, "onStartCommand: 從MainActivity獲取notificationAdapter: " + (notificationAdapter != null));
        }

        // 如果服務被系統殺死後重新創建，我們希望它繼續運行
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind 被調用: " + (intent != null ? intent.getAction() : "null"));
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "通知監聽服務已連接到系統");

        // 嘗試請求當前所有通知，測試服務是否正常工作
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                StatusBarNotification[] activeNotifications = getActiveNotifications();
                if (activeNotifications != null) {
                    Log.d(TAG, "目前系統中有 " + activeNotifications.length + " 條活躍通知");
                    // 檢查靜態引用
                    checkStaticReferences();
                }
            } catch (Exception e) {
                Log.e(TAG, "無法獲取活躍通知", e);
            }
        }

        // 廣播服務已連接
        Intent intent = new Intent("com.colinsong.notify.NOTIFICATION_LISTENER_CONNECTED");
        sendBroadcast(intent);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.e(TAG, "通知監聽服務與系統斷開連接");

        // 嘗試重新連接
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, NotificationReceiver.class));
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "NotificationReceiver 服務被終止，正在嘗試重新啟動");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentInstance = null;
        // 釋放喚醒鎖
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // 停止前台服務
        stopForeground(true);

        // 服務被終止時重新啟動
        Intent restartServiceIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent);
        } else {
            startService(restartServiceIntent);
        }

        // 廣播服務狀態變化通知
        Intent broadcastIntent = new Intent("com.colinsong.notify.SERVICE_STATE_CHANGED");
        broadcastIntent.putExtra("isRunning", false);
        sendBroadcast(broadcastIntent);

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationPosted 被調用，包名: " + sbn.getPackageName());
        try {
            // 檢查靜態引用
            if (notificationList == null) {
                notificationList = MainActivity.notificationList;
                Log.i(TAG, "onNotificationPosted: 嘗試獲取notificationList: " + (notificationList != null));
            }

            if (notificationAdapter == null) {
                notificationAdapter = MainActivity.notificationAdapter;
                Log.i(TAG, "onNotificationPosted: 嘗試獲取notificationAdapter: " + (notificationAdapter != null));
            }

            if (notificationList != null && notificationAdapter != null) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String appName = getAppNameFromPackage(sbn.getPackageName());

                Log.d(TAG, "處理來自 " + appName + " 的通知");

                // 略過某些應用程式的通知
                if (!appName.equals("Chat")) {
                    // 如果不是 Chat，則跳過此通知
                    Log.d(TAG, "不是 Chat 應用的通知，忽略: " + appName);
                    return;
                }

                String notificationTitle = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
                String notificationContent = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);

                Log.d(TAG, "通知標題: " + notificationTitle + ", 內容: " + notificationContent);

                // 略過空標題或內容的通知
                if (TextUtils.isEmpty(notificationTitle) || TextUtils.isEmpty(notificationContent)) {
                    Log.d(TAG, "標題或內容為空，略過此通知");
                    return;
                }


                // 正則表達式，不論大小寫，匹配 "colin" 這個單詞
                Pattern pattern = Pattern.compile("colin", Pattern.CASE_INSENSITIVE);

                boolean isGoogleChat = sbn.getPackageName().equals("com.google.android.apps.dynamite") ||
                        sbn.getPackageName().equals("com.google.android.talk");
                boolean containsTeams = false;

                Matcher titleMatcher = pattern.matcher(notificationTitle);
                Matcher contentMatcher = pattern.matcher(notificationContent);

                // 檢查通知標題和內容是否匹配 "colin" 這個單詞
                boolean containsColin = titleMatcher.find() || contentMatcher.find();

                // 創建一個 SpannableString，用於設定字體樣式
                SpannableString spannableTitle = new SpannableString(notificationTitle);
                SpannableString spannableContent = new SpannableString(notificationContent);

                if (( isGoogleChat) ) {
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

                    Log.d(TAG, "觸發鈴聲通知");
                    if ( containsColin) {
                        triggerRingtone();
                    }

                }

                // 組合通知標題和內容，並加入到通知列表
                String formattedTitle = appName + "\n" + timeStamp + "\n" + spannableTitle;
                final String finalNotificationInfo = formattedTitle + "\n " + spannableContent;

                // 直接在主線程上更新 UI
                Handler handler = new Handler(getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "嘗試添加通知到列表: " + (notificationList != null));
                        if (notificationList != null) {
                            notificationList.add(0, finalNotificationInfo);

                            if (notificationAdapter != null) {
                                Log.d(TAG, "通知適配器，更新UI");
                                notificationAdapter.notifyDataSetChanged();
                            } else {
                                Log.e(TAG, "通知適配器為空，無法更新UI");
                            }
                        } else {
                            Log.e(TAG, "通知列表為空，無法添加通知");
                        }
                    }
                });

                // 寫入資料庫
                writeToDatabase(appName, notificationTitle, notificationContent, timeStamp);

                Log.i(TAG, "已接收並處理通知: " + appName);
            } else {
                Log.e(TAG, "通知列表或適配器為空，無法處理通知");
                // 發送廣播請求MainActivity重新設置引用
                Intent intent = new Intent("com.colinsong.notify.REQUEST_REFERENCES");
                sendBroadcast(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "處理通知時發生錯誤", e);
            e.printStackTrace();
        }
    }

    private String getAppNameFromPackage(String packageName) {
        PackageManager packageManager = getApplicationContext().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "無法獲取應用名稱: " + packageName, e);
            return packageName;
        }
    }

    public static void addNotification(String notificationInfo) {
        try {
            if (notificationList != null) {
                notificationList.add(0, notificationInfo);

                if (notificationAdapter != null) {
                    // 使用主線程來更新UI
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notificationAdapter.notifyDataSetChanged();
                        }
                    });

                    Log.i(TAG, "通過靜態方法添加了通知");
                } else {
                    Log.e(TAG, "適配器為空，無法更新UI");
                }
            } else {
                Log.e(TAG, "通知列表為空，無法添加通知");
            }
        } catch (Exception e) {
            Log.e(TAG, "添加通知時發生錯誤", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // 處理通知移除邏輯，如果需要的話
        Log.d(TAG, "通知已移除: " + sbn.getPackageName());
    }

    private void triggerSystemAlarm() {
        try {
            // 設定鬧鐘時間為當前時間加5秒（可以根據需要調整）
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 1);

            // 創建鬧鐘意圖
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "通知觸發的鬧鐘");
            alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY));
            alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE));
            alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true); // 跳過UI直接設置鬧鐘
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 啟動鬧鐘應用
            startActivity(alarmIntent);

            Log.d(TAG, "已觸發系統鬧鐘");
        } catch (Exception e) {
            Log.e(TAG, "觸發系統鬧鐘時發生錯誤", e);
        }
    }

    private MediaPlayer mediaPlayer;

    private void triggerRingtone() {
        try {
            // 如果已有播放器正在播放，先停止並釋放
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }

            // 創建新的 MediaPlayer
            mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.windows);

            if (mediaPlayer != null) {
                // 設置循環播放
                mediaPlayer.setLooping(true);
                mediaPlayer.start();

                // 啟動一個活動來顯示提醒對話框
                Intent dialogIntent = new Intent(this, AlarmDialogActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);

                // 當對話框關閉時，廣播接收器會接收到訊息並停止播放
            }
        } catch (Exception e) {
            Log.e(TAG, "播放鈴聲時發生錯誤", e);
        }
    }

    // 停止播放的方法（將被從對話框活動調用）
    public static void stopRingtone() {
        // 這個靜態方法需要從外部訪問當前的 NotificationReceiver 實例
        if (currentInstance != null && currentInstance.mediaPlayer != null) {
            if (currentInstance.mediaPlayer.isPlaying()) {
                currentInstance.mediaPlayer.stop();
            }
            currentInstance.mediaPlayer.release();
            currentInstance.mediaPlayer = null;
        }
    }

    // 保存當前實例的靜態引用
    private static NotificationReceiver currentInstance;




    public MutableLiveData<List<String>> getNotificationLiveData() {
        return notificationLiveData;
    }

    private void writeToDatabase(String appName, String title, String content, String timeStamp) {
        try {
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

            // 檢查插入是否成功
            if (newRowId == -1) {
                Log.e(TAG, "資料庫寫入失敗: " + appName + ", " + title);
            } else {
                Log.d(TAG, "成功寫入資料庫，ID: " + newRowId);
            }

            // 釋放資源
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "寫入資料庫時發生錯誤", e);
        }
    }
}