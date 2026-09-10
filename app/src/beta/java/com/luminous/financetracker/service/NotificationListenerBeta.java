package com.luminous.financetracker.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.luminous.financetracker.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.regex.Pattern;

import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.R;

public class NotificationListenerBeta extends NotificationListenerService {

    private static final String TAG = "NotificationBeta";
    private static final String CSV_FILE_NAME = "ml_training_data.csv";

    private com.luminous.financetracker.repository.TransactionRepository repository;

    // Broad trigger to catch ANYTHING involving Malaysian Ringgit
    private static final Pattern RM_PATTERN = Pattern.compile(
            "\\b(?:rm|myr)\\s*(\\d+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    // Reuse your existing patterns to pre-label the dataset!
    private static final Pattern PROMO_PATTERN = Pattern.compile(
            "\\b(to get|to win|min(imum)? spend(t)?|up to|win rm|t&c|terms( and | & )conditions|promo(tions?)?|expir(e|ing|y)|cashback|voucher|survey|reward|redeem|discount|% off|deal|limited time|pay later)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CASH_IN_PATTERN = Pattern.compile(
            "\\b(credited|ka-ching|refund|top up|cash in|money received)\\b|" +
                    "\\b(received?)\\b.{1,30}\\b(from|into|to)\\b|" +
                    "\\bhas\\s+transferred\\b.{1,30}\\bto\\s+you\\b|" +
                    "\\bwas\\s+transferred\\b.{1,30}\\bto\\s+you\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPENSE_PATTERN = Pattern.compile(
            "\\b(spend|spent|payment|paid|deducted|debited|not you|charged|transaction)\\b|" +
                    "\\b(you( have)?( successfully)? transferred|payment|paid|successful.*transfer|your transfer)\\b.{1,50}\\bto\\b|" +
                    "\\btransfer.*successful\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onCreate() {
        super.onCreate();
        // This spins up the database connection when the service starts
        repository = new com.luminous.financetracker.repository.TransactionRepository(getApplication());
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d("NotificationBeta", "HEARTBEAT: Service is alive and saw a notification!");
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        boolean isLoggingEnabled = prefs.getBoolean("allow_logging_financial_details", false);

        if (!isLoggingEnabled) {
            return; // Exit early if the toggle is off
        }

        String packageName = sbn.getPackageName();
        if (packageName != null && (
                packageName.contains("whatsapp") ||
                        packageName.contains("telegram") ||
                        packageName.contains("instagram") ||
                        packageName.contains("facebook") ||
                        packageName.contains("twitter") ||
                        packageName.contains("discord"))) {
            return; // Ignore social media completely
        }

        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(android.app.Notification.EXTRA_TITLE);
        String body = extras.getString(android.app.Notification.EXTRA_TEXT);
        String bigText = extras.getString(android.app.Notification.EXTRA_BIG_TEXT);

        String text = (title != null ? title + " " : "") +
                (body != null ? body + " " : "") +
                (bigText != null ? bigText : "");

        if (text.trim().isEmpty()) return;

        String lowerText = text.toLowerCase();

        // Only log if it contains RM or MYR
        if (RM_PATTERN.matcher(lowerText).find()) {

            // Clean up the text for CSV compatibility (remove newlines and internal quotes)
            String cleanText = text.replaceAll("[\\r\\n]+", " ").replace("\"", "\"\"");
            String category = determineCategory(lowerText);

            writeToCsv(cleanText, category);

            // --- REFACTORED: DATABASE INSERTION & SMART NOTIFICATION ---
            if ("EXPENSE".equals(category)) {
                java.util.regex.Pattern extractAmountPattern = java.util.regex.Pattern.compile("\\b(?:rm|myr)\\s*(\\d+(?:\\.\\d{1,2})?)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher amountMatcher = extractAmountPattern.matcher(lowerText);

                double amount = 0.0;

                // FIX 1: Removed the double 'find()' trap! Only use the while loop.
                while (amountMatcher.find()) {
                    try {
                        double parsedAmount = Double.parseDouble(amountMatcher.group(1));
                        if (parsedAmount > 0) {
                            amount = parsedAmount;
                            break; // Found a valid > 0 amount, exit loop!
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Failed to parse amount digits, skipping...", e);
                    }
                }

                if (amount > 0) {
                    // FIX 2: Variables used inside a background thread lambda must be "effectively final"
                    final double finalAmount = amount;

                    // 1. Create the Transaction object
                    com.luminous.financetracker.data.entity.Transaction newTransaction = new com.luminous.financetracker.data.entity.Transaction(
                            finalAmount,
                            title != null ? title : "Auto-Logged Expense",
                            Constants.CATEGORY_OTHERS,
                            System.currentTimeMillis()
                    );

                    // 2. Insert into the database
                    repository.insert(newTransaction, () -> {
                        Log.d(TAG, "Successfully saved RM" + finalAmount + " to Dashboard Database!");

                        // 3. Fire the clickable push notification
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        PendingIntent pendingIntent = PendingIntent.getActivity(
                                this,
                                (int) System.currentTimeMillis(),
                                intent,
                                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_BUDGET_ALERTS)
                                .setSmallIcon(R.mipmap.meowneytrack_beta)
                                .setContentTitle("Meowney Track Beta")
                                .setContentText("RM " + finalAmount + " saved to Dashboard")
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                        }
                    });

                    return; // Successfully processed the expense, exit the method here!
                }
            }
        }
    }

    private String determineCategory(String lowerText) {
        // Auto-label the data so you don't have to do it manually later
        if (PROMO_PATTERN.matcher(lowerText).find()) return "JUNK";
        if (CASH_IN_PATTERN.matcher(lowerText).find()) return "CASH_IN";
        if (EXPENSE_PATTERN.matcher(lowerText).find()) return "EXPENSE";

        // The most valuable data for your ML model: The edge cases your regex missed!
        return "UNKNOWN";
    }

    private void writeToCsv(String text, String category) {
        // Saves to your app's private external files directory
        File file = new File(getExternalFilesDir(null), CSV_FILE_NAME);
        boolean isNewFile = !file.exists();

        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {

            // Add CSV header if creating the file for the first time
            if (isNewFile) {
                writer.append("Text,Category\n");
            }

            // Wrap text in quotes to safely handle any commas inside the notification text
            writer.append("\"").append(text).append("\",").append(category).append("\n");
            Log.d(TAG, "Logged ML Data: " + category);

        } catch (IOException e) {
            Log.e(TAG, "Failed to write to ML dataset CSV", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Not needed for dataset collection
    }
}