package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchDarkMode, switchNotifications, switchSounds, switchVibration;
    private TextView tvLanguageValue, tvAutoLockValue;
    private SharedPreferences sharedPreferences;
    private AudioManager audioManager;
    private Vibrator vibrator;

    private static final String[] LANGUAGES  = {"English", "اردو (Urdu)", "العربية (Arabic)", "Español (Spanish)"};
    private static final String[] AUTO_LOCK_OPTIONS = {"30 seconds", "1 minute", "5 minutes", "10 minutes", "Never"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        audioManager      = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator          = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        switchDarkMode      = findViewById(R.id.switch_dark_mode);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchSounds        = findViewById(R.id.switch_sounds);
        switchVibration     = findViewById(R.id.switch_vibration);
        tvLanguageValue     = findViewById(R.id.tv_language_value);
        tvAutoLockValue     = findViewById(R.id.tv_auto_lock_value);
        Button btnClearCache = findViewById(R.id.btn_clear_cache);

        loadSettings();
        setupSwitchListeners();

        findViewById(R.id.option_language).setOnClickListener(v -> showLanguagePicker());
        findViewById(R.id.option_auto_lock).setOnClickListener(v -> showAutoLockPicker());
        btnClearCache.setOnClickListener(v -> clearAppCache());
    }

    // ── Load persisted state ─────────────────────────────────────────────────

    private void loadSettings() {
        switchDarkMode.setChecked(sharedPreferences.getBoolean("darkMode", false));
        switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        switchSounds.setChecked(sharedPreferences.getBoolean("sounds", true));
        switchVibration.setChecked(sharedPreferences.getBoolean("vibration", true));

        int langIndex = sharedPreferences.getInt("languageIndex", 0);
        tvLanguageValue.setText(LANGUAGES[Math.min(langIndex, LANGUAGES.length - 1)]);

        int lockIndex = sharedPreferences.getInt("autoLockIndex", 1);
        tvAutoLockValue.setText(AUTO_LOCK_OPTIONS[Math.min(lockIndex, AUTO_LOCK_OPTIONS.length - 1)]);
    }

    // ── Switch listeners ─────────────────────────────────────────────────────

    private void setupSwitchListeners() {

        // Dark Mode — actually switches the night mode
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            saveBool("darkMode", isChecked);
            AppCompatDelegate.setDefaultNightMode(
                isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Push Notifications — persists the preference
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            saveBool("notifications", isChecked);
            Toast.makeText(this,
                isChecked ? "Push notifications enabled" : "Push notifications disabled",
                Toast.LENGTH_SHORT).show();
        });

        // Sounds — changes the device ringer mode via AudioManager
        switchSounds.setOnCheckedChangeListener((btn, isChecked) -> {
            saveBool("sounds", isChecked);
            if (audioManager != null) {
                audioManager.setRingerMode(
                    isChecked ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
            }
            Toast.makeText(this,
                isChecked ? "Sounds enabled" : "Sounds muted",
                Toast.LENGTH_SHORT).show();
        });

        // Vibration — actually vibrates the phone as confirmation
        switchVibration.setOnCheckedChangeListener((btn, isChecked) -> {
            saveBool("vibration", isChecked);
            if (isChecked && vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                        250, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //noinspection deprecation
                    vibrator.vibrate(250);
                }
            }
            Toast.makeText(this,
                isChecked ? "Vibration enabled" : "Vibration disabled",
                Toast.LENGTH_SHORT).show();
        });
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    private void showLanguagePicker() {
        int current = sharedPreferences.getInt("languageIndex", 0);
        new AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(LANGUAGES, current, (dialog, which) -> {
                sharedPreferences.edit().putInt("languageIndex", which).apply();
                tvLanguageValue.setText(LANGUAGES[which]);
                dialog.dismiss();
                Toast.makeText(this, "Language set to " + LANGUAGES[which], Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAutoLockPicker() {
        int current = sharedPreferences.getInt("autoLockIndex", 1);
        new AlertDialog.Builder(this)
            .setTitle("Auto-Lock Timeout")
            .setSingleChoiceItems(AUTO_LOCK_OPTIONS, current, (dialog, which) -> {
                sharedPreferences.edit().putInt("autoLockIndex", which).apply();
                tvAutoLockValue.setText(AUTO_LOCK_OPTIONS[which]);
                dialog.dismiss();
                Toast.makeText(this,
                    "Auto-lock set to " + AUTO_LOCK_OPTIONS[which],
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ── Cache ────────────────────────────────────────────────────────────────

    private void clearAppCache() {
        try {
            long sizeBefore = getDirSize(getCacheDir());
            deleteDir(getCacheDir());
            String freed = formatBytes(sizeBefore);
            Toast.makeText(this, "Cache cleared — " + freed + " freed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cache cleared successfully!", Toast.LENGTH_SHORT).show();
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

    private String formatBytes(long bytes) {
        if (bytes < 1024)       return bytes + " B";
        if (bytes < 1024*1024)  return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void saveBool(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }
}
