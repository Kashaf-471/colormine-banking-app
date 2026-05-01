package com.colormine.banking.models;

public class Transaction {
    private String id;
    private String name;
    private String date;
    private String category;
    private double amount;
    private boolean isIncome;

    public Transaction(String id, String name, String date, String category, double amount, boolean isIncome) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.category = category;
        this.amount = amount;
        this.isIncome = isIncome;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
}
