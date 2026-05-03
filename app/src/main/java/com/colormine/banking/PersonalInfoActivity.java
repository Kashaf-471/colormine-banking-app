package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PersonalInfoActivity extends AppCompatActivity {

    private TextView tvFullName, tvDetailEmail, tvDetailPhone, tvDetailAddress;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvFullName = findViewById(R.id.tv_full_name);
        tvDetailEmail = findViewById(R.id.tv_detail_email);
        tvDetailPhone = findViewById(R.id.tv_detail_phone);
        tvDetailAddress = findViewById(R.id.tv_detail_address);

        Button btnEdit = findViewById(R.id.btn_edit_profile);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(PersonalInfoActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvFullName.setText(user.getName());
                    tvDetailEmail.setText(user.getEmail());
                    
                    String phone = user.getPhoneNumber();
                    tvDetailPhone.setText(phone == null || phone.isEmpty() ? "Not set" : phone);
                    
                    String address = user.getAddress();
                    tvDetailAddress.setText(address == null || address.isEmpty() ? "Not set" : address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PersonalInfoActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
