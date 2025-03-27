package com.colinsong.notify;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.colinsong.notify.databinding.ActivityMainBinding;
import com.colinsong.notify.ui.notifications.NotificationsFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // 保留此列表用於與舊代碼兼容
    public static List<String> notificationList = new ArrayList<>();
    // 添加NotificationItem列表用於新的適配器
    private List<NotificationItem> notificationItemList = new ArrayList<>();
    // 新增靜態adapter讓NotificationReceiver可以訪問
    public static NotificationAdapter notificationAdapter;
    private ActivityMainBinding binding;
    private MyDatabaseHelper dbHelper;
    // 在MainActivity類的頂部，與其他常量一起定義
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private HomeFragment homeFragment;
    private DashboardFragment dashboardFragment;
    private NotificationsFragment notificationsFragment;

    // ViewModel成員變量
    private NotificationViewModel notificationViewModel;
    private MutableLiveData<List<String>> notificationLiveData = new MutableLiveData<>();

    private static final int NOTIFICATION_ID_PERMISSION_REMINDER = 1;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 200;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 201;

    // 用於廣播接收器，接收服務狀態變化
    private BroadcastReceiver serviceStateReceiver;
    private boolean isFirstRun = false;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity onCreate 開始");


        // 初始化 Fragment 實例
        homeFragment = new HomeFragment();
        dashboardFragment = new DashboardFragment();
        notificationsFragment = new NotificationsFragment();

        // 初始化 MyDatabaseHelper，使用單例模式
        dbHelper = MyDatabaseHelper.getInstance(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 創建或獲取現有的ViewModel
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        notificationList = notificationViewModel.getNotificationList();

        // 使用新的NotificationItem列表創建適配器 (不再設置給 RecyclerView)
        notificationAdapter = new NotificationAdapter(notificationItemList);

        // 確保NotificationReceiver可以訪問這些靜態變量
        setupStaticReferences();

        Button sendNotificationButton = binding.sendNotificationButton;

        // 检查通知权限并请求
        requestAllRequiredPermissions();

        // 观察 LiveData 数据并更新通知列表
        notificationLiveData.observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> notifications) {
                notificationList.clear();
                notificationList.addAll(notifications);

                // 將String列表轉換為NotificationItem列表
                updateNotificationItems();
            }
        });

        // 讀取資料庫中的通知資訊並加入到 notificationList 中
        readNotificationsFromDatabase();

        // 強制重新綁定通知監聽服務
        toggleNotificationListenerService();

        // 確保通知監聽服務正在運行
        ensureNotificationServiceRunning();

        // 註冊廣播接收器，用於監聽服務狀態
        registerServiceStateReceiver();

        // 設定定期檢查服務是否運行
        scheduleServiceCheck();

        sendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // 模擬接收通知
                    String appName = "測試應用";
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    String notificationTitle = "測試通知";
                    String notificationContent = "這是一條測試通知內容，時間戳：" + timeStamp;

                    // 形成與通知監聽器產生的格式一致的通知信息
                    String formattedTitle = appName + "\n" + timeStamp + "\n" + notificationTitle;
                    String notificationInfo = formattedTitle + "\n " + notificationContent;

                    // 添加到String列表
                    notificationList.add(0, notificationInfo);

                    // 創建NotificationItem並添加到itemList
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                    String formattedTime = outputFormat.format(new Date());

                    NotificationItem item = new NotificationItem(
                            notificationItemList.size() + 1,
                            appName,
                            formattedTime,
                            notificationTitle,
                            notificationContent
                    );
                    notificationItemList.add(0, item);
                    notificationAdapter.notifyDataSetChanged();

                    // 同時寫入資料庫，確保數據持久化
                    writeTestNotificationToDatabase(appName, notificationTitle, notificationContent, timeStamp);

                    Log.i(TAG, "已添加測試通知到列表");
                    Toast.makeText(MainActivity.this, "已添加測試通知", Toast.LENGTH_SHORT).show();

                    // 確保服務正在運行
                    ensureNotificationServiceRunning();

                    // 確保靜態引用正確設置
                    setupStaticReferences();

                    // 如果目前顯示的是主頁，通知 HomeFragment 更新
                    if (homeFragment != null && homeFragment.isVisible()) {
                        homeFragment.updateNotifications();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "添加測試通知失敗", e);
                    Toast.makeText(MainActivity.this, "添加測試通知失敗", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 處理特定品牌手機的優化
        handleBrandSpecificOptimizations();

        setupBottomNavigation();

        // 初始顯示主頁 Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit();

        Log.i(TAG, "MainActivity onCreate 完成");
    }

    // 將String列表轉換為NotificationItem列表


    private void updateNotificationItems() {
        notificationItemList.clear();
        for (String notification : notificationList) {
            NotificationItem item = convertStringToNotificationItem(notification);
            if (item != null) {
                notificationItemList.add(item);
            }
        }
        notificationAdapter.notifyDataSetChanged();
    }

    // 將String轉換為NotificationItem
    private NotificationItem convertStringToNotificationItem(String notification) {
        try {
            String[] parts = notification.split("\n", 4);
            if (parts.length >= 4) {
                String appNameWithId = parts[0]; // "count appName"
                String timestamp = parts[1];
                String title = parts[2];
                String content = parts[3].trim();

                // 解析ID和應用名
                int id = 0;
                String appName = appNameWithId;

                String[] idParts = appNameWithId.split(" ", 2);
                if (idParts.length > 1) {
                    try {
                        id = Integer.parseInt(idParts[0].trim());
                        appName = idParts[1].trim();
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "無法解析ID: " + appNameWithId, e);
                    }
                }

                return new NotificationItem(id, appName, timestamp, title, content);
            }
        } catch (Exception e) {
            Log.e(TAG, "轉換通知格式失敗: " + notification, e);
        }
        return null;
    }

    // 確保靜態引用設置正確
    private void setupStaticReferences() {
        // 這是確保NotificationReceiver可以訪問這些變量的關鍵
        NotificationReceiver.notificationList = notificationList;
        NotificationReceiver.notificationAdapter = notificationAdapter;
        Log.d(TAG, "設置靜態引用：notificationList=" + (notificationList != null) +
                ", notificationAdapter=" + (notificationAdapter != null));
    }

    // 提供靜態方法供NotificationReceiver使用
    public static NotificationAdapter getNotificationAdapter() {
        return notificationAdapter;
    }

    // 強制重新綁定通知監聽服務
    private void toggleNotificationListenerService() {
        Log.d(TAG, "嘗試重新綁定通知監聽服務");
        ComponentName thisComponent = new ComponentName(this, NotificationReceiver.class);
        PackageManager pm = getPackageManager();

        try {
            // 暫時禁用然後重新啟用通知監聽服務
            pm.setComponentEnabledSetting(thisComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            Thread.sleep(500); // 短暫延遲以確保設置生效

            pm.setComponentEnabledSetting(thisComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            Log.d(TAG, "已重新啟用通知監聽服務組件");
        } catch (Exception e) {
            Log.e(TAG, "重新綁定通知監聽服務失敗", e);
        }
    }

    // 處理不同品牌手機的特殊優化
    private void handleBrandSpecificOptimizations() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            // 小米手機的特殊處理
            requestXiaomiBatteryOptimization();
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            // 華為手機的特殊處理
            requestHuaweiBatteryOptimization();
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme") ||
                manufacturer.contains("oneplus")) {
            // OPPO/一加的特殊處理
            requestOppoBatteryOptimization();
        } else if (manufacturer.contains("vivo")) {
            // vivo手機的特殊處理
            requestVivoBatteryOptimization();
        }
    }

    // 小米手機的電池優化處理
    private void requestXiaomiBatteryOptimization() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
            intent.putExtra("package_name", getPackageName());
            intent.putExtra("package_label", getString(R.string.app_name));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "無法啟動小米電池優化設置", e);
            // 嘗試標準Android電池優化設置
            requestBatteryOptimizationExemption();
        }
    }

    // 華為手機的電池優化處理
    private void requestHuaweiBatteryOptimization() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "無法啟動華為電池優化設置", e);
            // 嘗試標準Android電池優化設置
            requestBatteryOptimizationExemption();
        }
    }

    // OPPO手機的電池優化處理
