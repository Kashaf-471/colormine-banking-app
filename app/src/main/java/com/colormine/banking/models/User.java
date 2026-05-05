package com.colormine.banking.models;

import com.google.firebase.database.PropertyName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private double balance;
    private String status; 
    private int isAdmin;
    private String phoneNumber;
    private String address;
    private long registrationDate;
    private Map<String, Boolean> settings;
    private List<Card> cards;
    private String primaryCardId;
    
    // Legacy fields for backward compatibility
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;

    public User() {
        this.settings = new HashMap<>();
        this.cards = new ArrayList<>();
    }

    public User(int id, String name, String email, String password, double balance, String status, int isAdmin, String cardNumber, String cardExpiry, String cardCvv) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.balance = balance;
        this.status = status;
        this.isAdmin = isAdmin;
        this.registrationDate = System.currentTimeMillis();
        this.settings = new HashMap<>();
        // Default settings
        this.settings.put("biometric", false);
        this.settings.put("twoFactor", false);
        this.settings.put("notifications", true);
        this.settings.put("darkMode", false);
        this.phoneNumber = "";
        this.address = "";
        
        this.cards = new ArrayList<>();
        // Add the initial card
        Card defaultCard = new Card("default", name, cardNumber, cardExpiry, cardCvv, "Visa");
        defaultCard.setBalance(balance);
        this.cards.add(defaultCard);
        this.primaryCardId = "default";
        
        // Keep legacy fields updated
        this.cardNumber = cardNumber;
        this.cardExpiry = cardExpiry;
        this.cardCvv = cardCvv;
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

    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public long getRegistrationDate() { return registrationDate; }
    public Map<String, Boolean> getSettings() { return settings; }
    
    public List<Card> getCards() { 
        if (cards == null) cards = new ArrayList<>();
        
        // Migration: If cards list is empty but legacy fields exist, add the legacy card
        if (cards.isEmpty() && cardNumber != null && !cardNumber.isEmpty()) {
            Card legacyCard = new Card("default", name, cardNumber, cardExpiry, cardCvv, "Visa");
            legacyCard.setBalance(balance);
            cards.add(legacyCard);
            if (primaryCardId == null) primaryCardId = "default";
        }
        
        return cards; 
    }
    
    public String getPrimaryCardId() { return primaryCardId; }

    // Compatibility getters
    public String getCardNumber() { return cardNumber; }
    public String getCardExpiry() { return cardExpiry; }
    public String getCardCvv() { return cardCvv; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBalance(double balance) { this.balance = balance; }
    public void setStatus(String status) { this.status = status; }

    @PropertyName("isAdmin")
    public void setIsAdmin(int isAdmin) { this.isAdmin = isAdmin; }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
    public void setSettings(Map<String, Boolean> settings) { this.settings = settings; }
    public void setCards(List<Card> cards) { this.cards = cards; }
    public void setPrimaryCardId(String primaryCardId) { this.primaryCardId = primaryCardId; }
    
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
}
