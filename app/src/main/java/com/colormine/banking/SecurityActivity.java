package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

public class SecurityActivity extends AppCompatActivity {

    private Switch switchBiometric, switchAlerts;
    private TextView tvPinStatus, tvSecurityScore, tvSecuritySubtitle;
    private ProgressBar securityProgress;
    private SharedPreferences sharedPreferences;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        sharedPreferences = getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);
        executor          = ContextCompat.getMainExecutor(this);

        // Header
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        View btnNotif = findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        }

        // Views
        switchBiometric    = findViewById(R.id.switch_biometric);
        switchAlerts       = findViewById(R.id.switch_alerts);
        tvPinStatus        = findViewById(R.id.tv_pin_status);
        tvSecurityScore    = findViewById(R.id.tv_security_score);
        tvSecuritySubtitle = findViewById(R.id.tv_security_subtitle);
        securityProgress   = findViewById(R.id.security_progress);

        // Load saved states
        switchBiometric.setChecked(sharedPreferences.getBoolean("biometric", false));
        switchAlerts.setChecked(sharedPreferences.getBoolean("alerts", true));

        // PIN status label
        String pin = sharedPreferences.getString("transactionPin", "");
        tvPinStatus.setText(pin.isEmpty()
            ? getString(R.string.transaction_pin_not_set)
            : getString(R.string.transaction_pin_set));

        setupBiometric();
        setupListeners();
        updateSecurityScore();
    }

    private void setupBiometric() {
        biometricPrompt = new BiometricPrompt(SecurityActivity.this, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // Don't uncheck if it was a user cancel
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        switchBiometric.setChecked(false);
                        saveBool("biometric", false);
                        updateSecurityScore();
                        Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    saveBool("biometric", true);
                    updateSecurityScore();
                    Toast.makeText(getApplicationContext(), "Biometric login enabled!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Login")
            .setSubtitle("Authenticate to activate fingerprint / face login")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG | BIOMETRIC_WEAK)
            .build();
    }

    private void setupListeners() {
        switchBiometric.setOnClickListener(v -> {
            boolean isChecked = switchBiometric.isChecked();
            if (isChecked) {
                BiometricManager biometricManager = BiometricManager.from(this);
                int canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG | BIOMETRIC_WEAK);
                if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                    biometricPrompt.authenticate(promptInfo);
                } else {
                    String msg = "Biometric not available";
                    if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        msg = "No biometric enrolled. Please set it up in your device settings.";
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    switchBiometric.setChecked(false);
                }
            } else {
                saveBool("biometric", false);
                updateSecurityScore();
                Toast.makeText(this, "Biometric login disabled", Toast.LENGTH_SHORT).show();
            }
        });

        switchAlerts.setOnCheckedChangeListener((btn, isChecked) -> {
            saveBool("alerts", isChecked);
            updateSecurityScore();
            Toast.makeText(this, "Login alerts " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.option_pin).setOnClickListener(v -> showSetPinDialog());
        findViewById(R.id.option_change_password).setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        findViewById(R.id.option_sessions).setOnClickListener(v -> showActiveSessionsDialog());
    }

    private void showSetPinDialog() {
        boolean pinExists = !sharedPreferences.getString("transactionPin", "").isEmpty();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 0);

        EditText etPin = new EditText(this);
        etPin.setHint("Enter 4-digit PIN");
        etPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setMaxLines(1);
        layout.addView(etPin);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(pinExists ? "Change Transaction PIN" : "Set Transaction PIN")
            .setView(layout)
            .setPositiveButton("Save PIN", (dialog, which) -> {
                String pin = etPin.getText().toString().trim();
                if (pin.length() == 4) {
                    sharedPreferences.edit().putString("transactionPin", pin).apply();
                    tvPinStatus.setText(getString(R.string.transaction_pin_set));
                    updateSecurityScore();
                    Toast.makeText(this, "Transaction PIN saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null);

        if (pinExists) {
            builder.setNeutralButton("Remove PIN", (dialog, which) -> {
                sharedPreferences.edit().remove("transactionPin").apply();
                tvPinStatus.setText(getString(R.string.transaction_pin_not_set));
                updateSecurityScore();
                Toast.makeText(this, "Transaction PIN removed", Toast.LENGTH_SHORT).show();
            });
        }
        builder.show();
    }

    private void showActiveSessionsDialog() {
        String deviceModel  = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVer   = "Android " + Build.VERSION.RELEASE;
        SharedPreferences sessionPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        long loginTime = sessionPref.getLong("loginTime", System.currentTimeMillis());
        String timeStr = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()).format(new Date(loginTime));

        String message = "📱  Current Device\n" + deviceModel + "\n" + androidVer  + "\n\n" +
                         "🕐  Session Started\n" + timeStr + "\n\n" +
                         "✅  This is your only active session.\n\n" +
                         "If you notice any suspicious activity, change your password immediately.";

        new AlertDialog.Builder(this)
            .setTitle("Active Sessions")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void updateSecurityScore() {
        int score = 15; // base
        if (sharedPreferences.getBoolean("biometric", false)) score += 40;
        if (!sharedPreferences.getString("transactionPin", "").isEmpty()) score += 30;
        if (sharedPreferences.getBoolean("alerts", true)) score += 15;

        tvSecurityScore.setText(score + "%");
        securityProgress.setProgress(score);

        if (score >= 85) tvSecuritySubtitle.setText("Your account is well protected");
        else if (score >= 55) tvSecuritySubtitle.setText("Good — enable more options to improve");
        else tvSecuritySubtitle.setText("Enable features below to secure your account");
    }

    private void saveBool(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }
}
