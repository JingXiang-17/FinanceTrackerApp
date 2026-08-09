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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.CategoryAdapter;
import com.luminous.financetracker.ui.adapter.MonthAdapter;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.util.BudgetAlertManager;
import com.luminous.financetracker.util.TimeUtils;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private SharedPreferences sharedPreferences;

    // Promoted to class-level so our filter method can access them
    private TransactionAdapter transactionAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Transaction> allCachedTransactions = new ArrayList<>();

    // Track which month the user is currently viewing
    private int selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH);

    // UI Elements
    private TextView tvTotalBalance, tvBudgetTitle, tvBudgetSpent, tvBudgetTotal, tvBudgetPercent, tvBudgetDaily,
            tvBudgetDaysLeft, tvEmptyTransactions;
    private ProgressBar progressBudget;
    private ShimmerFrameLayout shimmerContainer;
    private View realContentLayout;
    private RecyclerView rvTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);

        // 2. Link UI Elements
        tvTotalBalance = findViewById(R.id.tv_total_balance);
        tvBudgetTitle = findViewById(R.id.tv_budget_title);
        tvBudgetSpent = findViewById(R.id.tv_budget_spent);
        tvBudgetTotal = findViewById(R.id.tv_budget_total);
        tvBudgetPercent = findViewById(R.id.tv_budget_percent);
        progressBudget = findViewById(R.id.progress_budget);
        tvBudgetDaily = findViewById(R.id.tv_budget_daily);
        tvBudgetDaysLeft = findViewById(R.id.tv_budget_days_left);
        shimmerContainer = findViewById(R.id.shimmer_view_container);
        realContentLayout = findViewById(R.id.real_content_layout);
        tvEmptyTransactions = findViewById(R.id.tv_empty_transactions);

        // 3. Setup Categories RecyclerView
        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter();
        rvCategories.setAdapter(categoryAdapter);

        // 4. Setup Transactions RecyclerView & Swipe to Delete
        rvTransactions = findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        rvTransactions.setAdapter(transactionAdapter);

        transactionAdapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Transaction transaction) {
                showEditTransactionDialog(transaction);
            }

            @Override
            public void onDeleteClick(Transaction transaction) {
                transactionViewModel.delete(transaction);
            }
        });

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Transaction transactionToDelete = transactionAdapter.getCurrentList().get(position);
                transactionViewModel.delete(transactionToDelete);
                Toast.makeText(DashboardActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvTransactions);

        // 5. Setup Horizontal Month Selector
        RecyclerView rvMonths = findViewById(R.id.rv_dashboard_months);
        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        MonthAdapter monthAdapter = new MonthAdapter(monthIndex -> {
            selectedMonthIndex = monthIndex;
            refreshDashboardUI(); // Trigger UI refresh when a new month is tapped
        });
        rvMonths.setAdapter(monthAdapter);
        rvMonths.scrollToPosition(selectedMonthIndex);

        // 6. Initialize ViewModel & Observe Data ONCE
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        transactionViewModel.getAllTransactions().observe(this, transactions -> {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
            realContentLayout.setVisibility(View.VISIBLE);

            if (transactions != null) {
                allCachedTransactions = transactions;
                refreshDashboardUI(); // Process and filter the data
            }
        });

        // 7. Button Listeners
        findViewById(R.id.btn_add).setOnClickListener(v -> showManualAddDialog());
        findViewById(R.id.tv_transactions_see_all)
                .setOnClickListener(v -> showSeeAllDialog(transactionAdapter.getCurrentList()));

        // 8. Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_stats) {
                startActivity(new Intent(getApplicationContext(), StatisticsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return itemId == R.id.nav_home;
        });

        promptForNotificationAccess();
    }

    // --- CORE FILTERING LOGIC ---
    private void refreshDashboardUI() {
        long startOfMonth = TimeUtils.getStartOfMonthIndex(selectedMonthIndex);
        long endOfMonth = TimeUtils.getEndOfMonthIndex(selectedMonthIndex);

        List<Transaction> filteredList = new ArrayList<>();
        double monthTotalSpent = 0.0;

        Map<String, Double> categoryTotals = new java.util.LinkedHashMap<>();
        categoryTotals.put("Dining", 0.0);
        categoryTotals.put("Transport", 0.0);
        categoryTotals.put("Entertainment", 0.0);
        categoryTotals.put("Shopping", 0.0);
        categoryTotals.put("Others", 0.0);

        // Filter data locally
        for (Transaction t : allCachedTransactions) {
            if (t.getTimestamp() >= startOfMonth && t.getTimestamp() <= endOfMonth) {
                filteredList.add(t);
                monthTotalSpent += t.getAmount();
                categoryTotals.put(t.getCategory(), categoryTotals.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        // Update Adapters
        if (filteredList.isEmpty()) {
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
        transactionAdapter.submitList(filteredList);
        categoryAdapter.setCategories(categoryTotals);

        // Update Budget Card UI
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, selectedMonthIndex);
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        tvBudgetTitle.setText(monthName + " budget");

        float monthlyLimit = sharedPreferences.getFloat("limit_2", 1000.0f);
        float dailyLimit = sharedPreferences.getFloat("limit_0", 30.0f);

        tvTotalBalance.setText(String.format("RM %.2f", monthTotalSpent));
        tvBudgetSpent.setText(String.format("RM %.2f", monthTotalSpent));
        tvBudgetTotal.setText(String.format("/ RM %.2f", monthlyLimit));
        tvBudgetDaily.setText(String.format("Daily budget - RM %.2f", dailyLimit));

        int percent = (monthlyLimit > 0) ? (int) ((monthTotalSpent / monthlyLimit) * 100) : 0;
        progressBudget.setProgress(percent);
        tvBudgetPercent.setText(percent + "%");

        // Days left logic (only makes sense if viewing the current real-world month)
        if (selectedMonthIndex == Calendar.getInstance().get(Calendar.MONTH)) {
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentDay = cal.get(Calendar.DAY_OF_MONTH);
            tvBudgetDaysLeft.setText((maxDays - currentDay) + " days left");
            tvBudgetDaysLeft.setVisibility(View.VISIBLE);
        } else {
            tvBudgetDaysLeft.setVisibility(View.INVISIBLE);
        }
    }

    // --- PERMISSIONS & DIALOGS ---

    private boolean isNotificationServiceEnabled() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
    }

    private void promptForNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Auto-Tracking")
                    .setMessage(
                            "To automatically track your expenses from bank and e-wallet notifications, please grant Notification Access to the app.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void showManualAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Transaction Name (e.g., Lunch)");
        layout.addView(titleInput);

        final EditText amountInput = new EditText(this);
        amountInput.setHint("Amount (e.g., 15.50)");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        final EditText paymentInput = new EditText(this);
        paymentInput.setHint("From (Payment Method: TnG, Bank, etc.)");
        layout.addView(paymentInput);

        final EditText merchantInput = new EditText(this);
        merchantInput.setHint("To (Merchant Name)");
        layout.addView(merchantInput);

        final Spinner categorySpinner = new Spinner(this);
        String[] categories = { "Dining", "Transport", "Entertainment", "Shopping", "Others" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                categories);
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
                        BudgetAlertManager.checkBudgets(getApplicationContext(),
                                FinanceDatabase.getDatabase(getApplicationContext()).transactionDao());
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void showSeeAllDialog(List<Transaction> allTransactions) {
        // 1. Create a container layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        // 2. Create the Search Bar
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Search title, merchant, notes...");
        searchInput.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        searchInput.setCompoundDrawablePadding(16);
        layout.addView(searchInput);

        // 3. Create the RecyclerView
        RecyclerView popupRecyclerView = new RecyclerView(this);
        popupRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add some margin between the search bar and the list
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 0);
        popupRecyclerView.setLayoutParams(params);

        TransactionAdapter popupAdapter = new TransactionAdapter();
        popupAdapter.submitList(allTransactions); // Load everything initially
        popupRecyclerView.setAdapter(popupAdapter);
        layout.addView(popupRecyclerView);

        // 4. Preserve Edit/Delete clicks
        popupAdapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Transaction transaction) {
                showEditTransactionDialog(transaction);
            }

            @Override
            public void onDeleteClick(Transaction transaction) {
                transactionViewModel.delete(transaction);
            }
        });

        // 5. Add the Search Logic (Filters as you type)
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                List<Transaction> filteredList = new ArrayList<>();

                for (Transaction t : allTransactions) {
                    // Prevent null pointer crashes if optional fields are empty
                    String title = t.getText() != null ? t.getText().toLowerCase() : "";
                    String merchant = t.getMerchantName() != null ? t.getMerchantName().toLowerCase() : "";
                    String notes = t.getNotes() != null ? t.getNotes().toLowerCase() : "";
                    String category = t.getCategory() != null ? t.getCategory().toLowerCase() : "";

                    // If the search query matches ANY of these fields, keep it in the list
                    if (title.contains(query) || merchant.contains(query) || notes.contains(query)
                            || category.contains(query)) {
                        filteredList.add(t);
                    }
                }

                // Animate the list to show only the filtered results
                popupAdapter.submitList(filteredList);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        // 6. Show the Dialog
        new AlertDialog.Builder(this)
                .setTitle("All Transactions")
                .setView(layout)
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
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
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
        String[] categories = { "Dining", "Transport", "Entertainment", "Shopping", "Others" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                categories);
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