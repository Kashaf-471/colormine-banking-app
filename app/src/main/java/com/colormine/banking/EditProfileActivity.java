package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress;
    private Button btnSave;
    private DatabaseReference mDatabase;
    private String userEmail;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        initViews();
        loadUserData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_edit_name);
        etPhone = findViewById(R.id.et_edit_phone);
        etAddress = findViewById(R.id.et_edit_address);
        btnSave = findViewById(R.id.btn_save_profile);

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    etName.setText(currentUser.getName());
                    etPhone.setText(currentUser.getPhoneNumber());
                    etAddress.setText(currentUser.getAddress());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        if (currentUser == null) return;

        currentUser.setName(name);
        currentUser.setPhoneNumber(phone);
        currentUser.setAddress(address);

        String sanitizedEmail = userEmail.replace(".", ",");
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        mDatabase.child("users").child(sanitizedEmail).setValue(currentUser)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
