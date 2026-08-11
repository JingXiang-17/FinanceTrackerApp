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
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.ui.adapter.TimeBudgetAdapter;
import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.util.Constants;
import com.luminous.financetracker.util.TimeUtils;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private TimeBudgetAdapter timeBudgetAdapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        RecyclerView rvTimeBudgets = findViewById(R.id.rv_time_budgets);
        rvTimeBudgets.setLayoutManager(new LinearLayoutManager(this));

        timeBudgetAdapter = new TimeBudgetAdapter();
        rvTimeBudgets.setAdapter(timeBudgetAdapter);

        timeBudgetAdapter.updateLimit(0, sharedPreferences.getFloat(Constants.KEY_LIMIT_0, 30.0f));
        timeBudgetAdapter.updateLimit(1, sharedPreferences.getFloat(Constants.KEY_LIMIT_1, 1000.0f));

        timeBudgetAdapter.setOnBudgetEditListener((position, title, currentLimit) ->
                showEditBudgetDialog(position, title, currentLimit));

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfDay()).observe(this, dailyTotal -> {
            double spentToday = (dailyTotal != null) ? dailyTotal : 0.0;
            timeBudgetAdapter.updateSpentAmount(0, spentToday);
        });

        transactionViewModel.getTotalSpentSince(TimeUtils.getStartOfMonth()).observe(this, monthlyTotal -> {
            double spentThisMonth = (monthlyTotal != null) ? monthlyTotal : 0.0;
            timeBudgetAdapter.updateSpentAmount(1, spentThisMonth);
        });

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

    private void showEditBudgetDialog(int position, String title, double currentLimit) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setText(String.valueOf(currentLimit));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(input);

        final CheckBox autoCalcCheckBox = new CheckBox(this);
        autoCalcCheckBox.setText("Auto-calculate related budgets");

        boolean isAutoCalc = sharedPreferences.getBoolean(Constants.KEY_AUTO_CALC_BUDGETS, true);
        autoCalcCheckBox.setChecked(isAutoCalc);
        layout.addView(autoCalcCheckBox);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + title)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String valueStr = input.getText().toString().trim();
                    if (valueStr.isEmpty()) {
                        return;
                    }

                    try {
                        float newLimit = Float.parseFloat(valueStr);
                        if (newLimit <= 0) {
                            Toast.makeText(this, "Budget must be greater than 0", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean autoCalc = autoCalcCheckBox.isChecked();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(Constants.KEY_AUTO_CALC_BUDGETS, autoCalc);

                        if (autoCalc) {
                            if (position == 0) {
                                editor.putFloat(Constants.KEY_LIMIT_0, newLimit);
                                editor.putFloat(Constants.KEY_LIMIT_1, newLimit * 30);
                            } else if (position == 1) {
                                editor.putFloat(Constants.KEY_LIMIT_0, newLimit / 30);
                                editor.putFloat(Constants.KEY_LIMIT_1, newLimit);
                            }
                        } else {
                            String targetKey = (position == 0) ? Constants.KEY_LIMIT_0 : Constants.KEY_LIMIT_1;
                            editor.putFloat(targetKey, newLimit);
                        }

                        invalidateCurrentPeriodNotifications(editor);
                        editor.apply();

                        timeBudgetAdapter.updateLimit(0, sharedPreferences.getFloat(Constants.KEY_LIMIT_0, 30.0f));
                        timeBudgetAdapter.updateLimit(1, sharedPreferences.getFloat(Constants.KEY_LIMIT_1, 1000.0f));

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    /**
     * Clears the 80%/100% "already notified" flags for the CURRENT day and month only,
     * so a changed limit is re-evaluated against fresh thresholds this period.
     * Past periods' flags are left alone — BudgetAlertManager.cleanupOldPrefs() is
     * responsible for pruning those once their period has ended.
     */
    private void invalidateCurrentPeriodNotifications(SharedPreferences.Editor editor) {
        long startOfDay = TimeUtils.getStartOfDay();
        long startOfMonth = TimeUtils.getStartOfMonth();

        String dailyPrefix = Constants.PERIOD_DAILY + "_";
        String monthlyPrefix = Constants.PERIOD_MONTHLY + "_";

        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.contains("_notified_")) {
                continue;
            }

            try {
                long timestamp = Long.parseLong(key.substring(key.lastIndexOf("_") + 1));

                if (key.startsWith(dailyPrefix) && timestamp == startOfDay) {
                    editor.remove(key);
                } else if (key.startsWith(monthlyPrefix) && timestamp == startOfMonth) {
                    editor.remove(key);
                }
            } catch (NumberFormatException e) {
                // Ignore malformed keys
            }
        }
    }
}