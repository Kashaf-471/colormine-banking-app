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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.colormine.banking.adapters.UserAdapter;
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

/**
 * Admin Dashboard Activity for managing users and viewing transactions via Firebase.
 */
public class AdminActivity extends BaseActivity implements UserAdapter.OnUserActionListener {

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

        findViewById(R.id.card_loan_requests).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AdminLoansActivity.class));
        });

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out from admin console?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
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

        // Load pending loans count
        mDatabase.child("loan_requests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot loanSnap : snapshot.getChildren()) {
                    if ("PENDING".equals(loanSnap.child("status").getValue(String.class))) {
                        count++;
                    }
                }
                TextView tvCount = findViewById(R.id.tv_pending_loans_count);
                if (tvCount != null) {
                    if (count > 0) {
                        tvCount.setText(count + " pending requests requiring action");
                        tvCount.setTextColor(getResources().getColor(R.color.colorWarning));
                    } else {
                        tvCount.setText("No pending loan requests");
                        tvCount.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
                        // Remove user data from database
                        mDatabase.child("users").child(sanitizedEmail).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // Also record the deleted email so signup can detect this
                                mDatabase.child("deleted_users").child(sanitizedEmail).setValue(true);
                                Toast.makeText(AdminActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
                            })
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
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_edit_user, null);
        
        EditText etName = view.findViewById(R.id.et_edit_name);
        Spinner spinnerStatus = view.findViewById(R.id.spinner_status);
        EditText etBalance = view.findViewById(R.id.et_card_balance);
        Spinner spinnerCards = view.findViewById(R.id.spinner_select_card);
        Button btnSave = view.findViewById(R.id.btn_save_user);
        Button btnCancel = view.findViewById(R.id.btn_cancel_edit);
        ImageButton btnClose = view.findViewById(R.id.btn_close_edit);

        if (user.getName() != null) etName.setText(user.getName());
        
        // Setup Status Spinner - only Active or Blocked
        String[] statusOptions = {"Active", "Blocked"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        if (user.getStatus() != null) {
            for (int i = 0; i < statusOptions.length; i++) {
                if (statusOptions[i].equalsIgnoreCase(user.getStatus())) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        }

        // Setup Card Spinner
        List<Card> cards = user.getCards();
        List<String> cardNames = new ArrayList<>();
        if (cards != null && !cards.isEmpty()) {
            for (Card card : cards) {
                String last4 = "";
                if (card.getCardNumber() != null && card.getCardNumber().length() >= 4) {
                    last4 = card.getCardNumber().substring(card.getCardNumber().length() - 4);
                }
                cardNames.add(card.getType() + " (**** " + last4 + ")");
            }
        } else {
            cardNames.add("No cards available");
        }
        
        ArrayAdapter<String> cardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cardNames);
        cardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCards.setAdapter(cardAdapter);

        // Update balance field when a card is selected
        spinnerCards.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                if (cards != null && position < cards.size()) {
                    etBalance.setText(String.valueOf(cards.get(position).getBalance()));
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newStatus = spinnerStatus.getSelectedItem().toString();
            int selectedCardPos = spinnerCards.getSelectedItemPosition();
            
            double newCardBalance;
            try {
                newCardBalance = Double.parseDouble(etBalance.getText().toString().trim());
            } catch (NumberFormatException e) {
                newCardBalance = 0;
            }

            if (user.getEmail() != null) {
                String sanitizedEmail = user.getEmail().replace(".", ",");
                
                // Update local card balance and sync total user balance
                if (cards != null && selectedCardPos >= 0 && selectedCardPos < cards.size()) {
                    cards.get(selectedCardPos).setBalance(newCardBalance);
                    user.syncGlobalBalance();
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                // Save status as uppercase to match app conventions (ACTIVE/BLOCKED)
                updates.put("status", newStatus.toUpperCase());
                updates.put("balance", user.getBalance());
                updates.put("cards", cards);

                mDatabase.child("users").child(sanitizedEmail).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminActivity.this, "User updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
