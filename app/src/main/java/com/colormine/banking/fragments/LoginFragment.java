package com.colormine.banking.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.colormine.banking.AdminActivity;
import com.colormine.banking.ForgotPasswordActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.R;
import com.colormine.banking.models.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import com.colormine.banking.OtpService;
import com.colormine.banking.VerifyOtpActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import static android.app.Activity.RESULT_OK;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;

public class LoginFragment extends Fragment {

    private com.google.android.material.textfield.TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private ImageView btnBiometric;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences securityPrefs;
    private ActivityResultLauncher<Intent> otpLauncher;
    private com.colormine.banking.models.User pendingUser;
    private String pendingEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        securityPrefs = requireActivity().getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);

        tilEmail = view.findViewById(R.id.til_login_email);
        tilPassword = view.findViewById(R.id.til_login_password);
        etEmail = view.findViewById(R.id.et_login_email);
        etPassword = view.findViewById(R.id.et_login_password);
        btnLogin = view.findViewById(R.id.btn_login);
        progressBar = view.findViewById(R.id.login_progress);
        btnBiometric = view.findViewById(R.id.btn_biometric_login);
        TextView linkForgot = view.findViewById(R.id.link_forgot_password);

        btnLogin.setOnClickListener(v -> attemptLogin());
        linkForgot.setOnClickListener(v -> {
            if (getActivity() instanceof com.colormine.banking.BaseActivity) {
                ((com.colormine.banking.BaseActivity) getActivity()).playClickFeedback();
            }
            startActivity(new Intent(getActivity(), ForgotPasswordActivity.class));
        });

        otpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // OTP Verified successfully, proceed to dashboard
                    proceedToDashboard(pendingUser, pendingEmail);
                } else {
                    setLoading(false);
                    Toast.makeText(getContext(), "Verification cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Setup Biometric Login if enabled
        if (securityPrefs.getBoolean("biometric", false)) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> startBiometricAuth());
            
            // Auto-fill last email
            String lastEmail = securityPrefs.getString("last_email", "");
            if (!lastEmail.isEmpty()) {
                etEmail.setText(lastEmail);
            }
        } else {
            btnBiometric.setVisibility(View.GONE);
        }

        return view;
    }

    private void attemptLogin() {
        if (getActivity() instanceof com.colormine.banking.BaseActivity) {
            ((com.colormine.banking.BaseActivity) getActivity()).playClickFeedback();
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        String sanitizedEmail = email.replace(".", ",");
        
        // 1. Fetch user from Database to check current password
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                if (userProfile != null) {
                    // 2. Verify Database Password
                    if (password.equals(userProfile.getPassword())) {
                        // DB Password matches! 
                        // Now attempt Firebase Auth sign-in in background
                        mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(requireActivity(), task -> {
                                // We proceed regardless of Auth success if DB password matched,
                                // but we prefer Auth success for better security/features.
                                if (task.isSuccessful()) {
                                    securityPrefs.edit().putString("last_email", email).apply();
                                }
                                // Proceed with role check
                                checkUserRoleAndStatus(email);
                            });
                    } else {
                        // Password doesn't match DB
                        setLoading(false);
                        tilPassword.setError("Invalid password.");
                        etPassword.requestFocus();
                    }
                } else {
                    // User not found in DB
                    setLoading(false);
                    tilEmail.setError("No account found with this email.");
                    etEmail.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(getContext(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startBiometricAuth() {
        String savedEmail = securityPrefs.getString("last_email", "");
        if (savedEmail.isEmpty()) {
            Toast.makeText(getContext(), "Please login with password once to enable biometric", Toast.LENGTH_LONG).show();
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG | BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(getContext(), "Biometric login is not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                setLoading(true);
                checkUserRoleAndStatus(savedEmail);
            }
            
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(getContext(), "Biometric error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Log in as " + savedEmail)
                .setNegativeButtonText("Use Password")
                .setAllowedAuthenticators(BIOMETRIC_STRONG | BIOMETRIC_WEAK)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void checkUserRoleAndStatus(String email) {
        String sanitizedEmail = email.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if ("BLOCKED".equals(user.getStatus())) {
                        mAuth.signOut();
                        Toast.makeText(getContext(), "Account Suspended: Please contact support.", Toast.LENGTH_LONG).show();
                    } else {
                        if (securityPrefs.getBoolean("alerts", true)) {
                            sendLoginAlert(sanitizedEmail);
                        }

                        if (user.getSettings() != null && Boolean.TRUE.equals(user.getSettings().get("twoFactor"))) {
                            // 2FA Enabled - Send OTP and verify
                            pendingUser = user;
                            pendingEmail = email;
                            Toast.makeText(getContext(), "2-Step Verification required", Toast.LENGTH_SHORT).show();
                            sendOtpAndVerify(email);
                        } else {
                            // 2FA Disabled - Proceed directly
                            Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                            proceedToDashboard(user, email);
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
            }
        });
    }

    private void sendOtpAndVerify(String email) {
        setLoading(true);
        OtpService.generateAndSend(requireContext(), email, new OtpService.OtpCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Intent intent = new Intent(getActivity(), VerifyOtpActivity.class);
                intent.putExtra("email", email);
                intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_LOGIN);
                otpLauncher.launch(intent);
            }

            @Override
            public void onFallback(String error) {
                setLoading(false);
                Toast.makeText(getContext(), "Failed to send verification code: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void proceedToDashboard(User user, String email) {
        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        pref.edit()
            .putString("email", email)
            .putInt("isAdmin", user.getIsAdmin())
            .putLong("loginTime", System.currentTimeMillis())
            .apply();
        
        // Apply theme immediately before navigating
        if (user.getIsAdmin() == 1) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            boolean isDark = com.colormine.banking.utils.SettingsManager.getInstance(requireContext()).isDarkMode();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );
        }
        
        if (user.getIsAdmin() == 1) {
            startActivity(new Intent(getActivity(), AdminActivity.class));
        } else {
            startActivity(new Intent(getActivity(), MainActivity.class));
        }
        requireActivity().finish();
    }

    private void sendLoginAlert(String sanitizedEmail) {
        String id = mDatabase.child("notifications").child(sanitizedEmail).push().getKey();
        String time = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", "New Login Alert");
        notif.put("message", "A new login was detected on your account at " + time);
        notif.put("timestamp", System.currentTimeMillis());
        if (id != null) mDatabase.child("notifications").child(sanitizedEmail).child(id).setValue(notif);
        Toast.makeText(getContext(), "Security Alert: New login detected", Toast.LENGTH_SHORT).show();
    }

    private void handleAuthError(Exception e) {
        String errorMessage = "Authentication failed.";
        if (e instanceof FirebaseAuthInvalidUserException) errorMessage = "No account found.";
        else if (e instanceof FirebaseAuthInvalidCredentialsException) errorMessage = "Invalid password.";
        else if (e instanceof FirebaseNetworkException) errorMessage = "Network error.";
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnLogin != null) btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }
}
