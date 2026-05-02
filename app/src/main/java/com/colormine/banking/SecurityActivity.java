package com.colormine.banking;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        Switch switchBiometric = findViewById(R.id.switch_biometric);
        Switch switch2fa = findViewById(R.id.switch_2fa);
        Switch switchAlerts = findViewById(R.id.switch_alerts);

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Biometric login " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        switch2fa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "2FA " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });
    }
}
