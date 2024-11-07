package com.colinsong.notify;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {
    private static MyDatabaseHelper instance;
    private static final String DATABASE_NAME = "notifications.db";
    private static final int DATABASE_VERSION = 1;
    // 私有的構造函數，防止外部實例化

    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // 取得可寫入資料庫的實例
        SQLiteDatabase db = this.getWritableDatabase();

    }

    // 單例模式，獲取 MyDatabaseHelper 的唯一實例
    public static synchronized MyDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MyDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create database table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp TEXT," +
                "packageName TEXT," +
                "title TEXT," +
                "content TEXT)";

        db.execSQL(createTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database version upgrades if needed
        // This method will be called when you increment DATABASE_VERSION
        // For simplicity, we'll just drop the existing table and create a new one
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);
    }
}
