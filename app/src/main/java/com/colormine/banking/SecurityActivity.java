package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class SecurityActivity extends AppCompatActivity {

    private Switch switchBiometric, switch2fa, switchAlerts;
    private DatabaseReference mDatabase;
    private String userEmail;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        switchBiometric = findViewById(R.id.switch_biometric);
        switch2fa = findViewById(R.id.switch_2fa);
        switchAlerts = findViewById(R.id.switch_alerts);

        if (!userEmail.isEmpty()) {
            loadSecuritySettings();
        }

        setupListeners();
    }

    private void loadSecuritySettings() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).child("settings")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isUpdating = true;
                    if (snapshot.exists()) {
                        Boolean biometric = snapshot.child("biometric").getValue(Boolean.class);
                        Boolean twoFactor = snapshot.child("twoFactor").getValue(Boolean.class);
                        Boolean notifications = snapshot.child("notifications").getValue(Boolean.class);

                        if (biometric != null) switchBiometric.setChecked(biometric);
                        if (twoFactor != null) switch2fa.setChecked(twoFactor);
                        if (notifications != null) switchAlerts.setChecked(notifications);
                    }
                    isUpdating = false;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void setupListeners() {
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) updateSetting("biometric", isChecked);
        });

        switch2fa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) updateSetting("twoFactor", isChecked);
        });

        switchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) updateSetting("notifications", isChecked);
        });
    }

    private void updateSetting(String key, boolean value) {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).child("settings").child(key).setValue(value)
            .addOnSuccessListener(aVoid -> Toast.makeText(SecurityActivity.this, "Security updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(SecurityActivity.this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
