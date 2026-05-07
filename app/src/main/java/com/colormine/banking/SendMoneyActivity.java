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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.ContactAdapter;
import com.colormine.banking.models.Card;
import com.colormine.banking.models.Contact;
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

public class SendMoneyActivity extends BaseActivity {

    private ImageButton btnBack;
    private com.google.android.material.textfield.TextInputLayout tilAmount;
    private EditText etSearch, etAmount;
    private RecyclerView rvContacts;
    private Button btnContinue;
    private LinearLayout selectedContactInfo, btnSelectCard;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedUsername;
    private TextView tvSelectedCardNumber, tvSelectedCardExpiry, tvSelectedCardBalance;
    private ImageView ivSelectedCardType;
    
    private Contact selectedContact = null;
    private Card selectedCard = null;
    private List<Card> userCards = new ArrayList<>();
    
    private DatabaseReference mDatabase;
    private androidx.activity.result.ActivityResultLauncher<Intent> otpLauncher;
    private double pendingAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews();
        setupContactsRecyclerView();
        setupListeners();
        loadUserCards();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rvContacts = findViewById(R.id.rv_contacts);
        tilAmount = findViewById(R.id.til_amount);
        etAmount = findViewById(R.id.et_amount);
        btnContinue = findViewById(R.id.btn_continue);
        selectedContactInfo = findViewById(R.id.selected_contact_info);
        tvSelectedAvatar = findViewById(R.id.tv_selected_avatar);
        tvSelectedName = findViewById(R.id.tv_selected_name);
        tvSelectedUsername = findViewById(R.id.tv_selected_username);
        
