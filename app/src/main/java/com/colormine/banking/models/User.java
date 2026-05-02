package com.colormine.banking.models;

import com.google.firebase.database.PropertyName;
import java.util.HashMap;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private double balance;
    private String status; 
    private int isAdmin;
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private long registrationDate;
    private Map<String, Boolean> settings;

    public User() {
        this.settings = new HashMap<>();
    }

    public User(int id, String name, String email, String password, double balance, String status, int isAdmin, String cardNumber, String cardExpiry, String cardCvv) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.balance = balance;
        this.status = status;
        this.isAdmin = isAdmin;
        this.cardNumber = cardNumber;
        this.cardExpiry = cardExpiry;
        this.cardCvv = cardCvv;
        this.registrationDate = System.currentTimeMillis();
        this.settings = new HashMap<>();
        // Default settings
        this.settings.put("biometric", false);
        this.settings.put("twoFactor", false);
        this.settings.put("notifications", true);
        this.settings.put("darkMode", false);
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }
    public String getStatus() { return status; }

    @PropertyName("isAdmin")
    public int getIsAdmin() { return isAdmin; }

    public String getCardNumber() { return cardNumber; }
    public String getCardExpiry() { return cardExpiry; }
    public String getCardCvv() { return cardCvv; }
    public long getRegistrationDate() { return registrationDate; }
    public Map<String, Boolean> getSettings() { return settings; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setStatus(String status) { this.status = status; }

    @PropertyName("isAdmin")
    public void setIsAdmin(int isAdmin) { this.isAdmin = isAdmin; }

    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
    public void setSettings(Map<String, Boolean> settings) { this.settings = settings; }
}
