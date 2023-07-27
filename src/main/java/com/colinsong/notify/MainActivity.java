package com.colinsong.notify;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.colinsong.notify.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import android.content.ComponentName;
import android.content.IntentFilter;
public class MainActivity extends AppCompatActivity {
    private List<String> notificationList = new ArrayList<>();
    private NotificationAdapter notificationAdapter;
    private NotificationReceiver notificationReceiver;
    private ActivityMainBinding binding;




    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化 RecyclerView 和适配器
        RecyclerView recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();  // 创建通知列表

        notificationAdapter = new NotificationAdapter(notificationList);  // 创建适配器

        recyclerView.setAdapter(notificationAdapter);  // 关联适配器

        // 创建 NotificationReceiver 实例并传递 notificationList 和 notificationAdapter
        notificationReceiver = new NotificationReceiver(notificationList, notificationAdapter);


        Button sendNotificationButton = binding.sendNotificationButton;
        sendNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个模拟的通知
                String notificationTitle = "测试通知";
                String notificationContent = "这是一条测试通知内容";

                // 发送广播以触发 NotificationReceiver 的 onReceive() 方法
                Intent intent = new Intent("com.colinsong.NOTIFICATION_RECEIVED");
                intent.putExtra(NotificationCompat.EXTRA_TITLE, notificationTitle);
                intent.putExtra(NotificationCompat.EXTRA_TEXT, notificationContent);
                sendBroadcast(intent);
            }
        });

        // 检查通知权限并请求
        requestNotificationPermission();

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