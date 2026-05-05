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

    private Switch switchBiometric, switchAlerts, switch2fa, switchFreeze;
    private TextView tvSecurityScore, tvSecuritySubtitle;
    private ProgressBar securityProgress;
    private DatabaseReference mDatabase;
    private String userEmail;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private boolean isInitializing = true;

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
        isInitializing = false;
        updateSecurityScore();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        switchBiometric = findViewById(R.id.switch_biometric);
        switch2fa = findViewById(R.id.switch_2fa);
        switchFreeze = findViewById(R.id.switch_freeze);
        switchAlerts = findViewById(R.id.switch_alerts);
        
        tvSecurityScore = findViewById(R.id.tv_security_score);
        tvSecuritySubtitle = findViewById(R.id.tv_security_subtitle);
        securityProgress = findViewById(R.id.security_progress);

        // Option rows
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
            if (isInitializing) return;
            playClickFeedback();
            if (isChecked) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                settingsManager.setBiometricEnabled(false);
                updateSecurityScore();
                Toast.makeText(this, "Biometric Login Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        switch2fa.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitializing) return;
            playClickFeedback();
            settingsManager.set2FAEnabled(isChecked);
            
            // Sync to Firebase
            String sanitizedEmail = userEmail.replace(".", ",");
            mDatabase.child("users").child(sanitizedEmail).child("settings").child("twoFactor")
                    .setValue(isChecked)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, isChecked ? "2-Step Verification Enabled" : "2-Step Verification Disabled", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update 2FA: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Revert switch if failed
                        isInitializing = true;
                        switch2fa.setChecked(!isChecked);
                        isInitializing = false;
                    });

            updateSecurityScore();
        });


        switchFreeze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitializing) return;
            playClickFeedback();
            settingsManager.setAccountFrozen(isChecked);
            
            // Sync to Firebase
            String sanitizedEmail = userEmail.replace(".", ",");
            mDatabase.child("users").child(sanitizedEmail).child("status")
                    .setValue(isChecked ? "FROZEN" : "ACTIVE");

            updateSecurityScore();
            if (isChecked) {
                Toast.makeText(this, "Account Frozen. All transfers blocked.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Account Unfrozen.", Toast.LENGTH_SHORT).show();
            }
        });

        switchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitializing) return;
            playClickFeedback();
            settingsManager.setLoginAlertsEnabled(isChecked);
            Toast.makeText(this, isChecked ? "Login alerts enabled" : "Login alerts disabled", Toast.LENGTH_SHORT).show();
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
    }


    private void updateSecurityScore() {
        int score = 0;
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

}
