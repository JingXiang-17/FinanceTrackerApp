package com.luminous.financetracker.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.LruCache;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.repository.TransactionRepository;
import com.luminous.financetracker.util.BudgetAlertManager;
import com.luminous.financetracker.util.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.R;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    // Reusable Repository instance (prevents memory churn)
    private TransactionRepository repository;

    // Deduplication Cache: Stores notification keys to prevent duplicate inserts (50 items max)
    private static final LruCache<String, Long> processedNotifications = new LruCache<>(50);

    // 1. Compile Targets using Constants.java
    private static final List<String> TARGET_APPS = Arrays.asList(
            Constants.PKG_TNG, Constants.PKG_SHOPEE, Constants.PKG_LAZADA,
            Constants.PKG_GRAB, Constants.PKG_TEMU, Constants.PKG_WECHAT,
            Constants.PKG_ALIPAY, Constants.PKG_BOOST, Constants.PKG_HLB,
            Constants.PKG_RHB, Constants.PKG_BANK_RAKYAT, Constants.PKG_MAE,
            Constants.PKG_RYT, Constants.PKG_CIMB, Constants.PKG_OCBC
    );

    // 2. Pre-compiled Regex Patterns
    private static final Pattern PRE_AUTH_PATTERN = Pattern.compile(
            "\\b(pre-authorisation|pre-authorization|hold|earmarked|deposit|temporary)\\b",
            Pattern.CASE_INSENSITIVE
    );
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

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "\\b(?:rm|myr)\\s*(\\d+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "\\b(?:to|for)\\s+([A-Za-z0-9\\s&\\*\\-]+?)(?=\\.|\\s+on\\b|\\s+not you|$)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void onCreate() {
        super.onCreate();
        // Instantiate repository ONLY ONCE
        repository = new TransactionRepository(getApplication());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (packageName == null || !TARGET_APPS.contains(packageName)) {
            return;
        }

        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(android.app.Notification.EXTRA_TITLE);
        String body = extras.getString(android.app.Notification.EXTRA_TEXT);
        String bigText = extras.getString(android.app.Notification.EXTRA_BIG_TEXT);

// Combine everything available so your regex checks the whole notification payload
        String text = (title != null ? title + " " : "") +
                (body != null ? body + " " : "") +
                (bigText != null ? bigText : "");
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // --- DEDUPLICATION GUARD ---
        // Prevents Android from processing the same notification twice if it updates
        String notificationKey = sbn.getKey();
        long currentTime = System.currentTimeMillis();
        Long lastProcessedTime = processedNotifications.get(notificationKey);

        if (lastProcessedTime != null && (currentTime - lastProcessedTime < 10000)) {
            Log.d(TAG, "Duplicate notification dropped: " + notificationKey);
            return;
        }
        processedNotifications.put(notificationKey, currentTime);

        // --- PARSING PIPELINE ---
        String lowerText = text.toLowerCase();

        if (PRE_AUTH_PATTERN.matcher(lowerText).find()) return;
        if (PROMO_PATTERN.matcher(lowerText).find()) return;
        if (CASH_IN_PATTERN.matcher(lowerText).find()) return;
        if (!EXPENSE_PATTERN.matcher(lowerText).find()) return;

        // --- REFACTORED: SKIP RM0 AND INSERT ---
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);

        double finalAmount = 0.0;

        // Loop to skip over RM0 traps
        while (amountMatcher.find()) {
            try {
                double parsedAmount = Double.parseDouble(amountMatcher.group(1));
                if (parsedAmount > 0) {
                    finalAmount = parsedAmount;
                    break;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse amount from notification: " + text, e);
            }
        }

        if (finalAmount > 0) {
            // Extract Merchant Name dynamically instead of hardcoding "Unknown"
            String merchantName = "Unknown";
            Matcher merchantMatcher = MERCHANT_PATTERN.matcher(text);
            if (merchantMatcher.find()) {
                merchantName = merchantMatcher.group(1).trim();
            }

            String paymentMethod = determinePaymentMethod(packageName);

            Transaction newTransaction = new Transaction(
                    finalAmount,
                    text,
                    Constants.CATEGORY_OTHERS,
                    currentTime,
                    paymentMethod,
                    merchantName,
                    ""
            );

            // Execute Insert WITH CALLBACK to prevent the Race Condition
            final double savedAmount = finalAmount; // Effectively final for the lambda

            repository.insert(newTransaction, () -> {
                FinanceDatabase db = FinanceDatabase.getDatabase(getApplicationContext());
                BudgetAlertManager.checkBudgets(getApplicationContext(), db.transactionDao());
                Log.d(TAG, "Transaction saved and budget checked: RM" + savedAmount);

                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                int uniqueRequestCode = (int) System.currentTimeMillis();

                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this,
                        uniqueRequestCode,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_BUDGET_ALERTS)
                        .setSmallIcon(R.mipmap.meowneytrack)
                        .setContentTitle("Meowney Track")
                        .setContentText("RM " + savedAmount + " saved to " + paymentMethod)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                }
            });
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Implementation not needed for current functionality
    }

    private String determinePaymentMethod(String packageName) {
        switch (packageName) {
            case Constants.PKG_TNG: return Constants.PAY_TNG;
            case Constants.PKG_SHOPEE: return Constants.PAY_SHOPEE;
            case Constants.PKG_LAZADA: return Constants.PAY_LAZADA;
            case Constants.PKG_GRAB: return Constants.PAY_GRAB;
            case Constants.PKG_TEMU: return Constants.PAY_TEMU;
            case Constants.PKG_WECHAT: return Constants.PAY_WECHAT;
            case Constants.PKG_ALIPAY: return Constants.PAY_ALIPAY;
            case Constants.PKG_BOOST: return Constants.PAY_BOOST;
            case Constants.PKG_HLB: return Constants.PAY_HLB;
            case Constants.PKG_RHB: return Constants.PAY_RHB;
            case Constants.PKG_BANK_RAKYAT: return Constants.PAY_BANK_RAKYAT;
            case Constants.PKG_MAE: return Constants.PAY_MAE;
            case Constants.PKG_RYT: return Constants.PAY_RYT;
            case Constants.PKG_CIMB: return Constants.PAY_CIMB;
            case Constants.PKG_OCBC: return Constants.PAY_OCBC;
            default: return Constants.PAY_UNKNOWN;
        }
    }
}