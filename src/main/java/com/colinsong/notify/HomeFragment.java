package com.colinsong.notify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.colinsong.notify.databinding.FragmentHomeBinding;
import com.colinsong.notify.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            HomeViewModel homeViewModel =
                    new ViewModelProvider(this).get(HomeViewModel.class);

            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            Log.d(TAG, "HomeFragment onCreateView");

            // 初始化 RecyclerView (添加空值檢查)
            recyclerView = root.findViewById(R.id.notificationRecyclerView);
            if (recyclerView == null) {
                Log.e(TAG, "無法找到 notificationRecyclerView - 布局中可能缺少此 ID");
                // 創建應急 RecyclerView 防止崩潰
                recyclerView = new RecyclerView(requireContext());
            }

            // 確保 LayoutManager 被設置
            if (recyclerView.getLayoutManager() == null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            // 從 MainActivity 獲取通知列表 (添加空值檢查)
            MainActivity activity = (MainActivity) requireActivity();
            List<NotificationItem> notificationItemList = activity.getNotificationItemList();
            if (notificationItemList == null) {
                Log.e(TAG, "無法從 MainActivity 獲取通知列表");
                notificationItemList = new ArrayList<>(); // 創建空列表防止 NullPointerException
            }

            // 初始化適配器
            notificationAdapter = new NotificationAdapter(notificationItemList);
            recyclerView.setAdapter(notificationAdapter);
            Log.d(TAG, "RecyclerView 已設置適配器，項目數: " + notificationItemList.size());

            // 如果您仍然需要 TextView
            try {
                final TextView textView = binding.textHome;
                if (textView != null) {
                    homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
                }
            } catch (Exception e) {
                Log.e(TAG, "找不到或設置 textHome 控件時出錯: " + e.getMessage());
            }

            return root;
        } catch (Exception e) {
            Log.e(TAG, "HomeFragment onCreateView 發生錯誤: " + e.getMessage());
            e.printStackTrace();
            // 創建一個簡單的視圖以避免返回 null
            TextView errorView = new TextView(requireContext());
            errorView.setText("載入頁面時發生錯誤");
            return errorView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次 Fragment 恢復可見狀態時更新列表
        Log.d(TAG, "HomeFragment onResume");
        updateNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 更新通知列表
     */
    public void updateNotifications() {
        Log.d(TAG, "HomeFragment.updateNotifications() 被調用");

        // 獲取最新的通知列表
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && recyclerView != null && isAdded()) {
            // 注意：這裡我們不需要再調用 activity.readNotificationsFromDatabase()
            // 因為該方法在 filterNotificationsByDate() 或 onResume() 中已經被調用

            // 獲取通知列表並刷新 UI
            List<NotificationItem> notifications = activity.getNotificationItemList();
            Log.d(TAG, "更新通知列表, 數量: " + notifications.size());

            // 通知適配器數據更新
            if (notificationAdapter != null) {
                notificationAdapter.notifyDataSetChanged();
            }
        } else {
            Log.w(TAG, "無法更新通知: " + (activity == null ? "activity為空" : "recyclerView為空或Fragment未添加"));
        }
    }

    /**
     * 延遲更新通知，確保 Fragment 事務完成
     */
    public void postUpdate() {
        if (getView() != null) {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "HomeFragment.postUpdate() 執行更新");
                    updateNotifications();

                    // 確保 RecyclerView 捲動到頂部
                    if (recyclerView != null && recyclerView.getAdapter() != null &&
                            recyclerView.getAdapter().getItemCount() > 0) {
                        recyclerView.scrollToPosition(0);
                    }
                }
            });
        } else {
            Log.w(TAG, "HomeFragment.postUpdate(): getView() 返回 null");
        }
    }
}