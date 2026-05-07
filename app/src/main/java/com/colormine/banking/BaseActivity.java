package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.colormine.banking.utils.SettingsManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.view.View;

public class BaseActivity extends AppCompatActivity {

    protected SettingsManager settingsManager;

    // ✅ Auto-lock variables
    private Handler logoutHandler;
    private Runnable logoutRunnable;
    private long timeoutMillis;
    private com.google.firebase.database.ValueEventListener statusListener;
    private com.google.firebase.database.DatabaseReference statusRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        settingsManager = SettingsManager.getInstance(this);

        // ✅ Apply Dark Mode BEFORE super.onCreate
        if (settingsManager.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        // ✅ Initialize handler
        logoutHandler = new Handler(Looper.getMainLooper());

        setupAutoLock();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    // ✅ Setup auto-lock logic
    private void setupAutoLock() {
        timeoutMillis = settingsManager.getAutoLockTimeoutMillis();

        logoutRunnable = () -> {
            if (timeoutMillis != -1 && isUserLoggedIn()) {
                logout();
            }
        };
    }

    private boolean isUserLoggedIn() {
        // ❗ Avoid locking auth screens
        if (this instanceof LoginSignupActivity ||
                this instanceof SplashActivity ||
                this instanceof OnboardingActivity) {
            return false;
        }

        return getSharedPreferences("UserSession", MODE_PRIVATE)
                .getString("email", null) != null;
    }

    private void logout() {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(this, LoginSignupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ✅ Reset timer on user interaction
    public void resetDisconnectTimer() {
        if (timeoutMillis != -1 && logoutHandler != null && logoutRunnable != null) {
            logoutHandler.removeCallbacks(logoutRunnable);
            logoutHandler.postDelayed(logoutRunnable, timeoutMillis);
        }
    }

    public void stopDisconnectTimer() {
        if (logoutHandler != null && logoutRunnable != null) {
            logoutHandler.removeCallbacks(logoutRunnable);
        }
    }

    // ✅ Click feedback
    public void playClickFeedback() {
        if (settingsManager.isVibrationEnabled()) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(40);
                }
            }
        }

        if (settingsManager.isSoundsEnabled()) {
            try {
                // Use a crisp system sound on the Music stream which is less likely to be muted than System stream
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
                // Release after a short delay to ensure it plays but doesn't leak
                new Handler(Looper.getMainLooper()).postDelayed(tg::release, 500);
            } catch (Exception ignored) {}
        }
    }

    public void performHaptic(View view) {
        if (settingsManager.isVibrationEnabled()) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    // ✅ Success feedback
    public void playSuccessFeedback() {
        if (settingsManager.isVibrationEnabled()) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    );
                } else {
                    vibrator.vibrate(100);
                }
            }
        }

        if (settingsManager.isSoundsEnabled()) {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                if (ringtone != null) {
                    ringtone.play();
                } else {
                    // Fallback to ToneGenerator if no ringtone
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_ACK);
                    new Handler(Looper.getMainLooper()).postDelayed(tg::release, 500);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetDisconnectTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetDisconnectTimer();
        startStatusMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStatusMonitor();
    }

    private void startStatusMonitor() {
        if (!isUserLoggedIn()) return;

        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String email = pref.getString("email", "");
        if (email.isEmpty()) return;

        String sanitizedEmail = email.replace(".", ",");
        statusRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(sanitizedEmail).child("status");

        if (statusListener == null) {
            statusListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.getValue(String.class);
                    if ("BLOCKED".equalsIgnoreCase(status)) {
                        handleAccountBlocked();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
        }
        statusRef.addValueEventListener(statusListener);
    }

    private void stopStatusMonitor() {
        if (statusRef != null && statusListener != null) {
            statusRef.removeEventListener(statusListener);
        }
    }

    private void handleAccountBlocked() {
        if (isFinishing()) return;

        // Clear session and redirect
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
        
        // Use a flag to prevent multiple dialogs if multiple activities are in stack
        if (!isAccountBlockedDialogShowing) {
            isAccountBlockedDialogShowing = true;
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Blocked")
                .setMessage("Your account has been blocked by an administrator. You will be redirected to the login screen.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    isAccountBlockedDialogShowing = false;
                    Intent intent = new Intent(this, LoginSignupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
        }
    }

    private static boolean isAccountBlockedDialogShowing = false;

    @Override
    protected void onStop() {
        super.onStop();
        stopDisconnectTimer();
    }
}