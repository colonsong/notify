package com.colinsong.notify;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.colinsong.notify.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    public static List<String> notificationList = new ArrayList<>();
    private NotificationAdapter notificationAdapter;
    private NotificationReceiver notificationReceiver;
    private ActivityMainBinding binding;
    private MyDatabaseHelper dbHelper;

    // 創建ViewModel成員變量
    private NotificationViewModel notificationViewModel;
    private MutableLiveData<List<String>> notificationLiveData = new MutableLiveData<>();

    private static final int NOTIFICATION_ID_PERMISSION_REMINDER = 1;
    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 200;
    private static final int REQUEST_NOTIFICATION_ACCESS = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 MyDatabaseHelper，使用單例模式
        dbHelper = MyDatabaseHelper.getInstance(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化 RecyclerView 和适配器
        RecyclerView recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 創建或獲取現有的ViewModel
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        notificationList = notificationViewModel.getNotificationList();

        notificationAdapter = new NotificationAdapter(notificationList);  // 创建适配器
        recyclerView.setAdapter(notificationAdapter);  // 关联适配器

        // 创建 NotificationReceiver 实例并传递 notificationList 和 notificationAdapter
        notificationReceiver = new NotificationReceiver(notificationList, notificationAdapter);

        Button sendNotificationButton = binding.sendNotificationButton;
        sendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 模擬接收通知
                String notificationTitle = "測試通知";
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String notificationContent = "這是一條測試通知內容，時間戳：" + timeStamp;

                String notificationInfo = "Title: " + notificationTitle + "\nContent: " + notificationContent;
                NotificationReceiver.addNotification(notificationInfo);

                // 這裡不需要再手動更新列表，LiveData會自動通知UI更新
            }
        });

        // 检查是否已经授予通知监听权限
        if (!isNotificationListenerEnabled()) {
            // 如果没有授予权限，则请求用户授权
            requestNotificationListenerPermission();
        } else {
            // 如果已经授予权限，则启动NotificationReceiver服务
            startNotificationReceiverService();
        }

        // 观察 LiveData 数据并更新通知列表
        notificationLiveData.observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> notifications) {
                notificationList.clear();
                notificationList.addAll(notifications);
                notificationAdapter.notifyDataSetChanged();
            }
        });

        // 讀取資料庫中的通知資訊並加入到 notificationList 中
        readNotificationsFromDatabase();
    }

    // 请求用户授权通知监听权限
    private void requestNotificationListenerPermission() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivityForResult(intent, REQUEST_NOTIFICATION_ACCESS);

    }

    private void readNotificationsFromDatabase() {
        List<String> notifications = new ArrayList<>();

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

        // 將查詢結果加入到 notifications 中
        while (cursor.moveToNext()) {
            String timeStamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
            String packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
            // 取得 packageName 最後一個逗點後面的英文部分
            String appName = packageName.substring(packageName.lastIndexOf(".") + 1);

            String notificationTitle = count-- + " " +appName + "\n" + timeStamp + "\n" + title;
            String notificationContent = content;
            String notificationInfo = notificationTitle + "\n " + notificationContent;
            notifications.add(notificationInfo);
        }

        // 關閉 Cursor 和資料庫連接
        cursor.close();
        db.close();

        // 反转通知列表
       // Collections.reverse(notifications);

        // 使用 LiveData 更新通知列表数据
        notificationLiveData.setValue(notifications);
    }





    private boolean isNotificationPermissionEnabled() {
        // Android 8.0及以上版本
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            return notificationManager.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_NOTIFICATION_ACCESS) {
            if (isNotificationListenerEnabled()) {
                startNotificationReceiverService();
            } else {
                Toast.makeText(this, "未授予通知监听权限，无法启动服务", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 检查通知监听权限是否已经授予
    private boolean isNotificationListenerEnabled() {
        ComponentName cn = new ComponentName(this, NotificationReceiver.class);
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(cn.flattenToString());
    }

    // 启动NotificationReceiver服务
    private void startNotificationReceiverService() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        //startService(intent);

        startForegroundService(intent);
    }



    @Override
    protected void onResume() {
        super.onResume();

    }



}