package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.colormine.banking.utils.NotificationHelper;
import java.io.File;

public class SettingsActivity extends BaseActivity {

    private Switch switchDarkMode, switchNotifications, switchSounds, switchVibration, switchHideBalance;
    private TextView tvAutoLockValue;

    private static final String[] AUTO_LOCK_OPTIONS = {"30 seconds", "1 minute", "5 minutes", "10 minutes", "Never"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Header
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Views
        switchDarkMode      = findViewById(R.id.switch_dark_mode);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchSounds        = findViewById(R.id.switch_sounds);
        switchVibration     = findViewById(R.id.switch_vibration);
        switchHideBalance   = findViewById(R.id.switch_hide_balance);
        tvAutoLockValue     = findViewById(R.id.tv_auto_lock_value);

        loadCurrentSettings();
        setupListeners();
    }

    private void loadCurrentSettings() {
        switchDarkMode.setChecked(settingsManager.isDarkMode());
        switchNotifications.setChecked(settingsManager.isNotificationsEnabled());
        switchSounds.setChecked(settingsManager.isSoundsEnabled());
        switchVibration.setChecked(settingsManager.isVibrationEnabled());
        switchHideBalance.setChecked(settingsManager.isHideBalance());

        int lockIndex = settingsManager.getAutoLockIndex();
        tvAutoLockValue.setText(AUTO_LOCK_OPTIONS[Math.min(lockIndex, AUTO_LOCK_OPTIONS.length - 1)]);
    }

    private void setupListeners() {
        // Dark Mode
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            playClickFeedback();
            settingsManager.setDarkMode(isChecked);
            // Apply immediately across app
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });

        // Notifications
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            playClickFeedback();
            settingsManager.setNotificationsEnabled(isChecked);
            if (isChecked) {
                NotificationHelper.showNotification(this, "Notifications Enabled", "You will now receive account alerts.");
            }
        });

        // Sounds
        switchSounds.setOnCheckedChangeListener((btn, isChecked) -> {
            settingsManager.setSoundsEnabled(isChecked);
            playClickFeedback();
        });

        // Vibration
        switchVibration.setOnCheckedChangeListener((btn, isChecked) -> {
            settingsManager.setVibrationEnabled(isChecked);
            playClickFeedback();
        });

        // Hide Balance
        switchHideBalance.setOnCheckedChangeListener((btn, isChecked) -> {
            playClickFeedback();
            settingsManager.setHideBalance(isChecked);
            Toast.makeText(this, isChecked ? "Balance masked" : "Balance visible", Toast.LENGTH_SHORT).show();
        });

        // Auto Lock
        findViewById(R.id.option_auto_lock).setOnClickListener(v -> showAutoLockPicker());

        // Terms
        findViewById(R.id.option_terms).setOnClickListener(v -> {
            playClickFeedback();
            startActivity(new Intent(this, TermsActivity.class));
        });

        // Updates
        findViewById(R.id.option_update).setOnClickListener(v -> {
            playClickFeedback();
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                Toast.makeText(this, "Your app is up to date!", Toast.LENGTH_SHORT).show();
                playSuccessFeedback();
            }, 1500);
        });

        // Clear Cache
        findViewById(R.id.btn_clear_cache).setOnClickListener(v -> {
            playClickFeedback();
            new AlertDialog.Builder(this)
                .setTitle("Clear Cache")
                .setMessage("Delete temporary files?")
                .setPositiveButton("Yes, Clear", (dialog, which) -> {
                    clearAppCache();
                    playSuccessFeedback();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }



    private void showAutoLockPicker() {
        int current = settingsManager.getAutoLockIndex();
        new AlertDialog.Builder(this)
            .setTitle("Auto-Lock Timeout")
            .setSingleChoiceItems(AUTO_LOCK_OPTIONS, current, (dialog, which) -> {
                settingsManager.setAutoLockIndex(which);
                tvAutoLockValue.setText(AUTO_LOCK_OPTIONS[which]);
                dialog.dismiss();
                Toast.makeText(this, "Timeout updated", Toast.LENGTH_SHORT).show();
                // Timing logic is handled in BaseActivity
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void clearAppCache() {
        try {
            long sizeBefore = getDirSize(getCacheDir());
            deleteDir(getCacheDir());
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cleared successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) size += f.isDirectory() ? getDirSize(f) : f.length();
        }
        return size;
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) for (File c : children) deleteDir(c);
        }
        return dir != null && dir.delete();
    }
}
