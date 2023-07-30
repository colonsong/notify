package com.colinsong.notify;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
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



    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 200;

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

        // 检查通知权限并请求
        requestNotificationPermission();

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

        // 將查詢結果加入到 notifications 中
        while (cursor.moveToNext()) {
            String timeStamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
            String packageName = cursor.getString(cursor.getColumnIndexOrThrow("packageName"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
            // 取得 packageName 最後一個逗點後面的英文部分
            String appName = packageName.substring(packageName.lastIndexOf(".") + 1);

            String notificationTitle = appName + "\n" + timeStamp + "\n" + title;
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

    private void requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                ComponentName cn = new ComponentName(this, NotificationReceiver.class);
                if (!notificationManager.isNotificationListenerAccessGranted(cn)) {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            // 检查通知权限是否已开启
            if (isNotificationPermissionEnabled()) {
                // 通知权限已开启，可以执行相应操作
            } else {
                // 通知权限未开启，可以给出提示或处理逻辑
            }
        }
    }

}