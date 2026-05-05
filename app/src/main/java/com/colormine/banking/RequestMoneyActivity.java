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
import androidx.appcompat.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestMoneyActivity extends BaseActivity {

    private ImageButton btnBack;
    private RecyclerView rvContacts;
    private EditText etAmount;
    private Button btnContinue;
    private LinearLayout selectedContactInfo;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedUsername;
    private Contact selectedContact = null;
    private DatabaseReference mDatabase;
    private String currentUserEmail;
    private String currentUserName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_money);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        currentUserEmail = pref.getString("email", "");

        // Fetch current user name for the notification
        if (!currentUserEmail.isEmpty()) {
            mDatabase.child("users").child(currentUserEmail.replace(".", ",")).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User u = snapshot.getValue(User.class);
                    if (u != null) currentUserName = u.getName();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

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
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null && user.getEmail() != null && !user.getEmail().equals(currentUserEmail)) {
                        contacts.add(new Contact(String.valueOf(user.getId()), user.getName(), user.getEmail(), user.getName().substring(0,1)));
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });
        
        findViewById(R.id.btn_50).setOnClickListener(v -> etAmount.setText("50"));
        findViewById(R.id.btn_100).setOnClickListener(v -> etAmount.setText("100"));
        findViewById(R.id.btn_200).setOnClickListener(v -> etAmount.setText("200"));
        findViewById(R.id.btn_500).setOnClickListener(v -> etAmount.setText("500"));
        
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
                double amount = Double.parseDouble(amountStr);
                sendMoneyRequest(amount);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
            }
        });
    }

    private void sendMoneyRequest(double amount) {
        if (currentUserEmail.isEmpty() || currentUserName.isEmpty()) {
            Toast.makeText(this, "Session error, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false);
        btnContinue.setText("Checking security...");

        // Check if account is frozen
        mDatabase.child("users").child(currentUserEmail.replace(".", ",")).child("status")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.getValue(String.class);
                    boolean isFrozen = "FROZEN".equalsIgnoreCase(status) 
                            || com.colormine.banking.utils.SettingsManager.getInstance(RequestMoneyActivity.this).isAccountFrozen();
                            
                    if (isFrozen) {
                        btnContinue.setEnabled(true);
                        btnContinue.setText("Send Request");
                        new AlertDialog.Builder(RequestMoneyActivity.this)
                            .setTitle("Account Frozen")
                            .setMessage("Your account is currently frozen. Please unfreeze it from Security settings to make requests.")
                            .setPositiveButton("OK", null)
                            .show();
                    } else {
                        proceedWithRequest(amount);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Send Request");
                }
            });
    }

    private void proceedWithRequest(double amount) {
        btnContinue.setText("Sending Request...");

        String sanitizedRecipient = selectedContact.getUsername().replace(".", ",");

        // Record notification for recipient if enabled in settings
        if (!settingsManager.isNotificationsEnabled()) {
            proceedToSuccess(amount);
            return;
        }

        String title = "Money Request";
        String message = currentUserName + " has requested $" + String.format("%.2f", amount) + " from you.";

        String id = mDatabase.child("notifications").child(sanitizedRecipient).push().getKey();
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("type", "request");
        notif.put("senderEmail", currentUserEmail);
        notif.put("amount", amount);

        if (id != null) {
            mDatabase.child("notifications").child(sanitizedRecipient).child(id).setValue(notif)
                    .addOnSuccessListener(aVoid -> {
                        // Real Push Notification for sender (acknowledgment)
                        com.colormine.banking.utils.NotificationHelper.showNotification(
                                RequestMoneyActivity.this,
                                "Request Sent",
                                "Your request for $" + String.format("%.2f", amount) + " has been sent to " + selectedContact.getName()
                        );

                        Intent intent = new Intent(RequestMoneyActivity.this, RequestSuccessActivity.class);
                        intent.putExtra("amount", amount);
                        intent.putExtra("recipient", selectedContact.getName());
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(RequestMoneyActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                        btnContinue.setEnabled(true);
                        btnContinue.setText("Send Request");
                    });
        }
    }
    private void proceedToSuccess(double amount) {
        Intent intent = new Intent(RequestMoneyActivity.this, RequestSuccessActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra("recipient", selectedContact.getName());
        startActivity(intent);
        finish();
    }
}
