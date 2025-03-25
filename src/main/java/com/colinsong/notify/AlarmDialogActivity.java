package com.colinsong.notify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class AlarmDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用透明主題，只顯示對話框而不是整個活動
        setTheme(android.R.style.Theme_Translucent_NoTitleBar);

        // 創建並顯示對話框 - 注意這裡使用 android.app.AlertDialog 而非 androidx 版本
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重要通知提醒")
                .setMessage("您有重要通知，請注意查看！")
                .setCancelable(false) // 防止按返回鍵關閉
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 停止播放鈴聲
                        NotificationReceiver.stopRingtone();
                        // 關閉活動
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        // 對話框關閉時結束活動
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        // 確保鈴聲在活動結束時停止
        NotificationReceiver.stopRingtone();
        super.onDestroy();
    }
}