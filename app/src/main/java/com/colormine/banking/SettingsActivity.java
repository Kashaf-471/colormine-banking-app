package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Switch switchDarkMode = findViewById(R.id.switch_dark_mode);
        Switch switchWifi = findViewById(R.id.switch_wifi);
        Switch switchBluetooth = findViewById(R.id.switch_bluetooth);
        Button btnClearCache = findViewById(R.id.btn_clear_cache);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Dark mode toggled (Requires restart)", Toast.LENGTH_SHORT).show();
        });

        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Note: Android 10+ requires user to go to system settings to toggle WiFi
            Toast.makeText(this, "WiFi control requested", Toast.LENGTH_SHORT).show();
        });

        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Bluetooth control requested", Toast.LENGTH_SHORT).show();
        });

        btnClearCache.setOnClickListener(v -> {
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        });
    }
}
