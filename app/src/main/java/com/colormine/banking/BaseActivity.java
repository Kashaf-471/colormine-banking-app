package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.colormine.banking.utils.SettingsManager;

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
                // Use a crisp system sound
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, 60);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
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
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_ACK);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopDisconnectTimer();
    }
}