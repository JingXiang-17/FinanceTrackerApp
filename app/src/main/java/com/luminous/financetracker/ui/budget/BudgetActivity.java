package com.luminous.financetracker.ui.budget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.ui.adapter.TimeBudgetAdapter;
import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.util.TimeUtils;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

public class BudgetActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private TimeBudgetAdapter timeBudgetAdapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        // 1. Initialize SharedPreferences to store the target limits
        sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);

        // 2. Setup the RecyclerView and Adapter
        RecyclerView rvTimeBudgets = findViewById(R.id.rv_time_budgets);
        rvTimeBudgets.setLayoutManager(new LinearLayoutManager(this));

        timeBudgetAdapter = new TimeBudgetAdapter();
        rvTimeBudgets.setAdapter(timeBudgetAdapter);

        // Load saved limits (Default: 30 Daily, 200 Weekly, 1000 Monthly)
        timeBudgetAdapter.updateLimit(0, sharedPreferences.getFloat("limit_0", 30.0f));
        timeBudgetAdapter.updateLimit(1, sharedPreferences.getFloat("limit_1", 200.0f));
        timeBudgetAdapter.updateLimit(2, sharedPreferences.getFloat("limit_2", 1000.0f));

        // 3. Handle Edit Clicks - Wired to the NEW auto-calc dialog
        timeBudgetAdapter.setOnBudgetEditListener((position, title, currentLimit) -> {
            showEditBudgetDialog(position, title, currentLimit);
        });

        // 4. Initialize ViewModel and observe LiveData
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Observe Daily (Position 0)
        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfDay()).observe(this, dailyTotal -> {
            double spentToday = (dailyTotal != null) ? dailyTotal : 0.0;
            timeBudgetAdapter.updateSpentAmount(0, spentToday);
        });

        // Observe Weekly (Position 1)
        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfWeek()).observe(this, weeklyTotal -> {
            double spentThisWeek = (weeklyTotal != null) ? weeklyTotal : 0.0;
            timeBudgetAdapter.updateSpentAmount(1, spentThisWeek);
        });

        // Observe Monthly (Position 2)
        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfMonth()).observe(this, monthlyTotal -> {
            double spentThisMonth = (monthlyTotal != null) ? monthlyTotal : 0.0;
            timeBudgetAdapter.updateSpentAmount(2, spentThisMonth);
        });

        // 5. Sticky Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_budget);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_stats) {
                startActivity(new Intent(getApplicationContext(), StatisticsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return itemId == R.id.nav_budget;
        });
    }

    // --- Helper Method to Edit Limits (Now with Auto-Calc Logic) ---
    private void showEditBudgetDialog(int position, String title, double currentLimit) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setText(String.valueOf(currentLimit));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(input);

        // Auto-Calculate Checkbox
        final CheckBox autoCalcCheckBox = new CheckBox(this);
        autoCalcCheckBox.setText("Auto-calculate related budgets");

        // Remember the user's preference for this checkbox (default to true)
        boolean isAutoCalc = sharedPreferences.getBoolean("auto_calc_budgets", true);
        autoCalcCheckBox.setChecked(isAutoCalc);
        layout.addView(autoCalcCheckBox);

        new AlertDialog.Builder(this) // Uses androidx.appcompat.app.AlertDialog
                .setTitle("Edit " + title)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String valueStr = input.getText().toString().trim();
                    if (!valueStr.isEmpty()) {
                        float newLimit = Float.parseFloat(valueStr);
                        boolean autoCalc = autoCalcCheckBox.isChecked();

                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        // Save the checkbox state so the app remembers their choice next time
                        editor.putBoolean("auto_calc_budgets", autoCalc);

                        if (autoCalc) {
                            // Calculate based on which budget they clicked
                            if (position == 0) {
                                // Edited Daily
                                editor.putFloat("limit_0", newLimit);
                                editor.putFloat("limit_1", newLimit * 7);
                                editor.putFloat("limit_2", newLimit * 28);
                            } else if (position == 1) {
                                // Edited Weekly
                                editor.putFloat("limit_0", newLimit / 7);
                                editor.putFloat("limit_1", newLimit);
                                editor.putFloat("limit_2", newLimit * 4);
                            } else if (position == 2) {
                                // Edited Monthly
                                editor.putFloat("limit_0", newLimit / 28);
                                editor.putFloat("limit_1", newLimit / 4);
                                editor.putFloat("limit_2", newLimit);
                            }
                        } else {
                            // Auto-calc is OFF: Only update the specific budget they edited
                            editor.putFloat("limit_" + position, newLimit);
                        }

                        editor.apply();

                        // Refresh the UI instantly for all three budgets
                        timeBudgetAdapter.updateLimit(0, sharedPreferences.getFloat("limit_0", 30.0f));
                        timeBudgetAdapter.updateLimit(1, sharedPreferences.getFloat("limit_1", 200.0f));
                        timeBudgetAdapter.updateLimit(2, sharedPreferences.getFloat("limit_2", 1000.0f));
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }
}