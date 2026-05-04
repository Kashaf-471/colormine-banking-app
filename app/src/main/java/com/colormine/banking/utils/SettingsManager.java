package com.colormine.banking.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsManager {
    private static final String PREF_NAME = "AppSettings";
    private static SettingsManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    private SettingsManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    // Dark Mode
    public boolean isDarkMode() {
        return prefs.getBoolean("darkMode", false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean("darkMode", enabled).apply();
        AppCompatDelegate.setDefaultNightMode(
            enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // Hide Balance
    public boolean isHideBalance() {
        return prefs.getBoolean("hideBalance", false);
    }

    public void setHideBalance(boolean enabled) {
        prefs.edit().putBoolean("hideBalance", enabled).apply();
    }

    // Notifications
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean("notifications", true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean("notifications", enabled).apply();
    }

    // Sounds
    public boolean isSoundsEnabled() {
        return prefs.getBoolean("sounds", true);
    }

    public void setSoundsEnabled(boolean enabled) {
        prefs.edit().putBoolean("sounds", enabled).apply();
    }

    // Vibration
    public boolean isVibrationEnabled() {
        return prefs.getBoolean("vibration", true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean("vibration", enabled).apply();
    }

    // Auto-Lock
    public int getAutoLockIndex() {
        return prefs.getInt("autoLockIndex", 1); // Default 1 minute
    }

    public void setAutoLockIndex(int index) {
        prefs.edit().putInt("autoLockIndex", index).apply();
    }

    public long getAutoLockTimeoutMillis() {
        int index = getAutoLockIndex();
        switch (index) {
            case 0: return 30 * 1000;      // 30 seconds
            case 1: return 60 * 1000;      // 1 minute
            case 2: return 5 * 60 * 1000;  // 5 minutes
            case 3: return 10 * 60 * 1000; // 10 minutes
            default: return -1;            // Never
        }
    }

    // Security Settings
    private SharedPreferences getSecurityPrefs() {
        return context.getSharedPreferences("SecuritySettings", Context.MODE_PRIVATE);
    }

    public boolean isBiometricEnabled() {
        return getSecurityPrefs().getBoolean("biometric", false);
    }

    public void setBiometricEnabled(boolean enabled) {
        getSecurityPrefs().edit().putBoolean("biometric", enabled).apply();
    }

    public boolean is2FAEnabled() {
        return getSecurityPrefs().getBoolean("2fa", false);
    }

    public void set2FAEnabled(boolean enabled) {
        getSecurityPrefs().edit().putBoolean("2fa", enabled).apply();
    }

    public boolean isAccountFrozen() {
        return getSecurityPrefs().getBoolean("frozen", false);
    }

    public void setAccountFrozen(boolean enabled) {
        getSecurityPrefs().edit().putBoolean("frozen", enabled).apply();
    }

    public boolean isLoginAlertsEnabled() {
        return getSecurityPrefs().getBoolean("alerts", true);
    }

    public void setLoginAlertsEnabled(boolean enabled) {
        getSecurityPrefs().edit().putBoolean("alerts", enabled).apply();
    }

    public String getTransactionPin() {
        return getSecurityPrefs().getString("transactionPin", "");
    }

    public void setTransactionPin(String pin) {
        getSecurityPrefs().edit().putString("transactionPin", pin).apply();
    }

    public void removeTransactionPin() {
        getSecurityPrefs().edit().remove("transactionPin").apply();
    }
}
