package com.colormine.banking.models;

public class Notification {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;

    public Notification() {}

    public Notification(String id, String title, String message, long timestamp, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }
}
