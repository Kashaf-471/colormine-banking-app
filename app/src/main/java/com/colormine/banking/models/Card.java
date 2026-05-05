package com.colormine.banking.models;

import java.io.Serializable;

public class Card implements Serializable {
    private String id;
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;
    private String type; // Visa, Mastercard, etc.
    private String pin;  // Transaction PIN for this specific card
    private double balance; // Balance specific to this card

    public Card() {
    }

    public Card(String id, String cardHolderName, String cardNumber, String expiryDate, String cvv, String type) {
        this.id = id;
        this.cardHolderName = cardHolderName;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.type = type;
        this.pin = "";
        this.balance = 0.0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