        btnSelectCard = findViewById(R.id.btn_select_card);
        tvSelectedCardNumber = findViewById(R.id.tv_selected_card_number);
        tvSelectedCardExpiry = findViewById(R.id.tv_selected_card_expiry);
        tvSelectedCardBalance = findViewById(R.id.tv_selected_card_balance);
        ivSelectedCardType = findViewById(R.id.iv_selected_card_type);
    }

    private void loadUserCards() {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String currentUserEmail = pref.getString("email", "");
        String sanitizedEmail = currentUserEmail.replace(".", ",");

        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getCards() != null) {
                    userCards = user.getCards();
                    if (!userCards.isEmpty() && selectedCard == null) {
                        updateSelectedCard(userCards.get(0));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSelectedCard(Card card) {
        selectedCard = card;
        tvSelectedCardNumber.setText(card.getCardNumber());
        tvSelectedCardExpiry.setText("Exp: " + card.getExpiryDate());
        tvSelectedCardBalance.setText(String.format("Balance: $%,.2f", card.getBalance()));
        // You can add logic to set card type icon here
    }

    private void showCardSelectionDialog() {
        if (userCards.isEmpty()) {
            Toast.makeText(this, "No cards available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] cardDisplayNames = new String[userCards.size()];
        for (int i = 0; i < userCards.size(); i++) {
            cardDisplayNames[i] = userCards.get(i).getCardNumber() + " (" + userCards.get(i).getCardHolderName() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Payment Method")
                .setItems(cardDisplayNames, (dialog, which) -> {
                    updateSelectedCard(userCards.get(which));
                })
                .show();
    }

    private void setupContactsRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        List<Contact> contacts = new ArrayList<>();
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contacts.clear();
                SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                String currentUserEmail = pref.getString("email", "");

                String requestRecipientEmail = getIntent().getStringExtra("request_recipient_email");
                double requestAmount = getIntent().getDoubleExtra("request_amount", -1);

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null && user.getEmail() != null && !user.getEmail().equals(currentUserEmail)) {
                        Contact contact = new Contact(String.valueOf(user.getId()), user.getName(), user.getEmail(), user.getName().substring(0,1));
                        contacts.add(contact);

                        if (requestRecipientEmail != null && requestRecipientEmail.equals(user.getEmail())) {
                            selectedContact = contact;
                        }
                    }
                }

                ContactAdapter adapter = new ContactAdapter(contacts, contact -> {
                    selectedContact = contact;
                    selectedContactInfo.setVisibility(View.VISIBLE);
                    tvSelectedAvatar.setText(contact.getAvatarInitial());
                    tvSelectedName.setText(contact.getName());
                    String username = contact.getUsername();
                    if (username != null && username.contains("@")) {
                        username = "@" + username.split("@")[0];
                    }
                    tvSelectedUsername.setText(username);
                });
                rvContacts.setAdapter(adapter);

                if (selectedContact != null) {
                    selectedContactInfo.setVisibility(View.VISIBLE);
                    tvSelectedAvatar.setText(selectedContact.getAvatarInitial());
                    tvSelectedName.setText(selectedContact.getName());
                    String username = selectedContact.getUsername();
                    if (username != null && username.contains("@")) {
                        username = "@" + username.split("@")[0];
                    }
                    tvSelectedUsername.setText(username);
                }

                if (requestAmount > 0) {
                    etAmount.setText(String.format(Locale.getDefault(), "%.2f", requestAmount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSelectCard.setOnClickListener(v -> showCardSelectionDialog());

        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        findViewById(R.id.btn_50).setOnClickListener(v -> etAmount.setText("50"));
        findViewById(R.id.btn_100).setOnClickListener(v -> etAmount.setText("100"));
        findViewById(R.id.btn_200).setOnClickListener(v -> etAmount.setText("200"));
        findViewById(R.id.btn_500).setOnClickListener(v -> etAmount.setText("500"));

        otpLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    performFirebaseTransaction(pendingAmount);
                } else {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                    Toast.makeText(this, "Verification cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        );

        btnContinue.setOnClickListener(v -> {
            playClickFeedback();

            if (selectedContact == null) {
                Toast.makeText(this, "Select a recipient", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedCard == null) {
                Toast.makeText(this, "Select a payment card", Toast.LENGTH_SHORT).show();
                return;
            }

            String amountStr = etAmount.getText().toString().trim();
            tilAmount.setError(null);

            if (amountStr.isEmpty()) {
                tilAmount.setError("Amount is required");
                return;
            }

            try {
                pendingAmount = Double.parseDouble(amountStr);
                if (pendingAmount <= 0) {
                    tilAmount.setError("Amount must be greater than zero");
                    return;
                }
                if (pendingAmount > 1000000) {
                    tilAmount.setError("Amount exceeds maximum transfer limit ($1,000,000)");
                    return;
                }
                if (pendingAmount > selectedCard.getBalance()) {
                    tilAmount.setError("Insufficient balance in selected card");
                    return;
                }
                checkAccountStatusAndProceed();
            } catch (NumberFormatException e) {
                tilAmount.setError("Invalid amount format");
            }
        });
    }

    private void checkAccountStatusAndProceed() {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String currentUserEmail = pref.getString("email", "");
        String sanitizedEmail = currentUserEmail.replace(".", ",");

        btnContinue.setEnabled(false);
        btnContinue.setText("Checking security...");

        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                boolean isBlocked = user != null && "BLOCKED".equalsIgnoreCase(user.getStatus());
                boolean isFrozen = (user != null && "FROZEN".equalsIgnoreCase(user.getStatus())) 
                        || com.colormine.banking.utils.SettingsManager.getInstance(SendMoneyActivity.this).isAccountFrozen();
                        
                if (isBlocked) {
                    resetButton();
                    new AlertDialog.Builder(SendMoneyActivity.this)
                        .setTitle("Account Blocked")
                        .setMessage("Your account has been blocked by an administrator. Please contact support for assistance.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }

                if (isFrozen) {
                    resetButton();
                    new AlertDialog.Builder(SendMoneyActivity.this)
                        .setTitle("Account Frozen")
                        .setMessage("Your account is currently frozen. Please unfreeze it from Security settings to make transfers.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }

                // Verify PIN of the SELECTED card
                if (selectedCard != null && selectedCard.getPin() != null && !selectedCard.getPin().isEmpty()) {
                    showCardPinDialog(selectedCard.getPin());
                } else {
                    // Fallback to global PIN if card PIN is not set (for old cards)
                    String globalPin = settingsManager.getTransactionPin();
                    if (!globalPin.isEmpty()) {
                        showCardPinDialog(globalPin);
                    } else {
                        // Only start OTP flow if 2FA is enabled in user settings
                        if (user != null && user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().get("twoFactor"))) {
                            startOtpFlow(currentUserEmail);
                        } else {
                            // 2FA is off and no PIN is set, proceed directly
                            performFirebaseTransaction(pendingAmount);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetButton();
            }
        });
    }

    private void showCardPinDialog(String correctPin) {
        resetButton();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 0);

        EditText etPin = new EditText(this);
        etPin.setHint("Enter 4-digit Card PIN");
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setMaxLines(1);
        layout.addView(etPin);

        new AlertDialog.Builder(this)
            .setTitle("Card Verification")
            .setMessage("Please enter the PIN for card ending in " + selectedCard.getCardNumber().substring(selectedCard.getCardNumber().length() - 4))
            .setView(layout)
            .setPositiveButton("Verify", (dialog, which) -> {
                String inputPin = etPin.getText().toString().trim();
                if (correctPin.equals(inputPin)) {
                    SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                    startOtpFlow(pref.getString("email", ""));
                } else {
                    Toast.makeText(this, "Incorrect Card PIN", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void startOtpFlow(String email) {
        btnContinue.setEnabled(false);
        btnContinue.setText("Sending Email OTP...");

        OtpService.generateAndSend(this, email, new OtpService.OtpCallback() {
            @Override
            public void onSuccess() {
                resetButton();
                Intent intent = new Intent(SendMoneyActivity.this, VerifyOtpActivity.class);
                intent.putExtra("email", email);
                intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_SEND_MONEY);
                otpLauncher.launch(intent);
            }

            @Override
            public void onFallback(String error) {
                resetButton();
                Toast.makeText(SendMoneyActivity.this, "Failed to send OTP to your email: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performFirebaseTransaction(double amount) {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String senderEmail = pref.getString("email", "");
        String sanitizedSender = senderEmail.replace(".", ",");
        
        if (selectedContact == null) return;
        String sanitizedRecipient = selectedContact.getUsername().replace(".", ",");

        btnContinue.setEnabled(false);
        btnContinue.setText("Processing...");

        mDatabase.child("users").child(sanitizedSender).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User sender = snapshot.getValue(User.class);
                if (sender == null || selectedCard == null) {
                    resetButton();
                    return;
                }

                // Check if the SPECIFIC SELECTED CARD has enough balance
                if (selectedCard.getBalance() < amount) {
                    Toast.makeText(SendMoneyActivity.this, "Insufficient balance on selected card!", Toast.LENGTH_SHORT).show();
                    resetButton();
                    return;
                }

                // 1. Update Sender's specific card balance and global balance
                List<Card> updatedCards = sender.getCards();
                for (Card c : updatedCards) {
                    if (c.getId().equals(selectedCard.getId())) {
                        c.setBalance(c.getBalance() - amount);
                        break;
                    }
                }
                
                double newGlobalBalance = sender.getBalance() - amount;
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("balance", newGlobalBalance);
                updates.put("cards", updatedCards);

                mDatabase.child("users").child(sanitizedSender).updateChildren(updates);
                recordTransaction(senderEmail, "EXPENSE", amount, "Sent to " + selectedContact.getName() + " via Card " + selectedCard.getCardNumber().substring(selectedCard.getCardNumber().length()-4), "Transfer", selectedCard.getId());
                recordNotification(senderEmail, "Transfer Sent", "You sent $" + amount + " to " + selectedContact.getName());
                
                // Real Push Notification
                com.colormine.banking.utils.NotificationHelper.showNotification(
                    SendMoneyActivity.this, 
                    "Transfer Successful", 
                    "You sent $" + amount + " to " + selectedContact.getName()
                );

                // 2. Update Recipient Balance
                mDatabase.child("users").child(sanitizedRecipient).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        User recipient = snap.getValue(User.class);
                        if (recipient != null) {
                            double newRecipBalance = recipient.getBalance() + amount;
                            List<Card> recipCards = recipient.getCards();
                            String targetCardId = "default";
                            
                            if (recipCards != null && !recipCards.isEmpty()) {
                                String primaryId = recipient.getPrimaryCardId();
                                boolean updated = false;
                                for (Card rc : recipCards) {
                                    if (primaryId != null && rc.getId().equals(primaryId)) {
                                        rc.setBalance(rc.getBalance() + amount);
                                        targetCardId = rc.getId();
                                        updated = true;
                                        break;
                                    }
                                }
                                if (!updated) {
                                    recipCards.get(0).setBalance(recipCards.get(0).getBalance() + amount);
                                    targetCardId = recipCards.get(0).getId();
                                }
                            }
                            
                            Map<String, Object> recipUpdates = new HashMap<>();
                            recipUpdates.put("balance", newRecipBalance);
                            recipUpdates.put("cards", recipCards);
                            mDatabase.child("users").child(sanitizedRecipient).updateChildren(recipUpdates);

                            recordTransaction(selectedContact.getUsername(), "INCOME", amount, "Received from " + sender.getName(), "Transfer", targetCardId);
                            recordNotification(selectedContact.getUsername(), "Payment Received", "You received $" + amount + " from " + sender.getName());
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                // 3. Mark the original notification as paid if it exists
                String notificationId = getIntent().getStringExtra("notification_id");
                if (notificationId != null) {
                    mDatabase.child("notifications").child(sanitizedSender).child(notificationId).child("status").setValue("paid");
                }

                // 4. Success
                Intent intent = new Intent(SendMoneyActivity.this, TransferSuccessActivity.class);
                intent.putExtra("amount", amount);
                intent.putExtra("recipient", selectedContact.getName());
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnContinue.setEnabled(true);
        btnContinue.setText("Continue");
    }

    private void recordTransaction(String email, String type, double amount, String title, String category, String cardId) {
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        String txnId = mDatabase.child("transactions").push().getKey();
        Map<String, Object> txn = new HashMap<>();
        txn.put("user_email", email);
        txn.put("type", type);
        txn.put("amount", amount);
        txn.put("title", title);
        txn.put("date", date);
        txn.put("category", category);
        txn.put("card_id", cardId);
        if (txnId != null) mDatabase.child("transactions").child(txnId).setValue(txn);
    }

    private void recordNotification(String email, String title, String message) {
        if (!settingsManager.isNotificationsEnabled()) return;
        
        String sanitizedEmail = email.replace(".", ",");
        String id = mDatabase.child("notifications").child(sanitizedEmail).push().getKey();
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("timestamp", System.currentTimeMillis());
        if (id != null) mDatabase.child("notifications").child(sanitizedEmail).child(id).setValue(notif);
    }
}
