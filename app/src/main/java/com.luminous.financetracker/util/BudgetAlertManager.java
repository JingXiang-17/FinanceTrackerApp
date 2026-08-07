package com.luminous.financetracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.luminous.financetracker.R;
import com.luminous.financetracker.data.dao.TransactionDao;

import java.util.concurrent.Executors;

public class BudgetAlertManager {

    private static final String CHANNEL_ID = "budget_alerts";

    public static void checkBudgets(Context context, TransactionDao dao) {
        // Run on a background thread so we don't freeze the app
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = context.getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE);

            float dailyLimit = prefs.getFloat("limit_0", 30.0f);
            float weeklyLimit = prefs.getFloat("limit_1", 200.0f);
            float monthlyLimit = prefs.getFloat("limit_2", 1000.0f);

            // Fetch current totals directly from the database
            double dailySpent = dao.getTotalSpentSinceSync(TimeUtils.getStartOfDay());
            double weeklySpent = dao.getTotalSpentSinceSync(TimeUtils.getStartOfWeek());
            double monthlySpent = dao.getTotalSpentSinceSync(TimeUtils.getStartOfMonth());

            createNotificationChannel(context);

            // Check each time period (using unique IDs: 101, 102, 103 so they can stack in the tray)
            checkThreshold(context, prefs, "Daily", dailySpent, dailyLimit, TimeUtils.getStartOfDay(), 101);
            checkThreshold(context, prefs, "Weekly", weeklySpent, weeklyLimit, TimeUtils.getStartOfWeek(), 102);
            checkThreshold(context, prefs, "Monthly", monthlySpent, monthlyLimit, TimeUtils.getStartOfMonth(), 103);
        });
    }

    private static void checkThreshold(Context context, SharedPreferences prefs, String period, double spent, float limit, long periodStart, int notificationId) {
        if (limit <= 0) return; // Prevent division by zero

        double percentage = (spent / limit) * 100;

        // Dynamic keys tied to the start timestamp.
        // Example: "Daily_80_notified_1698710400000". This automatically resets when a new day/week/month starts!
        String key80 = period + "_80_notified_" + periodStart;
        String key100 = period + "_100_notified_" + periodStart;

        if (percentage >= 100 && !prefs.getBoolean(key100, false)) {
            String message = String.format("You spent RM %.2f, exceeding your %s limit of RM %.2f!", spent, period, limit);
            sendNotification(context, period + " Budget Exceeded \u26A0\uFE0F", message, notificationId);

            // Mark as notified
            prefs.edit().putBoolean(key100, true).apply();

        } else if (percentage >= 80 && percentage < 100 && !prefs.getBoolean(key80, false)) {
            String message = String.format("You have reached %.2f%% of your %s budget.", percentage, period);
            sendNotification(context, period + " Budget Warning \uD83D\uDEA8", message, notificationId);

            // Mark as notified
            prefs.edit().putBoolean(key80, true).apply();
        }
    }

    private static void sendNotification(Context context, String title, String message, int notificationId) {
        // Safety check for Android 13+ Notification Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // You can replace this with your own drawable
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Allows expanding for longer text
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for reaching 80% and 100% of your budgets");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}