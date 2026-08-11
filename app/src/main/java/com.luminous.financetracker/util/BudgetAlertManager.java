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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetAlertManager {

    // Reusable Executor prevents the memory leak identified in the review
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void checkBudgets(Context context, TransactionDao dao) {
        executor.execute(() -> {
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);

            float dailyLimit = prefs.getFloat(Constants.KEY_LIMIT_0, 30.0f);
            float monthlyLimit = prefs.getFloat(Constants.KEY_LIMIT_1, 1000.0f);

            long startOfDay = TimeUtils.getStartOfDay();
            long startOfMonth = TimeUtils.getStartOfMonth();

            // Enforces the "Fixed" category domain rule via the updated DAO method
            double dailySpent = dao.getTotalVariableSpentSinceSync(startOfDay, Constants.CATEGORY_FIXED);
            double monthlySpent = dao.getTotalVariableSpentSinceSync(startOfMonth, Constants.CATEGORY_FIXED);

            createNotificationChannel(context);

            // FIX: Use Constants instead of hardcoded "Daily" and "Monthly"
            checkThreshold(context, prefs, Constants.PERIOD_DAILY, dailySpent, dailyLimit, startOfDay, 101);
            checkThreshold(context, prefs, Constants.PERIOD_MONTHLY, monthlySpent, monthlyLimit, startOfMonth, 102);

            // Garbage collection for SharedPreferences bloat
            cleanupOldPrefs(prefs, startOfDay, startOfMonth);
        });
    }

    private static void checkThreshold(Context context, SharedPreferences prefs, String period, double spent, float limit, long periodStart, int notificationId) {
        if (limit <= 0) return;

        double percentage = (spent / limit) * 100;

        // FIX: Use centralized key builder
        String key80 = buildNotifiedKey(period, 80, periodStart);
        String key100 = buildNotifiedKey(period, 100, periodStart);

        if (percentage >= 100 && !prefs.getBoolean(key100, false)) {
            String message = String.format("You spent RM %.2f, exceeding your %s limit of RM %.2f!", spent, period, limit);
            sendNotification(context, period + " Budget Exceeded \u26A0\uFE0F", message, notificationId);
            prefs.edit().putBoolean(key100, true).apply();

        } else if (percentage >= 80 && percentage < 100 && !prefs.getBoolean(key80, false)) {
            String message = String.format("You have reached %.2f%% of your %s budget.", percentage, period);
            sendNotification(context, period + " Budget Warning \uD83D\uDEA8", message, notificationId);
            prefs.edit().putBoolean(key80, true).apply();
        }
    }

    // FIX: Centralized key builder to prevent drift
    private static String buildNotifiedKey(String period, int threshold, long timestamp) {
        return period + "_" + threshold + "_notified_" + timestamp;
    }

    // FIX: Safer parsing utilizing the Constants and matching the exact builder pattern
    private static void cleanupOldPrefs(SharedPreferences prefs, long currentDailyStart, long currentMonthlyStart) {
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        boolean hasDeletions = false;

        for (String key : allEntries.keySet()) {
            if (!key.contains("_notified_")) continue;

            try {
                long timestamp = Long.parseLong(key.substring(key.lastIndexOf("_") + 1));

                if (key.startsWith(Constants.PERIOD_DAILY + "_") && timestamp < currentDailyStart) {
                    editor.remove(key);
                    hasDeletions = true;
                } else if (key.startsWith(Constants.PERIOD_MONTHLY + "_") && timestamp < currentMonthlyStart) {
                    editor.remove(key);
                    hasDeletions = true;
                }
            } catch (NumberFormatException e) {
                // Ignore gracefully if a malformed key sneaks in
            }
        }

        if (hasDeletions) {
            editor.apply();
        }
    }

    private static void sendNotification(Context context, String title, String message, int notificationId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_BUDGET_ALERTS)
                .setSmallIcon(R.mipmap.meowneytrack_round)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_BUDGET_ALERTS,
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