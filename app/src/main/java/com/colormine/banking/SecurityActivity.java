package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.colormine.banking.utils.SettingsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.Executor;

public class SecurityActivity extends BaseActivity {

    private Switch switchBiometric, switchAlerts, switch2fa, switchFreeze, switchPin;
    private TextView tvPinStatus, tvSecurityScore, tvSecuritySubtitle;
    private ProgressBar securityProgress;
    private DatabaseReference mDatabase;
    private String userEmail;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        SharedPreferences sessionPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = sessionPref.getString("email", "");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupBiometric();
        loadSettings();
        updateSecurityScore();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        switchBiometric = findViewById(R.id.switch_biometric);
        switch2fa = findViewById(R.id.switch_2fa);
        switchFreeze = findViewById(R.id.switch_freeze);
        switchPin = findViewById(R.id.switch_pin);
        switchAlerts = findViewById(R.id.switch_alerts);
        
        tvPinStatus = findViewById(R.id.tv_pin_status);
        tvSecurityScore = findViewById(R.id.tv_security_score);
        tvSecuritySubtitle = findViewById(R.id.tv_security_subtitle);
        securityProgress = findViewById(R.id.security_progress);

        // Option rows
        findViewById(R.id.option_pin).setOnClickListener(v -> showPinSetupDialog());
        findViewById(R.id.option_change_password).setOnClickListener(v -> {
            // Action handled by click, switch is just visual
            Toast.makeText(this, "Redirecting to Change Password...", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.option_sessions).setOnClickListener(v -> {
            // Action handled by click, switch is just visual
            Toast.makeText(this, "Viewing Active Sessions...", Toast.LENGTH_SHORT).show();
        });

        // Toggle listeners
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                settingsManager.setBiometricEnabled(false);
                updateSecurityScore();
            }
        });

        switch2fa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.set2FAEnabled(isChecked);
            updateSecurityScore();
        });

        switchFreeze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setAccountFrozen(isChecked);
            updateSecurityScore();
            if (isChecked) {
                Toast.makeText(this, "Account Frozen. All transfers blocked.", Toast.LENGTH_LONG).show();
            }
        });

        switchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setLoginAlertsEnabled(isChecked);
        });
    }

    private void setupBiometric() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(SecurityActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switchBiometric.setChecked(false);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                settingsManager.setBiometricEnabled(true);
                updateSecurityScore();
                Toast.makeText(getApplicationContext(), "Biometric Login Enabled!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void loadSettings() {
        switchBiometric.setChecked(settingsManager.isBiometricEnabled());
        switch2fa.setChecked(settingsManager.is2FAEnabled());
        switchFreeze.setChecked(settingsManager.isAccountFrozen());
        switchAlerts.setChecked(settingsManager.isLoginAlertsEnabled());
        updatePinStatus();
    }

    private void updatePinStatus() {
        String pin = settingsManager.getTransactionPin();
        boolean isSet = !pin.isEmpty();
        tvPinStatus.setText(isSet ? "Active" : "Not Set");
        switchPin.setChecked(isSet);
    }

    private void updateSecurityScore() {
        int score = 0;
        if (!settingsManager.getTransactionPin().isEmpty()) score += 30;
        if (settingsManager.is2FAEnabled()) score += 30;
        if (settingsManager.isBiometricEnabled()) score += 20;
        if (settingsManager.isLoginAlertsEnabled()) score += 20;

        securityProgress.setProgress(score);
        tvSecurityScore.setText(score + "%");

        if (score >= 80) {
            tvSecuritySubtitle.setText("Your account is highly secured.");
        } else if (score >= 50) {
            tvSecuritySubtitle.setText("Standard protection. Enable more features.");
        } else {
            tvSecuritySubtitle.setText("Security is low. Action required!");
        }
    }

    private void showPinSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_setup_pin, null);
        builder.setView(view);

        EditText etPin = view.findViewById(R.id.et_pin);
        Button btnSave = view.findViewById(R.id.btn_save_pin);
        
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String pin = etPin.getText().toString();
            if (pin.length() == 4) {
                settingsManager.setTransactionPin(pin);
                updatePinStatus();
                updateSecurityScore();
                dialog.dismiss();
                Toast.makeText(this, "Transaction PIN set successfully!", Toast.LENGTH_SHORT).show();
            } else {
                etPin.setError("PIN must be 4 digits");
            }
        });

        dialog.show();
    }
}
