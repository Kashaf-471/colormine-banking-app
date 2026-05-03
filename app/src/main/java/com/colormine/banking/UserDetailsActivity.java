package com.colormine.banking;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
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

public class UserDetailsActivity extends AppCompatActivity {

    private TextView tvInitials, tvFullName, tvEmail, tvBalance, tvStatusText, tvEmpty;
    private View statusIndicator;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private DatabaseReference mDatabase;
    private String userEmail;
    
    private EditText etNotifTitle, etNotifMessage;
    private Button btnSendNotif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews();

        userEmail = getIntent().getStringExtra("user_email");
        String userName = getIntent().getStringExtra("user_name");
        double userBalance = getIntent().getDoubleExtra("user_balance", 0.0);
        String userStatus = getIntent().getStringExtra("user_status");

        displayUserDetails(userName, userEmail, userBalance, userStatus);
        
        if (userEmail != null) {
            loadUserTransactionsFromFirebase(userEmail);
            observeUserAccount(userEmail);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        btnSendNotif.setOnClickListener(v -> sendNotificationToUser());
    }

    private void initViews() {
        tvInitials = findViewById(R.id.tv_initials);
        tvFullName = findViewById(R.id.tv_full_name);
        tvEmail = findViewById(R.id.tv_email);
        tvBalance = findViewById(R.id.tv_balance);
        tvStatusText = findViewById(R.id.tv_status_text);
        tvEmpty = findViewById(R.id.tv_empty_transactions);
        statusIndicator = findViewById(R.id.status_indicator);
        rvTransactions = findViewById(R.id.rv_user_transactions);
        
        etNotifTitle = findViewById(R.id.et_notif_title);
        etNotifMessage = findViewById(R.id.et_notif_message);
        btnSendNotif = findViewById(R.id.btn_send_notif);

        transactionList = new ArrayList<>();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void sendNotificationToUser() {
        String title = etNotifTitle.getText().toString().trim();
        String message = etNotifMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please enter both title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userEmail == null) return;

        String sanitizedEmail = userEmail.replace(".", ",");
        String id = mDatabase.child("notifications").child(sanitizedEmail).push().getKey();
        
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("timestamp", System.currentTimeMillis());

        if (id != null) {
            btnSendNotif.setEnabled(false);
            btnSendNotif.setText("Sending...");
            
            mDatabase.child("notifications").child(sanitizedEmail).child(id).setValue(notif)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserDetailsActivity.this, "Notification sent to " + userEmail, Toast.LENGTH_SHORT).show();
                    etNotifTitle.setText("");
                    etNotifMessage.setText("");
                    btnSendNotif.setEnabled(true);
                    btnSendNotif.setText("Send Notification");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserDetailsActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSendNotif.setEnabled(true);
                    btnSendNotif.setText("Send Notification");
                });
        }
    }

    private void observeUserAccount(String email) {
        String sanitizedEmail = email.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    displayUserDetails(user.getName(), user.getEmail(), user.getBalance(), user.getStatus());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayUserDetails(String name, String email, double balance, String status) {
        tvFullName.setText(name);
        tvEmail.setText(email);
        tvBalance.setText(String.format("$%,.2f", balance));
        tvStatusText.setText(status != null ? status : "ACTIVE");

        if (name != null && !name.isEmpty()) {
            tvInitials.setText(name.substring(0, 1).toUpperCase());
        }

        if ("BLOCKED".equals(status)) {
            statusIndicator.setBackgroundResource(R.drawable.bg_dot_red);
            tvStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusIndicator.setBackgroundResource(R.drawable.bg_dot_green);
            tvStatusText.setTextColor(getResources().getColor(R.color.colorSuccess));
        }
    }

    private void loadUserTransactionsFromFirebase(String email) {
        mDatabase.child("transactions").orderByChild("user_email").equalTo(email)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    transactionList.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String id = postSnapshot.getKey();
                        String type = postSnapshot.child("type").getValue(String.class);
                        Double amount = postSnapshot.child("amount").getValue(Double.class);
                        String title = postSnapshot.child("title").getValue(String.class);
                        String date = postSnapshot.child("date").getValue(String.class);
                        String category = postSnapshot.child("category").getValue(String.class);

                        if (amount != null) {
                            transactionList.add(0, new Transaction(id, title, date, category, amount, "INCOME".equals(type)));
                        }
                    }

                    if (transactionList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvTransactions.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvTransactions.setVisibility(View.VISIBLE);
                        if (adapter == null) {
                            adapter = new TransactionAdapter(transactionList);
                            rvTransactions.setAdapter(adapter);
                        } else {
                            adapter.updateData(transactionList);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UserDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
