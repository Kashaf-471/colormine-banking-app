package com.colormine.banking;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ColorMineApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply Dark Mode preference globally at startup using SettingsManager
        com.colormine.banking.utils.SettingsManager settingsManager = com.colormine.banking.utils.SettingsManager.getInstance(this);
        
        if (settingsManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
