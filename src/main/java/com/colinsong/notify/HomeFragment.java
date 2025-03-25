package com.colinsong.notify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.colinsong.notify.databinding.FragmentHomeBinding;
import com.colinsong.notify.ui.home.HomeViewModel;

import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化 RecyclerView
        recyclerView = root.findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 從 MainActivity 獲取通知列表
        List<NotificationItem> notificationItemList = ((MainActivity) requireActivity()).getNotificationItemList();

        // 初始化適配器
        notificationAdapter = new NotificationAdapter(notificationItemList);
        recyclerView.setAdapter(notificationAdapter);
        Log.d(TAG, "RecyclerView 已設置適配器，項目數: " + notificationItemList.size());

        // 如果您仍然需要 TextView
        try {
            final TextView textView = binding.textHome;
            homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        } catch (Exception e) {
            Log.e(TAG, "找不到 textHome 控件: " + e.getMessage());
        }

        return root;
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
        if (notificationAdapter != null) {
            Log.d(TAG, "更新通知列表");
            notificationAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "無法更新通知列表，適配器為空");
        }
    }
}