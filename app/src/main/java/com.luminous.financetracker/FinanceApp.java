package com.luminous.financetracker;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class FinanceApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Force Light Mode globally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        /*// 1. Load the user's saved theme preference
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);

        // 2. Default to MODE_NIGHT_FOLLOW_SYSTEM if they haven't chosen one yet
        int savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // 3. Apply the theme globally to all activities instantly
        AppCompatDelegate.setDefaultNightMode(savedTheme);*/
    }
}