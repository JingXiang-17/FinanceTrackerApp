package com.luminous.financetracker;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;

public class FinanceApp extends Application {

    // Must match the exact string you used in your NotificationBuilder!
    public static final String CHANNEL_ID = "default_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Force Light Mode globally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // --- ADDED: Create the Notification Channel ---
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Notification channels are only required on Android 8.0 (Oreo) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Transaction Alerts";
            String description = "Notifications when new transactions are auto-logged";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}