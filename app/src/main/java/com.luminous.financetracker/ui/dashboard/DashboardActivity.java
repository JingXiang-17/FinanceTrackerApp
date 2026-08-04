package com.luminous.financetracker.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Navigation Imports
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1: Link to rv_transactions
        RecyclerView recyclerView = findViewById(R.id.rv_transactions);

        // 2: Initialize your LayoutManager and attach it to the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3: Initialize the TransactionAdapter and attach it to the RecyclerView
        TransactionAdapter adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        // --- FIXED: The new two-action click listener for Edit and Delete ---
        adapter.setOnItemClickListener(new TransactionAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Transaction transaction) {
                // Launch the Edit Dialog
                showEditTransactionDialog(transaction);
            }

            @Override
            public void onDeleteClick(Transaction transaction) {
                // Delete it from Room immediately
                transactionViewModel.delete(transaction);
            }
        });
        // ------------------------------------------------------------------

        // 4: Initialize the TransactionViewModel using ViewModelProvider
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // 5: Observe the LiveData from the ViewModel
        transactionViewModel.getAllTransactions().observe(this, new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                adapter.submitList(transactions);
            }
        });

        // --- NEW: Wire up the manual Add button in your header (btn_add) ---
        View btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            showManualAddDialog();
        });
        // -------------------------------------------------------------------

        // 6: --- STICKY BOTTOM NAVIGATION LOGIC ---
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
    }

    // --- NEW: THE MANUAL ADD DIALOG ---
    private void showManualAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("Title (e.g., Groceries)");
        layout.addView(titleInput);

        final EditText amountInput = new EditText(this);
        amountInput.setHint("Amount (e.g., 25.50)");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Manual Transaction")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String titleStr = titleInput.getText().toString().trim();
                    String amountStr = amountInput.getText().toString().trim();

                    if (!titleStr.isEmpty() && !amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        long currentTimestamp = System.currentTimeMillis();

                        // Default manual entries to "Uncategorized" category
                        Transaction newTransaction = new Transaction(amount, titleStr, "Uncategorized", currentTimestamp);
                        transactionViewModel.insert(newTransaction);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    // --- UPDATED: THE EDIT ALL FIELDS DIALOG ---
    private void showEditTransactionDialog(Transaction transaction) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText titleInput = new EditText(this);
        titleInput.setText(transaction.getText());
        titleInput.setHint("Title");
        layout.addView(titleInput);

        final EditText amountInput = new EditText(this);
        amountInput.setText(String.valueOf(transaction.getAmount()));
        amountInput.setHint("Amount");
        amountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        final EditText categoryInput = new EditText(this);
        categoryInput.setText(transaction.getCategory());
        categoryInput.setHint("Category");
        layout.addView(categoryInput);

        new androidx.appcompat.app.AlertDialog.Builder(DashboardActivity.this)
                .setTitle("Edit Transaction")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newTitle = titleInput.getText().toString().trim();
                    String newAmountStr = amountInput.getText().toString().trim();
                    String newCategory = categoryInput.getText().toString().trim();

                    if (!newTitle.isEmpty() && !newAmountStr.isEmpty() && !newCategory.isEmpty()) {
                        transaction.setText(newTitle);
                        transaction.setAmount(Double.parseDouble(newAmountStr));
                        transaction.setCategory(newCategory);

                        transactionViewModel.update(transaction);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }
}