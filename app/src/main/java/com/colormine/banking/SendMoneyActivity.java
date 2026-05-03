package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.ContactAdapter;
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

public class SendMoneyActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvContacts;
    private EditText etAmount;
    private Button btnContinue;
    private LinearLayout selectedContactInfo;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedUsername;
    private Contact selectedContact = null;
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
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rvContacts = findViewById(R.id.rv_contacts);
        etAmount = findViewById(R.id.et_amount);
        btnContinue = findViewById(R.id.btn_continue);
        selectedContactInfo = findViewById(R.id.selected_contact_info);
        tvSelectedAvatar = findViewById(R.id.tv_selected_avatar);
        tvSelectedName = findViewById(R.id.tv_selected_name);
        tvSelectedUsername = findViewById(R.id.tv_selected_username);
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
                    tvSelectedUsername.setText(contact.getUsername());
                });
                rvContacts.setAdapter(adapter);

                if (selectedContact != null) {
                    selectedContactInfo.setVisibility(View.VISIBLE);
                    tvSelectedAvatar.setText(selectedContact.getAvatarInitial());
                    tvSelectedName.setText(selectedContact.getName());
                    tvSelectedUsername.setText(selectedContact.getUsername());
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
        
        findViewById(R.id.btn_100).setOnClickListener(v -> etAmount.setText("100"));
        findViewById(R.id.btn_500).setOnClickListener(v -> etAmount.setText("500"));
        findViewById(R.id.btn_1000).setOnClickListener(v -> etAmount.setText("1000"));
        
        otpLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    performFirebaseTransaction(pendingAmount);
                } else {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Continue");
                    Toast.makeText(this, "Transaction cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        );

        btnContinue.setOnClickListener(v -> {
            if (selectedContact == null) {
                Toast.makeText(this, "Select a recipient", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }
            
            try {
                pendingAmount = Double.parseDouble(amountStr);
                
                SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                String currentUserEmail = pref.getString("email", "");
                
                if (currentUserEmail.isEmpty()) {
                    Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnContinue.setEnabled(false);
                btnContinue.setText("Sending OTP...");

                OtpService.generateAndSend(this, currentUserEmail, new OtpService.OtpCallback() {
                    @Override
                    public void onSuccess() {
                        btnContinue.setText("Verify to Send");
                        Intent intent = new Intent(SendMoneyActivity.this, VerifyOtpActivity.class);
                        intent.putExtra("email", currentUserEmail);
                        intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_SEND_MONEY);
                        otpLauncher.launch(intent);
                    }

                    @Override
                    public void onFallback(String fallbackOtp, String error) {
                        btnContinue.setText("Verify to Send");
                        com.google.android.material.snackbar.Snackbar.make(btnContinue, "📧 SMTP missing. Test OTP: " + fallbackOtp, com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE)
                            .setAction("Next", v2 -> {
                                Intent intent = new Intent(SendMoneyActivity.this, VerifyOtpActivity.class);
                                intent.putExtra("email", currentUserEmail);
                                intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_SEND_MONEY);
                                otpLauncher.launch(intent);
                            }).show();
                    }
                });

            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
            }
        });
    }

    private void performFirebaseTransaction(double amount) {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String senderEmail = pref.getString("email", "");
        String sanitizedSender = senderEmail.replace(".", ",");
        String sanitizedRecipient = selectedContact.getUsername().replace(".", ",");

        btnContinue.setEnabled(false);
        btnContinue.setText("Processing...");

        mDatabase.child("users").child(sanitizedSender).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User sender = snapshot.getValue(User.class);
                if (sender == null || sender.getBalance() < amount) {
                    Toast.makeText(SendMoneyActivity.this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
                    resetButton();
                    return;
                }

                // 1. Update Sender Balance
                mDatabase.child("users").child(sanitizedSender).child("balance").setValue(sender.getBalance() - amount);
                recordTransaction(senderEmail, "EXPENSE", amount, "Sent to " + selectedContact.getName(), "Transfer");
                recordNotification(senderEmail, "Transfer Sent", "You sent $" + amount + " to " + selectedContact.getName());

                // 2. Update Recipient Balance
                mDatabase.child("users").child(sanitizedRecipient).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        User recipient = snap.getValue(User.class);
                        if (recipient != null) {
                            mDatabase.child("users").child(sanitizedRecipient).child("balance").setValue(recipient.getBalance() + amount);
                            recordTransaction(selectedContact.getUsername(), "INCOME", amount, "Received from " + sender.getName(), "Transfer");
                            recordNotification(selectedContact.getUsername(), "Payment Received", "You received $" + amount + " from " + sender.getName());
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                // 3. Success
                startActivity(new Intent(SendMoneyActivity.this, TransferSuccessActivity.class)
                    .putExtra("amount", amount).putExtra("recipient", selectedContact.getName()));
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

    private void recordTransaction(String email, String type, double amount, String title, String category) {
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
        String txnId = mDatabase.child("transactions").push().getKey();
        Map<String, Object> txn = new HashMap<>();
        txn.put("user_email", email);
        txn.put("type", type);
        txn.put("amount", amount);
        txn.put("title", title);
        txn.put("date", date);
        txn.put("category", category);
        if (txnId != null) mDatabase.child("transactions").child(txnId).setValue(txn);
    }

    private void recordNotification(String email, String title, String message) {
        String sanitizedEmail = email.replace(".", ",");
        String id = mDatabase.child("notifications").child(sanitizedEmail).push().getKey();
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("timestamp", System.currentTimeMillis());
        if (id != null) mDatabase.child("notifications").child(sanitizedEmail).child(id).setValue(notif);
    }
}