// OPPO手機的電池優化處理
    private void requestOppoBatteryOptimization() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "無法啟動OPPO電池優化設置", e);
            // 嘗試標準Android電池優化設置
            requestBatteryOptimizationExemption();
        }
    }

    // Vivo手機的電池優化處理
    private void requestVivoBatteryOptimization() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "無法啟動Vivo電池優化設置", e);
            // 嘗試標準Android電池優化設置
            requestBatteryOptimizationExemption();
        }
    }

    // 將測試通知寫入資料庫
    private void writeTestNotificationToDatabase(String appName, String title, String content, String timeStamp) {
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
                Log.e(TAG, "測試通知資料庫寫入失敗");
            } else {
                Log.i(TAG, "測試通知已寫入資料庫，ID: " + newRowId);
            }

            // 釋放資源
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "寫入測試通知到資料庫時發生錯誤", e);
        }
    }

    public void readNotificationsFromDatabase() {
        try {
            List<String> notifications = new ArrayList<>();
            notificationItemList.clear();

            // 取得可讀取的資料庫實例
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // 指定要查詢的欄位
            String[] projection = {
                    "timestamp",
                    "packageName",
                    "title",
                    "content"
            };

            // 查詢資料庫
            Cursor cursor = db.query(
                    "messages",  // 表格名稱
                    projection,  // 欄位名稱
                    null,  // WHERE 子句
                    null,  // WHERE 子句參數
                    null,  // GROUP BY 子句
                    null,  // HAVING 子句
                    "timestamp DESC"  // ORDER BY 子句
            );

            int count = cursor.getCount(); // 获取总通知数量
            Log.i(TAG, "從資料庫讀取到 " + count + " 條通知");

            // 將查詢結果加入到 notifications 中
            while (cursor.moveToNext()) {
                String timeStamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                String packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));

                // 取得 packageName 最後一個逗點後面的英文部分
                String appName = packageName;
                if (packageName.lastIndexOf(".") != -1) {
                    appName = packageName.substring(packageName.lastIndexOf(".") + 1);
                }

                // 美化時間戳格式
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                String formattedTime = timeStamp;
                try {
                    Date date = inputFormat.parse(timeStamp);
                    if (date != null) {
                        formattedTime = outputFormat.format(date);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "日期格式轉換錯誤", e);
                }

                // 創建通知項目對象
                NotificationItem item = new NotificationItem(
                        count--,
                        appName,
                        formattedTime,
                        title,
                        content
                );
                notificationItemList.add(item);

                // 同時維護String格式的通知列表用於與舊代碼兼容
                String notificationTitle = (count+1) + " " + appName + "\n" + timeStamp + "\n" + title;
                String notificationInfo = notificationTitle + "\n " + content;
                notifications.add(notificationInfo);
            }

            // 關閉 Cursor 和資料庫連接
            cursor.close();
            db.close();

            // 更新String通知列表
            notificationList.clear();
            notificationList.addAll(notifications);

            // 通知適配器數據更新
            notificationAdapter.notifyDataSetChanged();

            Log.d(TAG, "已從資料庫加載 " + notificationItemList.size() + " 條通知");
        } catch (Exception e) {
            Log.e(TAG, "讀取資料庫時發生錯誤", e);
            Toast.makeText(this, "讀取通知歷史記錄失敗", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestAllRequiredPermissions() {
        // 檢查通知監聽權限
        requestNotificationListenerPermission();

        // 檢查電池優化排除權限
        requestBatteryOptimizationExemption();
    }

    private boolean isNotificationPermissionEnabled() {
        // 檢查通知監聽權限是否已啟用
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            ComponentName cn = new ComponentName(this, NotificationReceiver.class);
            return notificationManager.isNotificationListenerAccessGranted(cn);
        }
        return false;
    }

    private void requestNotificationListenerPermission() {

        // 檢查普通通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // 請求普通通知權限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
        // 檢查通知監聽權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                ComponentName cn = new ComponentName(this, NotificationReceiver.class);
                if (!notificationManager.isNotificationListenerAccessGranted(cn)) {
                    new AlertDialog.Builder(this)
                            .setTitle("需要通知權限")
                            .setMessage("本應用需要通知訪問權限來捕獲設備上的通知。請在下一個畫面中啟用此權限。")
                            .setPositiveButton("前往設定", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                                startActivity(intent);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                        .setTitle("需要電池優化排除")
                        .setMessage("為確保應用能夠持續接收通知，請在下一個畫面中允許本應用不受電池優化限制。")
                        .setPositiveButton("前往設定", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        }
    }

    private void ensureNotificationServiceRunning() {
        // 檢查通知監聽服務是否正在運行
        if (!isNotificationServiceRunning()) {
            Log.w(TAG, "通知監聽服務未運行，嘗試啟動");

            // 確保靜態引用正確
            setupStaticReferences();

            // 啟動服務
            Intent serviceIntent = new Intent(this, NotificationReceiver.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Log.i(TAG, "已嘗試啟動服務，靜態引用已設置");
        } else {
            Log.i(TAG, "通知監聽服務正在運行");
            // 即使服務運行中，也要確保靜態引用正確
            setupStaticReferences();
        }
    }

    private boolean isNotificationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (NotificationReceiver.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void registerServiceStateReceiver() {
        serviceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.colinsong.notify.SERVICE_STATE_CHANGED".equals(intent.getAction())) {
                    boolean isRunning = intent.getBooleanExtra("isRunning", false);
                    if (!isRunning) {
                        ensureNotificationServiceRunning();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.colinsong.notify.SERVICE_STATE_CHANGED");
        registerReceiver(serviceStateReceiver, filter);
    }

    private void scheduleServiceCheck() {
        // 創建定期檢查服務的鬧鐘
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ServiceRestartReceiver.class);
        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // 每5分鐘檢查一次（縮短檢查間隔）
        long interval = 5 * 60 * 1000; // 5分鐘
        long triggerTime = System.currentTimeMillis() + interval;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.i(TAG, "已設定服務檢查鬧鐘，每5分鐘檢查一次");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            // 检查通知权限是否已开启
            if (isNotificationPermissionEnabled()) {
                // 通知权限已开启，可以执行相应操作
                Log.i(TAG, "通知權限已啟用");
                // 重新啟動服務
                ensureNotificationServiceRunning();
            } else {
                // 通知权限未开启，可以给出提示或处理逻辑
                Log.w(TAG, "通知權限未啟用");
                Toast.makeText(this, "通知權限未開啟。", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Log.i(TAG, "已獲得電池優化排除權限");
            } else {
                Log.w(TAG, "未獲得電池優化排除權限");
                Toast.makeText(this, "未獲得電池優化排除權限，應用可能無法持續接收通知。", Toast.LENGTH_LONG).show();
            }
        }
    }

    // 在 MainActivity 中添加
    public void filterNotificationsByDate(String date) {
        Log.d(TAG, "過濾日期: " + date);

        // 1. 讀取特定日期的通知
        readNotificationsFromDatabaseByDate(date);

        // 2. 切換回主頁視圖
        if (binding.navView != null) {
            binding.navView.setSelectedItemId(R.id.navigation_home);
        }

        // 3. 不需要在這裡調用 homeFragment.updateNotifications()
        // 因為 showMainFragment() 會自動調用它
    }

    // 按日期從資料庫讀取通知
    private void readNotificationsFromDatabaseByDate(String date) {
        try {
            notificationItemList.clear();  // 清空當前列表

            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // 使用 LIKE 查詢來匹配日期前綴
            String selection = "timestamp LIKE ?";
            String[] selectionArgs = {date + "%"};  // 例如，"2023-03-25%"

            Cursor cursor = db.query(
                    "messages",  // 表格名稱
                    new String[]{"timestamp", "packageName", "title", "content"},  // 欄位
                    selection,  // WHERE 條件
                    selectionArgs,  // WHERE 參數
                    null, null,
                    "timestamp DESC"  // 排序
            );

            int count = cursor.getCount();
            Log.i(TAG, "找到 " + count + " 條 " + date + " 的通知");

            // 讀取查詢結果
            while (cursor.moveToNext()) {
                // 提取數據，創建 NotificationItem 物件，加入列表
                // (與原本的 readNotificationsFromDatabase 方法類似)
                String timeStamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                String packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));

                // 處理應用名稱
                String appName = packageName;
                if (packageName.lastIndexOf(".") != -1) {
                    appName = packageName.substring(packageName.lastIndexOf(".") + 1);
                }

                // 格式化時間
                String formattedTime = formatTime(timeStamp);

                // 創建並添加 NotificationItem
                NotificationItem item = new NotificationItem(count--, appName, formattedTime, title, content);
                notificationItemList.add(item);
            }

            cursor.close();
            db.close();

            // 通知適配器刷新
            if (notificationAdapter != null) {
                notificationAdapter.notifyDataSetChanged();
            }

            Toast.makeText(this, date + " 的通知：" + notificationItemList.size() + " 條", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "按日期讀取通知時出錯: " + e.getMessage());
            Toast.makeText(this, "讀取通知時出錯", Toast.LENGTH_SHORT).show();
        }
    }

    // 輔助方法：格式化時間戳
    private String formatTime(String timeStamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timeStamp);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "格式化時間出錯: " + e.getMessage());
        }
        return timeStamp;
    }

    // 更新 HomeFragment - 修正版，不再使用 NavHostFragment
    private void updateHomeFragment() {
        // 直接使用持有的 homeFragment 實例進行更新
        if (homeFragment != null) {
            homeFragment.updateNotifications();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "MainActivity onResume");

        // 每次回到前台時設置靜態引用
        setupStaticReferences();

        // 確保通知監聽服務正在運行
        ensureNotificationServiceRunning();

        // 更新通知列表
        readNotificationsFromDatabase();
    }

    // 在 onCreate 方法中添加底部導航的監聽器設置
    private void setupBottomNavigation() {
        // 設置底部導航點擊監聽
        binding.navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                showMainFragment();
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                showDashboardFragment();
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                showNotificationsFragment();
                return true;
            }
            return false;
        });
    }

    // 添加切換到不同 Fragment 的方法
    // 修改 showMainFragment 方法，確保在切換後更新通知
    private void showMainFragment() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, homeFragment)
                .commit();

        // 這裡不要立即調用更新，因為 Fragment 事務是異步的
        // 延遲一點時間再更新
        homeFragment.postUpdate();
    }

    private void showDashboardFragment() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, dashboardFragment)
                .commit();
    }

    private void showNotificationsFragment() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, notificationsFragment)
                .commit();
    }

    public List<NotificationItem> getNotificationItemList() {
        return notificationItemList;
    }

    // 添加更新通知的方法
    public void updateNotifications() {
        if (homeFragment != null) {
            homeFragment.updateNotifications();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 註銷廣播接收器
        if (serviceStateReceiver != null) {
            unregisterReceiver(serviceStateReceiver);
        }

        Log.i(TAG, "MainActivity onDestroy");
    }
}