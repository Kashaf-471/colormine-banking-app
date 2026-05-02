package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountActivity extends AppCompatActivity {

    private TextView tvName, tvEmail;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        tvName = findViewById(R.id.tv_user_name);
        tvEmail = findViewById(R.id.tv_user_email);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RelativeLayout optionPersonalInfo = findViewById(R.id.option_personal_info);
        RelativeLayout optionMyCards = findViewById(R.id.option_my_cards);
        RelativeLayout optionNotifications = findViewById(R.id.option_notifications);
        RelativeLayout optionSecurity = findViewById(R.id.option_security);
        RelativeLayout optionSettings = findViewById(R.id.option_settings);
        RelativeLayout optionHelp = findViewById(R.id.option_help);
        Button btnLogout = findViewById(R.id.btn_logout);

        loadProfileData();

        optionMyCards.setOnClickListener(v -> startActivity(new Intent(this, CardDetailActivity.class)));
        optionNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        optionSecurity.setOnClickListener(v -> startActivity(new Intent(this, SecurityActivity.class)));
        optionSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        optionHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginSignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileData() {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (tvName != null) tvName.setText(user.getName());
                    if (tvEmail != null) tvEmail.setText(user.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
