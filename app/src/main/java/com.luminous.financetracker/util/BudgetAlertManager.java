package com.luminous.financetracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.luminous.financetracker.R;
import com.luminous.financetracker.data.dao.TransactionDao;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetAlertManager {

    private static final String TAG = "BudgetAlert";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void checkBudgets(Context context, TransactionDao dao) {
        executor.execute(() -> {
            Log.d(TAG, "checkBudgets triggered!");
            SharedPreferences prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);

            float dailyLimit = prefs.getFloat(Constants.KEY_LIMIT_0, 30.0f);
            float monthlyLimit = prefs.getFloat(Constants.KEY_LIMIT_1, 1000.0f);

            long startOfDay = TimeUtils.getStartOfDay();
            long startOfMonth = TimeUtils.getStartOfMonth();

            double dailySpent = dao.getTotalVariableSpentSinceSync(startOfDay, Constants.CATEGORY_FIXED);
            double monthlySpent = dao.getTotalVariableSpentSinceSync(startOfMonth, Constants.CATEGORY_FIXED);

            Log.d(TAG, String.format("Daily Spent: RM%.2f / Limit: RM%.2f", dailySpent, dailyLimit));
            Log.d(TAG, String.format("Monthly Spent: RM%.2f / Limit: RM%.2f", monthlySpent, monthlyLimit));

            createNotificationChannel(context);

            checkThreshold(context, prefs, Constants.PERIOD_DAILY, dailySpent, dailyLimit, startOfDay, 101);
            checkThreshold(context, prefs, Constants.PERIOD_MONTHLY, monthlySpent, monthlyLimit, startOfMonth, 102);

            cleanupOldPrefs(prefs, startOfDay, startOfMonth);
        });
    }

    private static void checkThreshold(Context context, SharedPreferences prefs, String period, double spent, float limit, long periodStart, int notificationId) {
        if (limit <= 0) {
            Log.d(TAG, period + " limit is 0 or unconfigured. Skipping.");
            return;
        }

        double percentage = (spent / limit) * 100;
        Log.d(TAG, String.format("%s budget progress: %.2f%%", period, percentage));

        String key80 = buildNotifiedKey(period, 80, periodStart);
        String key100 = buildNotifiedKey(period, 100, periodStart);

        boolean alreadyNotified80 = prefs.getBoolean(key80, false);
        boolean alreadyNotified100 = prefs.getBoolean(key100, false);

        if (percentage >= 100) {
            if (!alreadyNotified100) {
                String message = String.format("You spent RM %.2f, exceeding your %s limit of RM %.2f!", spent, period, limit);
                Log.d(TAG, "Firing 100% alert for " + period);
                sendNotification(context, period + " Budget Exceeded \u26A0\uFE0F", message, notificationId);
                prefs.edit().putBoolean(key100, true).apply();
            } else {
                Log.d(TAG, period + " 100% alert was already sent previously.");
            }
        } else if (percentage >= 80) {
            if (!alreadyNotified80) {
                String message = String.format("You have reached %.2f%% of your %s budget.", percentage, period);
                Log.d(TAG, "Firing 80% warning for " + period);
                sendNotification(context, period + " Budget Warning \uD83D\uDEA8", message, notificationId);
                prefs.edit().putBoolean(key80, true).apply();
            } else {
                Log.d(TAG, period + " 80% alert was already sent previously.");
            }
        }
    }

    private static String buildNotifiedKey(String period, int threshold, long timestamp) {
        return period + "_" + threshold + "_notified_" + timestamp;
    }

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
                Log.e(TAG, "POST_NOTIFICATIONS permission not granted. Cannot post budget alert.");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_BUDGET_ALERTS)
                .setSmallIcon(R.mipmap.meowneytrack_beta)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        Log.d(TAG, "Notification successfully pushed to system: " + title);
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