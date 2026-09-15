package com.luminous.financetracker.ui.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.CategoryAdapter;
import com.luminous.financetracker.ui.adapter.MonthAdapter;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.util.Constants;
import com.luminous.financetracker.util.TimeUtils;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.ImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TransactionViewModel transactionViewModel;
    private SharedPreferences sharedPreferences;

    private TransactionAdapter transactionAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Transaction> allCachedTransactions = new ArrayList<>();
    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private int selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH);

    // UI Elements
    private TextView tvTotalBalance, tvBudgetTitle, tvBudgetSpent, tvBudgetTotal, tvBudgetPercent, tvBudgetDaily,
            tvBudgetDaysLeft, tvEmptyTransactions, tvFixedAmount;
    private ProgressBar progressBudget;
    private ShimmerFrameLayout shimmerContainer;
    private View realContentLayout;
    private RecyclerView rvTransactions;
    private ImageView ivProfileImage;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications enabled successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Budget alerts will remain muted without permission.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

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
        ivProfileImage = findViewById(R.id.iv_profile_image);
        tvFixedAmount = findViewById(R.id.tv_fixed_spending_amount);

        String savedImagePath = sharedPreferences.getString(Constants.KEY_PROFILE_PIC_PATH, null);
        if (savedImagePath != null) {
            File imgFile = new File(savedImagePath);
            if (imgFile.exists()) {
                ivProfileImage.setImageURI(Uri.fromFile(imgFile));
            }
        }

        findViewById(R.id.profile_image_card).setOnClickListener(v -> pickProfileImage.launch("image/*"));

        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryAdapter = new CategoryAdapter();
        rvCategories.setAdapter(categoryAdapter);

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

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    com.luminous.financetracker.data.database.FinanceDatabase db = com.luminous.financetracker.data.database.FinanceDatabase.getDatabase(DashboardActivity.this);
                    com.luminous.financetracker.util.BudgetAlertManager.checkBudgets(DashboardActivity.this, db.transactionDao());
                }, 500);
            }
        });

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Call the unified method from the adapter!
                transactionAdapter.confirmDeletion(position, DashboardActivity.this, () -> {
                    // This Runnable executes if the user cancels, bouncing the item back
                    transactionAdapter.notifyItemChanged(position);
                });
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvTransactions);

        RecyclerView rvMonths = findViewById(R.id.rv_dashboard_months);
        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        MonthAdapter monthAdapter = new MonthAdapter((year, monthIndex) -> {
            selectedYear = year;
            selectedMonthIndex = monthIndex;
            refreshDashboardUI(selectedYear);
        });
        rvMonths.setAdapter(monthAdapter);
        rvMonths.scrollToPosition(selectedMonthIndex);

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        transactionViewModel.getAllTransactions().observe(this, transactions -> {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
            realContentLayout.setVisibility(View.VISIBLE);

            if (transactions != null) {
                allCachedTransactions = transactions;
                refreshDashboardUI(selectedYear);
            }
        });

        findViewById(R.id.btn_add).setOnClickListener(v -> showManualAddDialog());
        findViewById(R.id.tv_transactions_see_all).setOnClickListener(v -> showSeeAllDialog(allCachedTransactions));

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
        checkAndRequestNotificationPermission();
    }

    private void refreshDashboardUI(int targetYear) {
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // FIX #1: Cross-Year Boundary Logic
        // If the selected month index is greater than the current month in a rolling picker, it mathematically belongs to last year.

        long startOfMonth = TimeUtils.getStartOfMonthIndex(targetYear, selectedMonthIndex);
        long endOfMonth = TimeUtils.getEndOfMonthIndex(targetYear, selectedMonthIndex);
        long startOfDay = TimeUtils.getStartOfDay();

        List<Transaction> monthList = new ArrayList<>();
        List<Transaction> todayList = new ArrayList<>();

        double todayTotalSpent = 0.0;
        double monthTotalSpent = 0.0;
        double monthBudgetSpent = 0.0;
        double fixedMonthSpent = 0.0;

        Map<String, Double> categoryTotals = new java.util.LinkedHashMap<>();
        categoryTotals.put(Constants.CATEGORY_DINING, 0.0);
        categoryTotals.put(Constants.CATEGORY_TRANSPORT, 0.0);
        categoryTotals.put(Constants.CATEGORY_ENTERTAINMENT, 0.0);
        categoryTotals.put(Constants.CATEGORY_SHOPPING, 0.0);
        categoryTotals.put(Constants.CATEGORY_OTHERS, 0.0);

        for (Transaction t : allCachedTransactions) {
            if (t.getTimestamp() >= startOfMonth && t.getTimestamp() <= endOfMonth) {
                monthList.add(t);
                monthTotalSpent += t.getAmount();

                if (Constants.CATEGORY_FIXED.equalsIgnoreCase(t.getCategory())) {
                    fixedMonthSpent += t.getAmount();
                } else {
                    monthBudgetSpent += t.getAmount();

                    // FIX #2: Defensive Category Fallback
                    String cat = t.getCategory();
                    if (!categoryTotals.containsKey(cat)) {
                        cat = Constants.CATEGORY_OTHERS; // Force unknown categories into "Others"
                    }
                    categoryTotals.put(cat, categoryTotals.get(cat) + t.getAmount());
                }
            }

            // Ensure today's list strictly pulls from today's actual date, regardless of month selected
            if (t.getTimestamp() >= startOfDay) {
                todayList.add(t);
                todayTotalSpent += t.getAmount();
            }
        }

        if (todayList.isEmpty()) {
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmptyTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
        }
        todayList.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
        transactionAdapter.submitList(todayList);
        categoryAdapter.setCategories(categoryTotals);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, selectedMonthIndex);
        tvBudgetTitle.setText(cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " budget");

        float monthlyLimit = sharedPreferences.getFloat(Constants.KEY_LIMIT_1, 1000.0f);
        float dailyLimit = sharedPreferences.getFloat(Constants.KEY_LIMIT_0, 30.0f);

        tvTotalBalance.setText(String.format("RM %.2f", todayTotalSpent));
        tvBudgetSpent.setText(String.format("RM %.2f", monthBudgetSpent));
        tvBudgetTotal.setText(String.format("/ RM %.2f", monthlyLimit));
        tvBudgetDaily.setText(String.format("Daily budget - RM %.2f", dailyLimit));

        tvFixedAmount.setText(String.format("RM %.2f", fixedMonthSpent));

        int percent = (monthlyLimit > 0) ? (int) ((monthBudgetSpent / monthlyLimit) * 100) : 0;
        progressBudget.setProgress(percent);
        tvBudgetPercent.setText(percent + "%");

        // Verify both month AND year match before showing "days left"
        if (selectedMonthIndex == currentMonth && targetYear == currentYear) {
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentDay = cal.get(Calendar.DAY_OF_MONTH);
            tvBudgetDaysLeft.setText((maxDays - currentDay) + " days left");
            tvBudgetDaysLeft.setVisibility(View.VISIBLE);
        } else {
            tvBudgetDaysLeft.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isNotificationServiceEnabled() {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
    }

    private void promptForNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Auto-Tracking")
                    .setMessage("To automatically track your expenses from bank and e-wallet notifications, please grant Notification Access to the app.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
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
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        final EditText paymentInput = new EditText(this);
        paymentInput.setHint("From (Payment Method: TnG, Bank, etc.)");
        layout.addView(paymentInput);

        final EditText merchantInput = new EditText(this);
        merchantInput.setHint("To (Merchant Name)");
        layout.addView(merchantInput);

        final android.widget.CheckBox cbFixed = new android.widget.CheckBox(this);
        cbFixed.setText("Mark as Fixed Expense (e.g., Rent)");
        layout.addView(cbFixed);

        final Spinner categorySpinner = new Spinner(this);
        String[] categories = {
                Constants.CATEGORY_DINING, Constants.CATEGORY_TRANSPORT,
                Constants.CATEGORY_ENTERTAINMENT, Constants.CATEGORY_SHOPPING, Constants.CATEGORY_OTHERS
        };
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
                        try {
                            // FIX #3: Graceful exception handling for unparseable numbers
                            double amount = Double.parseDouble(amountStr);
                            if (amount < 0) throw new NumberFormatException("Amount cannot be negative");

                            String category = cbFixed.isChecked() ? Constants.CATEGORY_FIXED : categorySpinner.getSelectedItem().toString();
                            long timestamp = System.currentTimeMillis();

                            Transaction newTransaction = new Transaction(amount, title, category, timestamp);
                            newTransaction.setPaymentMethod(paymentInput.getText().toString().trim());
                            newTransaction.setMerchantName(merchantInput.getText().toString().trim());
                            newTransaction.setNotes(notesInput.getText().toString().trim());

                            transactionViewModel.insert(newTransaction);

                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                com.luminous.financetracker.data.database.FinanceDatabase db = com.luminous.financetracker.data.database.FinanceDatabase.getDatabase(DashboardActivity.this);
                                com.luminous.financetracker.util.BudgetAlertManager.checkBudgets(DashboardActivity.this, db.transactionDao());
                            }, 500);

                        } catch (NumberFormatException e) {
                            Toast.makeText(DashboardActivity.this, "Please enter a valid positive amount.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DashboardActivity.this, "Title and Amount are required.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private void showSeeAllDialog(List<Transaction> allTransactions) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        final EditText searchInput = new EditText(this);
        searchInput.setHint("Search title, merchant, notes...");
        searchInput.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        searchInput.setCompoundDrawablePadding(16);
        layout.addView(searchInput);

        RecyclerView popupRecyclerView = new RecyclerView(this);
        popupRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 0);
        popupRecyclerView.setLayoutParams(params);
        layout.addView(popupRecyclerView);

        TransactionAdapter popupAdapter = new TransactionAdapter();
        popupRecyclerView.setAdapter(popupAdapter);

        // A container to hold the freshest data for the search bar to use
        List<Transaction> latestData = new ArrayList<>();

        // 1. Assign the observer to a variable so we can kill it later
        androidx.lifecycle.Observer<List<Transaction>> dialogObserver = liveTransactions -> {
            if (liveTransactions != null) {
                // Update our local cache for the search bar
                latestData.clear();
                latestData.addAll(liveTransactions);
                latestData.sort((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

                // Re-run the search filter instantly to keep the UI perfectly synced
                String currentQuery = searchInput.getText().toString().toLowerCase().trim();
                filterAndSubmitToPopup(currentQuery, latestData, popupAdapter);
            }
        };

        // Attach the observer
        transactionViewModel.getAllTransactions().observe(this, dialogObserver);

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

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Use the latestData list, NOT the old allTransactions list!
                filterAndSubmitToPopup(s.toString().toLowerCase().trim(), latestData, popupAdapter);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("All Transactions")
                .setView(layout)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                // 2. Kill the observer when the dialog closes to prevent memory leaks!
                .setOnDismissListener(dialog -> transactionViewModel.getAllTransactions().removeObserver(dialogObserver))
                .show();
    }

    // A quick helper method to keep your code clean
    private void filterAndSubmitToPopup(String query, List<Transaction> listToFilter, TransactionAdapter adapter) {
        if (query.isEmpty()) {
            adapter.submitList(new ArrayList<>(listToFilter));
            return;
        }

        List<Transaction> filteredList = new ArrayList<>();
        for (Transaction t : listToFilter) {
            String title = t.getText() != null ? t.getText().toLowerCase() : "";
            String merchant = t.getMerchantName() != null ? t.getMerchantName().toLowerCase() : "";
            String notes = t.getNotes() != null ? t.getNotes().toLowerCase() : "";
            String category = t.getCategory() != null ? t.getCategory().toLowerCase() : "";

            if (title.contains(query) || merchant.contains(query) || notes.contains(query) || category.contains(query)) {
                filteredList.add(t);
            }
        }
        adapter.submitList(filteredList);
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

        final android.widget.CheckBox cbFixed = new android.widget.CheckBox(this);
        cbFixed.setText("Mark as Fixed Expense (e.g., Rent)");
        cbFixed.setChecked(Constants.CATEGORY_FIXED.equals(transaction.getCategory()));
        layout.addView(cbFixed);

        final Spinner categorySpinner = new Spinner(this);
        String[] categories = {
                Constants.CATEGORY_DINING, Constants.CATEGORY_TRANSPORT,
                Constants.CATEGORY_ENTERTAINMENT, Constants.CATEGORY_SHOPPING, Constants.CATEGORY_OTHERS
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        if (!Constants.CATEGORY_FIXED.equals(transaction.getCategory())) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(transaction.getCategory())) {
                    categorySpinner.setSelection(i);
                    break;
                }
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
                        try {
                            double amount = Double.parseDouble(amountStr);
                            if (amount < 0) throw new NumberFormatException("Amount cannot be negative");

                            // --- THE FIX: Create a brand new object instead of mutating the old one ---
                            String category = cbFixed.isChecked() ? Constants.CATEGORY_FIXED : categorySpinner.getSelectedItem().toString();

                            Transaction updatedTransaction = new Transaction(amount, title, category, transaction.getTimestamp());

                            // Crucial: You MUST set the ID so Room knows to overwrite the existing row!
                            updatedTransaction.setId(transaction.getId());

                            updatedTransaction.setMerchantName(merchantInput.getText().toString().trim());
                            updatedTransaction.setPaymentMethod(paymentInput.getText().toString().trim());
                            updatedTransaction.setNotes(notesInput.getText().toString().trim());

                            // Send the new clone to the database
                            transactionViewModel.update(updatedTransaction);

                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                com.luminous.financetracker.data.database.FinanceDatabase db = com.luminous.financetracker.data.database.FinanceDatabase.getDatabase(DashboardActivity.this);
                                com.luminous.financetracker.util.BudgetAlertManager.checkBudgets(DashboardActivity.this, db.transactionDao());
                            }, 500);
                            // --------------------------------------------------------------------------

                        } catch (NumberFormatException e) {
                            Toast.makeText(DashboardActivity.this, "Please enter a valid positive amount.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DashboardActivity.this, "Title and Amount are required.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private final ActivityResultLauncher<String> pickProfileImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ivProfileImage.setImageURI(uri);
                    saveProfileImageToInternalStorage(uri);
                }
            }
    );

    private void saveProfileImageToInternalStorage(Uri uri) {
        // FIX #4: Try-with-resources absolutely guarantees stream closures to prevent memory leaks
        File profilePic = new File(getFilesDir(), "profile_picture.jpg");

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(profilePic)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            sharedPreferences.edit().putString(Constants.KEY_PROFILE_PIC_PATH, profilePic.getAbsolutePath()).apply();

        } catch (Exception e) {
            // FIX #4b: Replace standard printStackTrace with proper Log.e
            Log.e(TAG, "Failed to save profile picture.", e);
            Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}