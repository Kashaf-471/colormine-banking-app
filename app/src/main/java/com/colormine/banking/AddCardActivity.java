package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.Card;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class AddCardActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputLayout tilName, tilNumber, tilExpiry, tilCvv, tilPin;
    private EditText etName, etNumber, etExpiry, etCvv, etPin;
    private TextView tvPreviewName, tvPreviewNumber, tvPreviewExpiry;
    private Button btnSave;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        if (userEmail.isEmpty()) {
            finish();
            return;
        }

        tilName = findViewById(R.id.til_cardholder_name);
        tilNumber = findViewById(R.id.til_card_number);
        tilExpiry = findViewById(R.id.til_expiry_date);
        tilCvv = findViewById(R.id.til_cvv);
        tilPin = findViewById(R.id.til_card_pin);

        etName = findViewById(R.id.et_cardholder_name);
        etNumber = findViewById(R.id.et_card_number);
        etExpiry = findViewById(R.id.et_expiry_date);
        etCvv = findViewById(R.id.et_cvv);
        etPin = findViewById(R.id.et_card_pin);
        
        tvPreviewName = findViewById(R.id.tv_preview_name);
        tvPreviewNumber = findViewById(R.id.tv_preview_number);
        tvPreviewExpiry = findViewById(R.id.tv_preview_expiry);
        
        btnSave = findViewById(R.id.btn_save_card);

        setupTextWatchers();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveCard());
    }

    private void setupTextWatchers() {
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvPreviewName.setText(s.length() > 0 ? s.toString().toUpperCase() : "FULL NAME");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return;
                isFormatting = true;

                String raw = s.toString().replace(" ", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < raw.length(); i++) {
                    if (i > 0 && i % 4 == 0) formatted.append(" ");
                    formatted.append(raw.charAt(i));
                }
                
                etNumber.setText(formatted.toString());
                etNumber.setSelection(formatted.length());
                tvPreviewNumber.setText(formatted.length() > 0 ? formatted.toString() : "XXXX XXXX XXXX XXXX");
                isFormatting = false;
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etExpiry.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", "");
                    String sel = current;
                    int selCount = clean.length();
                    for (int i = 2; i <= selCount && i < 4; i += 2) {
                        sel += "/" + clean.substring(i - 2, i);
                    }
                    
                    if (clean.length() >= 2) {
                        String mm = clean.substring(0, 2);
                        String yy = "";
                        if (clean.length() > 2) {
                            yy = clean.substring(2);
                        }
                        current = mm + "/" + yy;
                    } else {
                        current = clean;
                    }

                    tvPreviewExpiry.setText(current.length() > 0 ? current : "MM/YY");
                    etExpiry.removeTextChangedListener(this);
                    etExpiry.setText(current);
                    etExpiry.setSelection(current.length());
                    etExpiry.addTextChangedListener(this);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void saveCard() {
        String name = etName.getText().toString().trim();
        String numberStr = etNumber.getText().toString().replace(" ", "").trim();
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCvv.getText().toString().trim();
        String pin = etPin.getText().toString().trim();

        tilName.setError(null);
        tilNumber.setError(null);
        tilExpiry.setError(null);
        tilCvv.setError(null);
        tilPin.setError(null);

        if (name.isEmpty()) {
            tilName.setError("Cardholder name is required");
            return;
        }
        if (name.length() < 3) {
            tilName.setError("Name must be at least 3 characters");
            return;
        }

        if (numberStr.isEmpty()) {
            tilNumber.setError("Card number is required");
            return;
        }
        if (numberStr.length() != 16) {
            tilNumber.setError("Card number must be 16 digits");
            return;
        }
        if (!numberStr.matches("\\d+")) {
            tilNumber.setError("Card number must contain only digits");
            return;
        }

        if (expiry.isEmpty()) {
            tilExpiry.setError("Expiry date is required");
            return;
        }
        if (!isValidExpiry(expiry)) {
            tilExpiry.setError("Invalid expiry date (MM/YY)");
            return;
        }

        if (cvv.isEmpty()) {
            tilCvv.setError("CVV is required");
            return;
        }
        if (cvv.length() < 3) {
            tilCvv.setError("CVV must be 3 digits");
            return;
        }
        if (!cvv.matches("\\d+")) {
            tilCvv.setError("CVV must contain only digits");
            return;
        }

        if (pin.isEmpty()) {
            tilPin.setError("PIN is required");
            return;
        }
        if (pin.length() != 4) {
            tilPin.setError("PIN must be 4 digits");
            return;
        }
        if (!pin.matches("\\d+")) {
            tilPin.setError("PIN must contain only digits");
            return;
        }

        String sanitizedEmail = userEmail.replace(".", ",");

        // Pre-format the card number (needed inside lambda)
        StringBuilder formattedNumBuilder = new StringBuilder();
        for (int i = 0; i < numberStr.length(); i++) {
            if (i > 0 && i % 4 == 0) formattedNumBuilder.append(" ");
            formattedNumBuilder.append(numberStr.charAt(i));
        }
        String formattedNum = formattedNumBuilder.toString();

        // First check if account is blocked by admin
        mDatabase.child("users").child(sanitizedEmail).child("status")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot statusSnapshot) {
                        String status = statusSnapshot.getValue(String.class);
                        if ("BLOCKED".equalsIgnoreCase(status)) {
                            new androidx.appcompat.app.AlertDialog.Builder(AddCardActivity.this)
                                    .setTitle("Account Blocked")
                                    .setMessage("Your account has been blocked by an administrator. Please contact support for assistance.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }
                        // Account is not blocked — proceed to save the card
                        doSaveCard(sanitizedEmail, name, formattedNum, expiry, cvv, pin);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // On error, allow the operation to proceed
                        doSaveCard(sanitizedEmail, name, formattedNum, expiry, cvv, pin);
                    }
                });
    }

    private void doSaveCard(String sanitizedEmail, String name, String formattedNum, String expiry, String cvv, String pin) {
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    List<Card> cards = user.getCards();
                    if (cards == null) cards = new ArrayList<>();

                    String cardId = UUID.randomUUID().toString();
                    Card newCard = new Card(cardId, name, formattedNum, expiry, cvv, "Visa");
                    newCard.setPin(pin);

                    cards.add(newCard);

                    // If it's the first card, make it primary
                    if (user.getPrimaryCardId() == null || user.getPrimaryCardId().isEmpty()) {
                        user.setPrimaryCardId(cardId);
                    }

                    mDatabase.child("users").child(sanitizedEmail).child("cards").setValue(cards);
                    mDatabase.child("users").child(sanitizedEmail).child("primaryCardId").setValue(user.getPrimaryCardId())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AddCardActivity.this, "Card added successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean isValidExpiry(String expiry) {
        if (!expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) return false;

        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt("20" + parts[1]);

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        if (year < currentYear) return false;
        if (year == currentYear && month < currentMonth) return false;

        return true;
    }
}
