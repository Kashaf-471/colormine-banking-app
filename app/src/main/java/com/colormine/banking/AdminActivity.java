package com.colormine.banking;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.adapters.UserAdapter;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin Dashboard Activity for managing users and viewing transactions via Firebase.
 */
public class AdminActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    private ListView listView;
    private UserAdapter adapter;
    private List<User> allUsersList;
    private List<User> filteredUsersList;
    private TextView tvTotalBalance;
    private EditText etSearch;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        listView = findViewById(R.id.list_users);
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        etSearch = findViewById(R.id.et_search_user);
        
        allUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();

        loadUsersFromFirebase();

        // Search implementation
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

        // Navigation to System Transactions
        View cardTransactions = findViewById(R.id.card_all_transactions);
        if (cardTransactions != null) {
            cardTransactions.setOnClickListener(v -> {
                startActivity(new Intent(AdminActivity.this, AdminTransactionsActivity.class));
            });
        }

        // Logout implementation
        View btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
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
                        // Skip system admin in the management list
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
        if (query.isEmpty()) {
            filteredUsersList.addAll(allUsersList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : allUsersList) {
                if (user.getName().toLowerCase().contains(lowerCaseQuery) || 
                    user.getEmail().toLowerCase().contains(lowerCaseQuery)) {
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

    @Override
    public void onDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    String sanitizedEmail = user.getEmail().replace(".", ",");
                    mDatabase.child("users").child(sanitizedEmail).removeValue()
                        .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "User deleted from Firebase", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onView(User user) {
        Intent intent = new Intent(this, UserDetailsActivity.class);
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_name", user.getName());
        intent.putExtra("user_balance", user.getBalance());
        intent.putExtra("user_status", user.getStatus());
        startActivity(intent);
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null);
        
        EditText etName = view.findViewById(R.id.et_edit_name);
        EditText etBalance = view.findViewById(R.id.et_edit_balance);
        Spinner spinnerStatus = view.findViewById(R.id.spinner_status);

        // Populate Status Spinner
        String[] statuses = {"ACTIVE", "BLOCKED"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Set current values
        if (etName != null) etName.setText(user.getName());
        if (etBalance != null) etBalance.setText(String.valueOf(user.getBalance()));
        if (user.getStatus() != null) {
            int position = user.getStatus().equals("BLOCKED") ? 1 : 0;
            spinnerStatus.setSelection(position);
        }

        builder.setView(view);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();
            String newStatus = spinnerStatus.getSelectedItem().toString();

            if (!name.isEmpty() && !balanceStr.isEmpty()) {
                try {
                    double balance = Double.parseDouble(balanceStr);
                    String sanitizedEmail = user.getEmail().replace(".", ",");
                    
                    user.setName(name);
                    user.setBalance(balance);
                    user.setStatus(newStatus);

                    mDatabase.child("users").child(sanitizedEmail).setValue(user)
                        .addOnSuccessListener(aVoid -> Toast.makeText(AdminActivity.this, "User updated successfully", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid balance format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
