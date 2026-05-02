package com.colormine.banking.models;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private double balance;
    private String status; // "ACTIVE" or "BLOCKED"

    // Required for Firebase
    public User() {}

    public User(int id, String name, String email, String password, double balance, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.balance = balance;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }
    public String getStatus() { return status; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setStatus(String status) { this.status = status; }
}
