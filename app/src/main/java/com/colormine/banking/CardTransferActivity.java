package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.colormine.banking.models.Card;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CardTransferActivity extends BaseActivity {

    private ImageButton btnBack;
    private View btnSelectFrom, btnSelectTo;
    private TextView tvFromNumber, tvFromBalance, tvToNumber, tvToBalance;
    private ImageView ivFromType, ivToType;
    private EditText etAmount;
    private Button btnTransfer;

    private DatabaseReference mDatabase;
    private String userEmail;
    private List<Card> userCards = new ArrayList<>();
    private Card fromCard, toCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_transfer);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        initViews();
        loadUserCards();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnSelectFrom = findViewById(R.id.btn_select_from_card);
        btnSelectTo = findViewById(R.id.btn_select_to_card);
        tvFromNumber = findViewById(R.id.tv_from_card_number);
        tvFromBalance = findViewById(R.id.tv_from_card_balance);
        tvToNumber = findViewById(R.id.tv_to_card_number);
        tvToBalance = findViewById(R.id.tv_to_card_balance);
        ivFromType = findViewById(R.id.iv_from_card_type);
        ivToType = findViewById(R.id.iv_to_card_type);
        etAmount = findViewById(R.id.et_amount);
        btnTransfer = findViewById(R.id.btn_transfer);

        btnBack.setOnClickListener(v -> finish());
        btnSelectFrom.setOnClickListener(v -> showCardPicker(true));
        btnSelectTo.setOnClickListener(v -> showCardPicker(false));
        btnTransfer.setOnClickListener(v -> validateAndTransfer());
    }

    private void loadUserCards() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getCards() != null) {
                    userCards = user.getCards();
                    if (userCards.size() < 2) {
                        Toast.makeText(CardTransferActivity.this, "Only one card available. Add another card to use this feature.", Toast.LENGTH_LONG).show();
                        btnTransfer.setEnabled(false);
                        btnTransfer.setAlpha(0.5f);
                    } else {
                        updateCardUi(userCards.get(0), true);
                        updateCardUi(userCards.get(1), false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCardUi(Card card, boolean isFrom) {
        if (isFrom) {
            fromCard = card;
            tvFromNumber.setText(card.getCardNumber());
            tvFromBalance.setText(String.format(Locale.getDefault(), "Balance: $%.2f", card.getBalance()));
        } else {
            toCard = card;
            tvToNumber.setText(card.getCardNumber());
            tvToBalance.setText(String.format(Locale.getDefault(), "Balance: $%.2f", card.getBalance()));
        }
    }

    private void showCardPicker(boolean isFrom) {
        if (userCards.isEmpty()) return;

        String[] items = new String[userCards.size()];
        for (int i = 0; i < userCards.size(); i++) {
            items[i] = userCards.get(i).getCardNumber() + " ($" + userCards.get(i).getBalance() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(isFrom ? "Select Source Card" : "Select Destination Card")
                .setItems(items, (dialog, which) -> {
                    Card selected = userCards.get(which);
                    if (isFrom) {
                        if (toCard != null && toCard.getId().equals(selected.getId())) {
                            Toast.makeText(this, "Cannot select the same card", Toast.LENGTH_SHORT).show();
                        } else {
                            updateCardUi(selected, true);
                        }
                    } else {
                        if (fromCard != null && fromCard.getId().equals(selected.getId())) {
                            Toast.makeText(this, "Cannot select the same card", Toast.LENGTH_SHORT).show();
                        } else {
                            updateCardUi(selected, false);
                        }
                    }
                })
                .show();
    }

    private void validateAndTransfer() {
        if (fromCard == null || toCard == null) {
            Toast.makeText(this, "Please select both cards", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) {
            etAmount.setError("Required");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            etAmount.setError("Invalid amount");
            return;
        }

        if (fromCard.getBalance() < amount) {
            Toast.makeText(this, "Insufficient balance on source card", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Account Status (Real-time Firebase check)
        btnTransfer.setEnabled(false);
        btnTransfer.setText("Checking security...");

        String sanitizedEmailCheck = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmailCheck).child("status")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String status = snapshot.getValue(String.class);
                        boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
                        boolean isFrozen = "FROZEN".equalsIgnoreCase(status) 
                                || com.colormine.banking.utils.SettingsManager.getInstance(CardTransferActivity.this).isAccountFrozen();

                        if (isBlocked) {
                            btnTransfer.setEnabled(true);
                            btnTransfer.setText("Transfer Now");
                            new AlertDialog.Builder(CardTransferActivity.this)
                                    .setTitle("Account Blocked")
                                    .setMessage("Your account has been blocked by an administrator. Please contact support for assistance.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else if (isFrozen) {
                            btnTransfer.setEnabled(true);
                            btnTransfer.setText("Transfer Now");
                            new AlertDialog.Builder(CardTransferActivity.this)
                                    .setTitle("Account Frozen")
                                    .setMessage("Your account is currently frozen. Please unfreeze it from Security settings to make transfers.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            performTransfer(amount);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnTransfer.setEnabled(true);
                        btnTransfer.setText("Transfer Now");
                    }
                });
    }

    private void performTransfer(double amount) {
        btnTransfer.setEnabled(false);
        btnTransfer.setText("Processing...");

        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getCards() != null) {
                    List<Card> cards = user.getCards();
                    boolean updatedFrom = false, updatedTo = false;

                    for (Card c : cards) {
                        if (c.getId().equals(fromCard.getId())) {
                            c.setBalance(c.getBalance() - amount);
                            updatedFrom = true;
                        } else if (c.getId().equals(toCard.getId())) {
                            c.setBalance(c.getBalance() + amount);
                            updatedTo = true;
                        }
                    }

                    if (updatedFrom && updatedTo) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("cards", cards);
                        // Global balance remains the same because it's an internal transfer
                        // but if we want to be safe and ensure it's synced (though it should be)
                        // updates.put("balance", user.getBalance()); 
                        
                        mDatabase.child("users").child(sanitizedEmail).updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    recordTransaction(fromCard, "EXPENSE", amount, "Transfer to Card " + toCard.getCardNumber().substring(toCard.getCardNumber().length()-4), "Transfer");
                                    recordTransaction(toCard, "INCOME", amount, "Transfer from Card " + fromCard.getCardNumber().substring(fromCard.getCardNumber().length()-4), "Transfer");
                                    Toast.makeText(CardTransferActivity.this, "Transfer Successful!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnTransfer.setEnabled(true);
                                    btnTransfer.setText("Transfer Now");
                                    Toast.makeText(CardTransferActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnTransfer.setEnabled(true);
                btnTransfer.setText("Transfer Now");
            }
        });
    }

    private void recordTransaction(Card card, String type, double amount, String title, String category) {
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        String txnId = mDatabase.child("transactions").push().getKey();
        Map<String, Object> txn = new HashMap<>();
        txn.put("user_email", userEmail);
        txn.put("type", type);
        txn.put("amount", amount);
        txn.put("title", title);
        txn.put("date", date);
        txn.put("category", category);
        txn.put("card_id", card.getId());
        if (txnId != null) mDatabase.child("transactions").child(txnId).setValue(txn);
    }
}
