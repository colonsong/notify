package com.colinsong.notify;

/**
 * 通知項目類，用於保存通知的詳細信息
 */
public class NotificationItem {
    private int id;
    private String appName;
    private String timestamp;
    private String title;
    private String content;

    /**
     * 默認構造函數
     */
    public NotificationItem() {
    }

    /**
     * 完整構造函數
     *
     * @param id        通知的ID或序號
     * @param appName   應用名稱
     * @param timestamp 時間戳
     * @param title     通知標題
     * @param content   通知內容
     */
    public NotificationItem(int id, String appName, String timestamp, String title, String content) {
        this.id = id;
        this.appName = appName;
        this.timestamp = timestamp;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "NotificationItem{" +
                "id=" + id +
                ", appName='" + appName + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}