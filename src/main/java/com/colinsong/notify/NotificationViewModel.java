package com.colinsong.notify;

import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class NotificationViewModel extends ViewModel {

    private List<String> notificationList = new ArrayList<>();

    public List<String> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(List<String> notificationList) {
        this.notificationList = notificationList;
    }


}