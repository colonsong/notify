package com.colinsong.notify;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private RecyclerView dateRecyclerView;
    private DashboardAdapter dateAdapter;
    private List<DateItem> dateItems = new ArrayList<>();
    private MyDatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        dateRecyclerView = view.findViewById(R.id.dateRecyclerView);
        dateRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = MyDatabaseHelper.getInstance(getContext());

        // 初始化空的適配器
        dateItems = new ArrayList<>();
        dateAdapter = new DashboardAdapter(dateItems);
        dateRecyclerView.setAdapter(dateAdapter);

        // 載入數據（在適配器設置後調用）
        loadDateData();

        // 通知適配器數據已更新
        dateAdapter.notifyDataSetChanged();

        return view;
    }
    private void loadDateData() {
        try {
            // 從數據庫讀取不同的日期和每天的通知數量
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery(
                    "SELECT substr(timestamp, 1, 10) as date, COUNT(*) as count " +
                            "FROM messages " +
                            "GROUP BY substr(timestamp, 1, 10) " +
                            "ORDER BY date DESC", null);

            int totalCount = cursor.getCount();
            android.util.Log.d("DashboardFragment", "找到 " + totalCount + " 個日期分組");

            dateItems.clear();

            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));

                android.util.Log.d("DashboardFragment", "日期: " + date + ", 通知數量: " + count);
                dateItems.add(new DateItem(date, count));
            }

            cursor.close();
            db.close();

            // 檢查數據是否為空
            if (dateItems.isEmpty()) {
                android.util.Log.w("DashboardFragment", "沒有找到任何日期分組數據");
            }
        } catch (Exception e) {
            android.util.Log.e("DashboardFragment", "加載日期數據時出錯: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 日期適配器
    private class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DateViewHolder> {
        private List<DateItem> dateItems;

        public DashboardAdapter(List<DateItem> dateItems) {
            this.dateItems = dateItems;
        }

        @NonNull
        @Override
        public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dashboard_date, parent, false);
            return new DateViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
            DateItem item = dateItems.get(position);
            holder.dateTextView.setText(formatDate(item.getDate()));
            holder.countTextView.setText(item.getCount() + " 條通知");

            // 設置點擊事件
            holder.itemView.setOnClickListener(v -> {
                // 將日期傳給 MainActivity 進行過濾，並返回主頁
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();

                    // 調用過濾方法
                    activity.filterNotificationsByDate(item.getDate());

                    // 不需要在這裡調用其他方法，MainActivity 會處理好導航和更新
                }
            });
        }

        @Override
        public int getItemCount() {
            return dateItems.size();
        }

        private String formatDate(String dateStr) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM月dd日 E", Locale.CHINESE);
                Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateStr;
            }
        }

        class DateViewHolder extends RecyclerView.ViewHolder {
            TextView dateTextView;
            TextView countTextView;

            public DateViewHolder(@NonNull View itemView) {
                super(itemView);
                dateTextView = itemView.findViewById(R.id.dateTextView);
                countTextView = itemView.findViewById(R.id.countTextView);
            }
        }
    }

    // 日期項目簡單類
    private static class DateItem {
        private String date;
        private int count;

        public DateItem(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public int getCount() {
            return count;
        }
    }
}