package com.luminous.financetracker.service;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.repository.TransactionRepository;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationListener extends NotificationListenerService {

    // This method is required. The Android OS will trigger it
    // automatically every time any notification hits the phone.
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

        // 2 : Get and filter the notification text
        // We use the official Android constant for safety
        String text = sbn.getNotification().extras.getString(android.app.Notification.EXTRA_TEXT);

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
                lowercase.contains ("t & c") || lowercase.contains ("promo"));
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

                // 4: Construct the Transaction entity
                long currentTimestamp = System.currentTimeMillis();
                Transaction newTransaction = new Transaction(amount, text, currentTimestamp);

                // 5: Dispatch to the repository to save asynchronously
                TransactionRepository repository = new TransactionRepository(getApplication());
                repository.insert(newTransaction);

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    // This method is also required. It triggers when a user swipes a notification away.
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }
}