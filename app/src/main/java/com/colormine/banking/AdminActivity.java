package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.colormine.banking.adapters.UserAdapter;
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

/**
 * Admin Dashboard Activity for managing users and viewing transactions via Firebase.
 */
public class AdminActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    private ListView listView;
    private UserAdapter adapter;
    private List<User> allUsersList;
    private List<User> filteredUsersList;
    private TextView tvTotalBalance, tvAdminName, tvAdminEmail;
    private EditText etSearch;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews();
        
        allUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();

        loadAdminProfile();
        loadUsersFromFirebase();
        setupListeners();
    }

    private void initViews() {
        listView = findViewById(R.id.list_users);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminEmail = findViewById(R.id.tv_admin_email);
        etSearch = findViewById(R.id.et_search_user);
    }

    private void setupListeners() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        findViewById(R.id.card_all_transactions).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AdminTransactionsActivity.class));
        });

        findViewById(R.id.card_broadcast).setOnClickListener(v -> showBroadcastDialog());

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out from admin console?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
                        startActivity(new Intent(AdminActivity.this, LoginSignupActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showBroadcastDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_broadcast, null);
        EditText etTitle = view.findViewById(R.id.et_broadcast_title);
        EditText etMessage = view.findViewById(R.id.et_broadcast_message);

        new AlertDialog.Builder(this)
            .setTitle("Broadcast Notification")
            .setMessage("Send this message to ALL users")
            .setView(view)
            .setPositiveButton("Send All", (dialog, which) -> {
                String title = etTitle.getText().toString().trim();
                String message = etMessage.getText().toString().trim();
                if (!title.isEmpty() && !message.isEmpty()) {
                    sendBroadcast(title, message);
                } else {
                    Toast.makeText(this, "Title and Message required", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void sendBroadcast(String title, String message) {
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String sanitizedEmail = userSnap.getKey();
                    if (sanitizedEmail != null) {
                        String id = mDatabase.child("notifications").child(sanitizedEmail).push().getKey();
                        Map<String, Object> notif = new HashMap<>();
                        notif.put("title", title);
                        notif.put("message", message);
                        notif.put("timestamp", System.currentTimeMillis());
                        if (id != null) {
                            mDatabase.child("notifications").child(sanitizedEmail).child(id).setValue(notif);
                            count++;
                        }
                    }
                }
                Toast.makeText(AdminActivity.this, "Broadcast sent to " + count + " users", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAdminProfile() {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String email = pref.getString("email", "");
        if (email.isEmpty()) return;

        String sanitizedEmail = email.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User admin = snapshot.getValue(User.class);
                if (admin != null) {
                    if (tvAdminName != null) tvAdminName.setText(admin.getName());
                    if (tvAdminEmail != null) tvAdminEmail.setText(admin.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUsersFromFirebase() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUsersList.clear();
                double totalBalance = 0;
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null) {
                        if (user.getIsAdmin() != 1) {
                            allUsersList.add(user);
                            totalBalance += user.getBalance();
                        }
                    }
                }
                
                if (tvTotalBalance != null) {
                    tvTotalBalance.setText(String.format("$%,.2f", totalBalance));
                }
                
                filterUsers(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsersList.clear();
        if (query == null || query.isEmpty()) {
            filteredUsersList.addAll(allUsersList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : allUsersList) {
                String name = user.getName() != null ? user.getName().toLowerCase() : "";
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                
                if (name.contains(lowerCaseQuery) || email.contains(lowerCaseQuery)) {
                    filteredUsersList.add(user);
                }
            }
        }
        
        adapter = new UserAdapter(this, filteredUsersList, this);
        listView.setAdapter(adapter);
    }

    @Override
    public void onEdit(User user) {
        showEditDialog(user);
    }

    private void showCardManagementDialog(User user) {
        if (user.getCards() == null || user.getCards().isEmpty()) {
            Toast.makeText(this, "User has no cards", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_view_cards, null);
        TextView tvSummary = view.findViewById(R.id.tv_user_summary);
        RecyclerView rvCards = view.findViewById(R.id.rv_admin_cards);
        Button btnClose = view.findViewById(R.id.btn_close_view);

        tvSummary.setText("Showing all active cards for " + user.getName());
        rvCards.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        com.colormine.banking.adapters.CardAdapter cardAdapter = new com.colormine.banking.adapters.CardAdapter(
            user.getCards(), 
            user.getPrimaryCardId(), 
            null
        );
        rvCards.setAdapter(cardAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (user.getEmail() != null) {
                        String sanitizedEmail = user.getEmail().replace(".", ",");
                        mDatabase.child("users").child(sanitizedEmail).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "User deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onView(User user) {
        showCardManagementDialog(user);
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_edit_user, null);
        
        EditText etName = view.findViewById(R.id.et_edit_name);
        Spinner spinnerStatus = view.findViewById(R.id.spinner_status);
        Spinner spinnerCards = view.findViewById(R.id.spinner_select_card);
        EditText etCardBalance = view.findViewById(R.id.et_card_balance);

        // 1. Setup Status Spinner
        String[] statuses = {"ACTIVE", "BLOCKED"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        if (user.getStatus() != null) {
            spinnerStatus.setSelection(user.getStatus().equals("BLOCKED") ? 1 : 0);
        }

        // 2. Setup Cards Spinner
        List<com.colormine.banking.models.Card> userCards = user.getCards();
        List<String> cardNames = new ArrayList<>();
        for (com.colormine.banking.models.Card c : userCards) {
            cardNames.add(c.getCardNumber() + " (" + c.getCardHolderName() + ")");
        }
        ArrayAdapter<String> cardsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cardNames);
        cardsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCards.setAdapter(cardsAdapter);

        spinnerCards.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                etCardBalance.setText(String.valueOf(userCards.get(position).getBalance()));
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        if (etName != null) etName.setText(user.getName());

        builder.setView(view);
        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String newStatus = spinnerStatus.getSelectedItem().toString();
            String cardBalanceStr = etCardBalance.getText().toString().trim();

            if (!name.isEmpty() && !cardBalanceStr.isEmpty()) {
                try {
                    double newCardBalance = Double.parseDouble(cardBalanceStr);
                    int selectedCardIndex = spinnerCards.getSelectedItemPosition();
                    
                    user.setName(name);
                    user.setStatus(newStatus);
                    userCards.get(selectedCardIndex).setBalance(newCardBalance);
                    user.syncGlobalBalance(); // Keep total synced

                    String sanitizedEmail = user.getEmail().replace(".", ",");
                    mDatabase.child("users").child(sanitizedEmail).setValue(user)
                        .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "User updated successfully", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid balance format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Name and balance are required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }
}
