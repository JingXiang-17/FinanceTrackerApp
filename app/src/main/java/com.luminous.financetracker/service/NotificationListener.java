package com.luminous.financetracker.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.repository.TransactionRepository;
import com.luminous.financetracker.util.BudgetAlertManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // 1 : Get and filter the package names of a notification
        String packageName = sbn.getPackageName();

        // Define the financial apps that matters
        List<String> targetApps = Arrays.asList(
                //ewallets / online shopping
                "my.com.tngdigital.ewallet", // Touch 'n Go eWallet
                "com.shopee.my", // Shopee / ShopeePay
                "com.lazada.android", // Lazada
                "com.grabtaxi.passenger", // Grab / GrabPay
                "com.einnovation.temu", // Temu
                "com.tencent.mm", // Wechat / Wechat Pay
                "com.eg.android.AlipayGphone", // Alipay
                "my.com.myboost", // Boost ewallet
                //banks
                "com.hongleongconnect.mobileconnect", // Hong Leong Bank
                "com.rhbgroup.rhbmobilebanking", // RHB Bank
                "com.irakyatmob.bkrm", // Bank Rakyat
                "com.maybank2u.life", // MAE by Maybank
                "my.rytbank.app", // Ryt Bank
                "com.cimbocto", //CIMB Bank
                "com.ocbc.mobilemy" //OCBC Bank
        );

        // If the notification is NOT from your target list, drop it immediately
        if (packageName == null || !targetApps.contains(packageName)) {
            return;
        }

        // 2 : Get and filter the notification text and title
        String text = sbn.getNotification().extras.getString(android.app.Notification.EXTRA_TEXT);
        String title = sbn.getNotification().extras.getString(android.app.Notification.EXTRA_TITLE); // Get Title for Merchant

        // If it's a silent notification with no text, drop it
        if (text == null || text.isEmpty()) {
            return;
        }

        // 3 : Extract the amount from 'text' using Regex
        String lowercase = text.toLowerCase();
        if (lowercase.contains("transferred to you") || lowercase.contains("received")) { //mainly to address transferred keyword from tng notification
            return;
        }

        boolean isPromotion = (lowercase.contains("to get") || (lowercase.contains("to win")) || lowercase.contains("min spend") || lowercase.contains("min spent") || lowercase.contains("minimum spend") ||
                lowercase.contains ("minimum spent") || lowercase.contains ("up to") || lowercase.contains ("win rm") || lowercase.contains ("terms and conditions") || lowercase.contains ("t&c") || lowercase.contains ("terms & conditions") ||
                lowercase.contains ("t & c") || lowercase.contains ("promo") || lowercase.contains ("expiring") || lowercase.contains ("expire") || lowercase.contains ("cashback") ||
                lowercase.contains ("voucher") || lowercase.contains ("survey"));

        boolean isExpense = (lowercase.contains("spend") || lowercase.contains("paid") || lowercase.contains("deducted") || lowercase.contains("payment") || lowercase.contains("transferred") || lowercase.contains("spent"));

        if (isPromotion) {
            return;
        }
        if (!isExpense) { //this is cash in.
            if (lowercase.contains("receive") || lowercase.contains("credit") || lowercase.contains("top up") || lowercase.contains("ka-ching") || lowercase.contains("refund")) {
                return;
            }
            // not expense or income -- promotion or spam. drop this.
            return;
        }

        Pattern pattern = Pattern.compile("rm\\s?(\\d+\\.\\d{2})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            try {
                String amountString = matcher.group(1);
                double amount = Double.parseDouble(amountString);
                long currentTimestamp = System.currentTimeMillis();

                // --- DATA EXTRACTION ENGINE ---
                String category = determineCategory(lowercase);
                String paymentMethod = determinePaymentMethod(packageName); // The "From"
                String merchantName = determineMerchant(lowercase, title); // The "To"

                // 4: Construct the Transaction entity using the NEW 7-parameter constructor
                // Format: amount, text, category, timestamp, paymentMethod, merchantName, notes
                Transaction newTransaction = new Transaction(
                        amount,
                        text,
                        category,
                        currentTimestamp,
                        paymentMethod,
                        merchantName,
                        "" // Leave notes empty for auto-captured transactions
                );

                // 5: Dispatch to the repository to save asynchronously
                TransactionRepository repository = new TransactionRepository(getApplication());
                repository.insert(newTransaction);

                // 6: Trigger the Budget Alerts check
                FinanceDatabase db = FinanceDatabase.getDatabase(getApplicationContext());
                BudgetAlertManager.checkBudgets(getApplicationContext(), db.transactionDao());

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    // --- HELPER METHODS ---

    private String determinePaymentMethod(String packageName) {
        switch (packageName) {
            case "my.com.tngdigital.ewallet": return "Touch 'n Go eWallet";
            case "com.shopee.my": return "ShopeePay";
            case "com.lazada.android": return "Lazada Wallet";
            case "com.grabtaxi.passenger": return "GrabPay";
            case "com.einnovation.temu": return "Temu";
            case "com.tencent.mm": return "WeChat Pay";
            case "com.eg.android.AlipayGphone": return "Alipay";
            case "my.com.myboost": return "Boost";
            case "com.hongleongconnect.mobileconnect": return "Hong Leong Bank";
            case "com.rhbgroup.rhbmobilebanking": return "RHB Bank";
            case "com.irakyatmob.bkrm": return "Bank Rakyat";
            case "com.maybank2u.life": return "MAE";
            case "my.rytbank.app": return "Ryt Bank";
            case "com.cimbocto": return "CIMB Bank";
            case "com.ocbc.mobilemy": return "OCBC Bank";
            default: return "Unknown App";
        }
    }

    private String determineMerchant(String lowercaseText, String notificationTitle) {
        // 1. Try to find a known merchant keyword in the text first
        if (lowercaseText.contains("kfc")) return "KFC";
        if (lowercaseText.contains("luck bros kopi")) return "Luck Bros Kopi";
        if (lowercaseText.contains("sushi village")) return "Sushi Village";
        if (lowercaseText.contains("uni ramen")) return "Uni Ramen";
        if (lowercaseText.contains("taiwan tea house")) return "Taiwan Tea House";
        if (lowercaseText.contains("emart24")) return "emart24";
        if (lowercaseText.contains("luckin coffee")) return "Luckin Coffee";
        if (lowercaseText.contains("gigi coffee")) return "Gigi Coffee";
        if (lowercaseText.contains("koppiku")) return "Koppiku";
        if (lowercaseText.contains("tealive")) return "Tealive";
        if (lowercaseText.contains("zus coffee")) return "ZUS Coffee";
        if (lowercaseText.contains("water bar")) return "Water Bar";
        if (lowercaseText.contains("golden screen cinemas") || lowercaseText.contains("gsc")) return "Golden Screen Cinemas";
        if (lowercaseText.contains("legoland")) return "Legoland";
        if (lowercaseText.contains("ktm")) return "KTM";
        if (lowercaseText.contains("airasia")) return "AirAsia";
        if (lowercaseText.contains("malaysia airlines")) return "Malaysia Airlines";

        // 2. Fallback: Often, the title of the notification IS the merchant name (e.g. "Payment to Starbucks")
        if (notificationTitle != null && !notificationTitle.isEmpty()) {
            return notificationTitle;
        }

        // 3. Absolute fallback
        return "Unknown Merchant";
    }

    private String determineCategory(String lowercaseText) {
        Map<String, String> keywordMap = new HashMap<>();

        // Food & Beverages (Merged)
        keywordMap.put("kfc", "Food & Beverages");
        keywordMap.put("luck bros kopi", "Food & Beverages");
        keywordMap.put("sushi village", "Food & Beverages");
        keywordMap.put("uni ramen", "Food & Beverages");
        keywordMap.put("taiwan tea house", "Food & Beverages");
        keywordMap.put("emart24", "Food & Beverages");
        keywordMap.put("luckin coffee", "Food & Beverages");
        keywordMap.put("gigi coffee", "Food & Beverages");
        keywordMap.put("koppiku", "Food & Beverages");
        keywordMap.put("tealive", "Food & Beverages");
        keywordMap.put("zus coffee", "Food & Beverages");
        keywordMap.put("water bar", "Food & Beverages");

        // Entertainment
        keywordMap.put("golden screen cinemas", "Entertainment");
        keywordMap.put("legoland", "Entertainment");

        // Transport
        keywordMap.put("ktm", "Transport");
        keywordMap.put("airasia", "Transport");
        keywordMap.put("malaysia airlines", "Transport");

        // Scan the notification for matches
        for (Map.Entry<String, String> entry : keywordMap.entrySet()) {
            if (lowercaseText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Fallback for everything else
        return "Others";
    }
}