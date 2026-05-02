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
    private Button btn100, btn500, btn1000, btnContinue;
    
    private LinearLayout selectedContactInfo;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedUsername;
    private Contact selectedContact = null;
    private DatabaseReference mDatabase;

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
        
        btn100 = findViewById(R.id.btn_100);
        btn500 = findViewById(R.id.btn_500);
        btn1000 = findViewById(R.id.btn_1000);
        btnContinue = findViewById(R.id.btn_continue);
        
        selectedContactInfo = findViewById(R.id.selected_contact_info);
        tvSelectedAvatar = findViewById(R.id.tv_selected_avatar);
        tvSelectedName = findViewById(R.id.tv_selected_name);
        tvSelectedUsername = findViewById(R.id.tv_selected_username);
    }

    private void setupContactsRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // In a real app, you'd fetch other users from Firebase here
        List<Contact> contacts = new ArrayList<>();
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contacts.clear();
                SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                String currentUserEmail = pref.getString("email", "");

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null && !user.getEmail().equals(currentUserEmail)) {
                        contacts.add(new Contact(String.valueOf(user.getId()), user.getName(), user.getEmail(), user.getName().substring(0,1)));
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btn100.setOnClickListener(v -> etAmount.setText("100"));
        btn500.setOnClickListener(v -> etAmount.setText("500"));
        btn1000.setOnClickListener(v -> etAmount.setText("1000"));
        
        btnContinue.setOnClickListener(v -> {
            if (selectedContact == null) {
                Toast.makeText(this, "Please select a recipient", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                performFirebaseTransaction(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFirebaseTransaction(double amount) {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String senderEmail = pref.getString("email", "");
        String sanitizedSender = senderEmail.replace(".", ",");
        String sanitizedRecipient = selectedContact.getUsername().replace(".", ",");

        mDatabase.child("users").child(sanitizedSender).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User sender = snapshot.getValue(User.class);
                if (sender == null) return;

                if (sender.getBalance() < amount) {
                    Toast.makeText(SendMoneyActivity.this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 1. Update Sender
                double newSenderBalance = sender.getBalance() - amount;
                mDatabase.child("users").child(sanitizedSender).child("balance").setValue(newSenderBalance);
                recordTransaction(senderEmail, "EXPENSE", amount, "Sent to " + selectedContact.getName(), "Transfer");

                // 2. Update Recipient
                mDatabase.child("users").child(sanitizedRecipient).child("balance").get().addOnSuccessListener(dataSnapshot -> {
                    Double recipientBalance = dataSnapshot.getValue(Double.class);
                    if (recipientBalance != null) {
                        mDatabase.child("users").child(sanitizedRecipient).child("balance").setValue(recipientBalance + amount);
                        recordTransaction(selectedContact.getUsername(), "INCOME", amount, "Received from " + senderEmail, "Transfer");
                    }
                });

                // 3. Success
                Intent intent = new Intent(SendMoneyActivity.this, TransferSuccessActivity.class);
                intent.putExtra("amount", amount);
                intent.putExtra("recipient", selectedContact.getName());
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

        if (txnId != null) {
            mDatabase.child("transactions").child(txnId).setValue(txn);
        }
    }
}
