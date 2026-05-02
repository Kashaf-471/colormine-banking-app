package com.colormine.banking;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.adapters.UserAdapter;
import com.colormine.banking.database.DatabaseHelper;
import com.colormine.banking.models.User;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements UserAdapter.OnUserActionListener {

    private DatabaseHelper dbHelper;
    private ListView listView;
    private UserAdapter adapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        dbHelper = new DatabaseHelper(this);
        listView = findViewById(R.id.list_users);
        userList = new ArrayList<>();

        loadUsers();

        // Logout implementation
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, LoginSignupActivity.class));
            finish();
        });
    }

    private void loadUsers() {
        userList.clear();
        Cursor cursor = dbHelper.getAllUsers();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL));
                String password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BALANCE));
                userList.add(new User(id, name, email, password, balance));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter = new UserAdapter(this, userList, this);
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
                .setMessage("Are you sure you want to delete " + user.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbHelper.deleteUser(user.getId())) {
                        Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onView(User user) {
        Cursor cursor = dbHelper.getUserTransactions(user.getEmail());
        StringBuilder details = new StringBuilder();
        details.append("User: ").append(user.getName()).append("\n");
        details.append("Email: ").append(user.getEmail()).append("\n");
        details.append("Balance: $").append(String.format("%.2f", user.getBalance())).append("\n\n");
        details.append("Transaction History:\n");

        if (cursor != null && cursor.moveToFirst()) {
            int count = 1;
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TYPE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));

                details.append(count).append(". ").append(title)
                        .append(" (").append(type).append(")\n")
                        .append("   Amount: $").append(String.format("%.2f", amount))
                        .append(" | Date: ").append(date).append("\n");
                count++;
            } while (cursor.moveToNext() && count <= 10); // Show last 10
            cursor.close();
        } else {
            details.append("No transactions found for this user.");
        }

        new AlertDialog.Builder(this)
                .setTitle("View User Data")
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_user, null);
        EditText etName = view.findViewById(R.id.et_edit_name);
        EditText etEmail = view.findViewById(R.id.et_edit_email);
        EditText etBalance = view.findViewById(R.id.et_edit_balance);

        etName.setText(user.getName());
        etEmail.setText(user.getEmail());
        etBalance.setText(String.valueOf(user.getBalance()));

        builder.setView(view);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String balanceStr = etBalance.getText().toString().trim();

            if (!name.isEmpty() && !email.isEmpty() && !balanceStr.isEmpty()) {
                try {
                    double balance = Double.parseDouble(balanceStr);
                    if (dbHelper.updateUser(user.getId(), name, email, balance)) {
                        Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
                        loadUsers();
                    }
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
