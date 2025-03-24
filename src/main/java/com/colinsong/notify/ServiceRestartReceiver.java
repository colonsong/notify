package com.colinsong.notify;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ServiceRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "ServiceRestart";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "收到服務檢查廣播");

        // 檢查服務是否運行，如果沒有則重啟
        if (!isServiceRunning(context, NotificationReceiver.class)) {
            Log.w(TAG, "通知監聽服務未運行，正在重啟服務");

            // 啟動服務
            Intent serviceIntent = new Intent(context, NotificationReceiver.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 廣播服務狀態變更通知
            Intent stateIntent = new Intent("com.colinsong.notify.SERVICE_STATE_CHANGED");
            stateIntent.putExtra("isRunning", true);
            context.sendBroadcast(stateIntent);
        } else {
            Log.i(TAG, "通知監聽服務正在運行");
        }
    }

    private boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}


