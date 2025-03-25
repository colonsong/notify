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

import com.colinsong.notify.MainActivity;
import com.colinsong.notify.MyDatabaseHelper;
import com.colinsong.notify.R;

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

        // 加載日期數據
        loadDateData();

        // 設置適配器
        dateAdapter = new DashboardAdapter(dateItems);
        dateRecyclerView.setAdapter(dateAdapter);

        return view;
    }

    private void loadDateData() {
        // 從數據庫讀取不同的日期和每天的通知數量
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT substr(timestamp, 1, 10) as date, COUNT(*) as count " +
                        "FROM messages " +
                        "GROUP BY substr(timestamp, 1, 10) " +
                        "ORDER BY date DESC", null);

        dateItems.clear();

        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));

            dateItems.add(new DateItem(date, count));
        }

        cursor.close();
        db.close();
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
                    ((MainActivity) getActivity()).filterNotificationsByDate(item.getDate());
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