package com.luminous.financetracker.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.CategoryAdapter;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.util.BudgetAlertManager;
import com.luminous.financetracker.util.TimeUtils;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize SharedPreferences for Budget Limits
        sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);
        float monthlyLimit = sharedPreferences.getFloat("limit_2", 1000.0f);
        float dailyLimit = sharedPreferences.getFloat("limit_0", 30.0f);

        // 2. Link UI Elements
        TextView tvTotalBalance = findViewById(R.id.tv_total_balance);
        TextView tvBudgetTitle = findViewById(R.id.tv_budget_title);
        TextView tvBudgetSpent = findViewById(R.id.tv_budget_spent);
        TextView tvBudgetTotal = findViewById(R.id.tv_budget_total);
        TextView tvBudgetPercent = findViewById(R.id.tv_budget_percent);
        ProgressBar progressBudget = findViewById(R.id.progress_budget);
        TextView tvBudgetDaily = findViewById(R.id.tv_budget_daily);
        TextView tvBudgetDaysLeft = findViewById(R.id.tv_budget_days_left);

        // Link Shimmer and Empty State Elements
        ShimmerFrameLayout shimmerContainer = findViewById(R.id.shimmer_view_container);
        View realContentLayout = findViewById(R.id.real_content_layout);
        TextView tvEmptyTransactions = findViewById(R.id.tv_empty_transactions);

        // 3. Setup Date/Time Math
        Calendar cal = Calendar.getInstance();
        String currentMonthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int daysLeft = maxDays - currentDay;

        tvBudgetTitle.setText(currentMonthName + " budget");
        tvBudgetTotal.setText(String.format("/ RM %.2f", monthlyLimit));
        tvBudgetDaily.setText(String.format("Daily budget - RM %.2f", dailyLimit));
        tvBudgetDaysLeft.setText(daysLeft + " days left");

        // 4. Setup Categories RecyclerView
        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        CategoryAdapter categoryAdapter = new CategoryAdapter();
        rvCategories.setAdapter(categoryAdapter);

        // 5. Setup Transactions RecyclerView
        RecyclerView rvTransactions = findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        TransactionAdapter transactionAdapter = new TransactionAdapter();
        rvTransactions.setAdapter(transactionAdapter);

        transactionAdapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Transaction transaction) { showEditTransactionDialog(transaction); }
            @Override
            public void onDeleteClick(Transaction transaction) { transactionViewModel.delete(transaction); }
        });

        // 6. Initialize ViewModel & Observe Data
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // A. Observe Monthly Total for the Budget Card
        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfMonth()).observe(this, monthlyTotal -> {
            double spentThisMonth = (monthlyTotal != null) ? monthlyTotal : 0.0;

            tvTotalBalance.setText(String.format("RM %.2f", spentThisMonth));
            tvBudgetSpent.setText(String.format("RM %.2f", spentThisMonth));

            int percent = (int) ((spentThisMonth / monthlyLimit) * 100);
            progressBudget.setProgress(percent);
            tvBudgetPercent.setText(percent + "%");
        });

        // B. Observe All Transactions for Categories, Recent List, and Loading States
        transactionViewModel.getAllTransactions().observe(this, transactions -> {

            // Stop Shimmer and show Real Content
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
            realContentLayout.setVisibility(View.VISIBLE);

            // Handle Empty State Phase
            if (transactions == null || transactions.isEmpty()) {
                tvEmptyTransactions.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);

                // --- CHANGED: Now using LinkedHashMap ---
                Map<String, Double> emptyCategories = new java.util.LinkedHashMap<>();
                emptyCategories.put("Food & Beverages", 0.0);
                emptyCategories.put("Transport", 0.0);
                emptyCategories.put("Entertainment", 0.0);
                emptyCategories.put("Others", 0.0);
                categoryAdapter.setCategories(emptyCategories);

                transactionAdapter.submitList(new ArrayList<>()); // Clear list
                return; // Exit early since there is no data to process
            }

            // POPULATED STATE
            tvEmptyTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);

            transactionAdapter.submitList(transactions); // Send to main list

            // --- CHANGED: Pre-fill with LinkedHashMap so 0.0 categories don't disappear ---
            Map<String, Double> categoryTotals = new java.util.LinkedHashMap<>();
            categoryTotals.put("Food & Beverages", 0.0);
            categoryTotals.put("Transport", 0.0);
            categoryTotals.put("Entertainment", 0.0);
            categoryTotals.put("Others", 0.0);

            long startOfMonth = TimeUtils.getStartOfMonth();

            for (Transaction t : transactions) {
                if (t.getTimestamp() >= startOfMonth) {
                    categoryTotals.put(t.getCategory(), categoryTotals.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
                }
            }
            categoryAdapter.setCategories(categoryTotals);
        });

        // 7. Manual Add Button
        findViewById(R.id.btn_add).setOnClickListener(v -> showManualAddDialog());

        // 8. "See All" Transactions Button
        findViewById(R.id.tv_transactions_see_all).setOnClickListener(v -> {
            showSeeAllDialog(transactionAdapter.getCurrentList());
        });

        // 9. Bottom Navigation Logic
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_stats) {
                startActivity(new Intent(getApplicationContext(), StatisticsActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            }
            return itemId == R.id.nav_home;
        });

        // 10. Check Notification Permissions
        promptForNotificationAccess();
    }

    private boolean isNotificationServiceEnabled() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
    }

    private void promptForNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Auto-Tracking")
                    .setMessage("To automatically track your expenses from bank and e-wallet notifications, please grant Notification Access to the app.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    // --- DIALOGS ---

    private void showManualAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Transaction Name (e.g., Lunch)");
        layout.addView(titleInput);

        final EditText amountInput = new EditText(this);
        amountInput.setHint("Amount (e.g., 15.50)");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        final EditText paymentInput = new EditText(this);
        paymentInput.setHint("From (Payment Method: TnG, Bank, etc.)");
        layout.addView(paymentInput);

        final EditText merchantInput = new EditText(this);
        merchantInput.setHint("To (Merchant Name)");
        layout.addView(merchantInput);

        final Spinner categorySpinner = new Spinner(this);
        String[] categories = {"Food & Beverages", "Transport", "Entertainment", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);
        layout.addView(categorySpinner);

        final EditText notesInput = new EditText(this);
        notesInput.setHint("Notes (Optional)");
        layout.addView(notesInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Transaction")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String amountStr = amountInput.getText().toString().trim();

                    if (!title.isEmpty() && !amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        String category = categorySpinner.getSelectedItem().toString();
                        long timestamp = System.currentTimeMillis();

                        Transaction newTransaction = new Transaction(amount, title, category, timestamp);

                        newTransaction.setPaymentMethod(paymentInput.getText().toString().trim());
                        newTransaction.setMerchantName(merchantInput.getText().toString().trim());
                        newTransaction.setNotes(notesInput.getText().toString().trim());

                        transactionViewModel.insert(newTransaction);

                        BudgetAlertManager.checkBudgets(getApplicationContext(), FinanceDatabase.getDatabase(getApplicationContext()).transactionDao());
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void showSeeAllDialog(List<Transaction> allTransactions) {
        RecyclerView popupRecyclerView = new RecyclerView(this);
        popupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        popupRecyclerView.setPadding(24, 24, 24, 24);

        TransactionAdapter popupAdapter = new TransactionAdapter();
        popupAdapter.submitList(allTransactions);
        popupRecyclerView.setAdapter(popupAdapter);

        popupAdapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Transaction transaction) { showEditTransactionDialog(transaction); }
            @Override
            public void onDeleteClick(Transaction transaction) { transactionViewModel.delete(transaction); }
        });

        new AlertDialog.Builder(this)
                .setTitle("All Transactions")
                .setView(popupRecyclerView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEditTransactionDialog(Transaction transaction) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleInput = new EditText(this);
        titleInput.setText(transaction.getText());
        layout.addView(titleInput);

        final EditText amountInput = new EditText(this);
        amountInput.setText(String.valueOf(transaction.getAmount()));
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        final EditText paymentInput = new EditText(this);
        paymentInput.setText(transaction.getPaymentMethod());
        paymentInput.setHint("From (Payment Method: TnG, Bank, etc.)");
        layout.addView(paymentInput);

        final EditText merchantInput = new EditText(this);
        merchantInput.setText(transaction.getMerchantName());
        merchantInput.setHint("To (Merchant Name)");
        layout.addView(merchantInput);

        final Spinner categorySpinner = new Spinner(this);
        String[] categories = {"Food & Beverages", "Transport", "Entertainment", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(transaction.getCategory())) {
                categorySpinner.setSelection(i);
                break;
            }
        }
        layout.addView(categorySpinner);

        final EditText notesInput = new EditText(this);
        notesInput.setText(transaction.getNotes());
        notesInput.setHint("Notes (Optional)");
        layout.addView(notesInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Transaction")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String amountStr = amountInput.getText().toString().trim();

                    if (!title.isEmpty() && !amountStr.isEmpty()) {
                        transaction.setText(title);
                        transaction.setAmount(Double.parseDouble(amountStr));
                        transaction.setMerchantName(merchantInput.getText().toString().trim());
                        transaction.setPaymentMethod(paymentInput.getText().toString().trim());
                        transaction.setCategory(categorySpinner.getSelectedItem().toString());
                        transaction.setNotes(notesInput.getText().toString().trim());

                        transactionViewModel.update(transaction);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }
}