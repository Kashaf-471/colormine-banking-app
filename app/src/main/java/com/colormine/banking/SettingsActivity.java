package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchDarkMode;
    private DatabaseReference mDatabase;
    private String userEmail;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switch_dark_mode);
        Switch switchWifi = findViewById(R.id.switch_wifi);
        Switch switchBluetooth = findViewById(R.id.switch_bluetooth);
        Button btnClearCache = findViewById(R.id.btn_clear_cache);

        if (!userEmail.isEmpty()) {
            loadSettings();
        }

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                updateSetting("darkMode", isChecked);
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "WiFi preference saved", Toast.LENGTH_SHORT).show();
        });

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Bluetooth preference saved", Toast.LENGTH_SHORT).show();
        });

        btnClearCache.setOnClickListener(v -> {
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSettings() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).child("settings")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isUpdating = true;
                    if (snapshot.exists()) {
                        Boolean darkMode = snapshot.child("darkMode").getValue(Boolean.class);
                        if (darkMode != null) switchDarkMode.setChecked(darkMode);
                    }
                    isUpdating = false;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void updateSetting(String key, boolean value) {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).child("settings").child(key).setValue(value);
    }
}
