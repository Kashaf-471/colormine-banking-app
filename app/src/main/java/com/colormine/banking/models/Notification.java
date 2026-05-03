package com.colormine.banking.models;

public class Notification {
    private String id;
    private String title;
    private String message;
    private long timestamp;
    private boolean read;
    private String type;
    private String senderEmail;
    private double amount;

    public Notification() {}

    public Notification(String id, String title, String message, long timestamp, boolean read, String type, String senderEmail, double amount) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
        this.senderEmail = senderEmail;
        this.amount = amount;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public String getType() { return type; }
    public String getSenderEmail() { return senderEmail; }
    public double getAmount() { return amount; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }
    public void setType(String type) { this.type = type; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public void setAmount(double amount) { this.amount = amount; }
}
