package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanRequestActivity extends BaseActivity {

    private ImageButton btnBack;
    private EditText etAmount;
    private Spinner spinnerCard;
    private Button btnRequest;
    private DatabaseReference mDatabase;
    private String currentUserEmail;
    private User currentUser;
    private List<Card> userCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_request);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("email", "");

        initViews();
        loadUserCards();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etAmount = findViewById(R.id.et_loan_amount);
        spinnerCard = findViewById(R.id.spinner_loan_card);
        btnRequest = findViewById(R.id.btn_request_loan);
    }

    private void loadUserCards() {
        if (currentUserEmail.isEmpty()) return;
        
        String sanitizedEmail = currentUserEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null && currentUser.getCards() != null) {
                    userCards = currentUser.getCards();
                    List<String> cardDisplayNames = new ArrayList<>();
                    for (Card card : userCards) {
                        String last4 = card.getCardNumber().length() > 4 
                            ? card.getCardNumber().substring(card.getCardNumber().length() - 4) 
                            : card.getCardNumber();
                        cardDisplayNames.add(card.getType() + " (**** " + last4 + ")");
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(LoanRequestActivity.this, 
                        android.R.layout.simple_spinner_item, cardDisplayNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCard.setAdapter(adapter);
                } else {
                    Toast.makeText(LoanRequestActivity.this, "No cards found. Please add a card first.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnRequest.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }
            
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    etAmount.setError("Enter a positive amount");
                    return;
                }
                
                checkAccountStatusAndProceed(amount);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
            }
        });
    }

    private void checkAccountStatusAndProceed(double amount) {
        if (currentUser == null) return;

        btnRequest.setEnabled(false);
        btnRequest.setText("Checking security...");

        String status = currentUser.getStatus();
        boolean isBlocked = "BLOCKED".equalsIgnoreCase(status);
        boolean isFrozen = "FROZEN".equalsIgnoreCase(status) 
                || com.colormine.banking.utils.SettingsManager.getInstance(this).isAccountFrozen();
                
        if (isBlocked) {
            showErrorDialog("Account Blocked", "Your account has been blocked by an administrator.");
        } else if (isFrozen) {
            showErrorDialog("Account Frozen", "Your account is currently frozen. Please unfreeze it from Security settings.");
        } else {
            submitLoanRequest(amount);
        }
    }

    private void showErrorDialog(String title, String message) {
        btnRequest.setEnabled(true);
        btnRequest.setText(R.string.btn_request_loan);
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void submitLoanRequest(double amount) {
        btnRequest.setText("Submitting...");
        
        int selectedCardIndex = spinnerCard.getSelectedItemPosition();
        if (selectedCardIndex < 0 || userCards == null || selectedCardIndex >= userCards.size()) {
            Toast.makeText(this, "Please select a card", Toast.LENGTH_SHORT).show();
            btnRequest.setEnabled(true);
            return;
        }
        
        Card selectedCard = userCards.get(selectedCardIndex);
        String loanRequestId = mDatabase.child("loan_requests").push().getKey();
        
        Map<String, Object> loanRequest = new HashMap<>();
        loanRequest.put("id", loanRequestId);
        loanRequest.put("userId", currentUserEmail.replace(".", ","));
        loanRequest.put("userName", currentUser.getName());
        loanRequest.put("amount", amount);
        loanRequest.put("cardId", selectedCard.getId());
        loanRequest.put("cardDetails", selectedCard.getType() + " (" + selectedCard.getCardNumber() + ")");
        loanRequest.put("status", "PENDING");
        loanRequest.put("timestamp", System.currentTimeMillis());
        loanRequest.put("interestRate", 0.1); // 10% hardcoded

        if (loanRequestId != null) {
            mDatabase.child("loan_requests").child(loanRequestId).setValue(loanRequest)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoanRequestActivity.this, R.string.toast_loan_requested, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoanRequestActivity.this, RequestSuccessActivity.class);
                    intent.putExtra("amount", amount);
                    intent.putExtra("recipient", "Bank Admin");
                    intent.putExtra("isLoan", true); // Handle success UI slightly differently if needed
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoanRequestActivity.this, "Failed to submit request", Toast.LENGTH_SHORT).show();
                    btnRequest.setEnabled(true);
                    btnRequest.setText(R.string.btn_request_loan);
                });
        }
    }
}
