package com.luminous.financetracker.ui.statistics;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

// MPAndroidChart Imports
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

// Navigation and Data Imports
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // 1. Link the XML views to your Java file
        PieChart pieChart = findViewById(R.id.chart_spending);
        TextView tvAnalysis = findViewById(R.id.tv_analysis);

        // 2. Configure the static visual styling for the "Donut" hole
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(70f);
        pieChart.setCenterTextSize(12f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);

        // 3. Initialize the ViewModel
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // 4. Observe the live database data
        transactionViewModel.getAllTransactions().observe(this, transactions -> {

            // Safeguard: If the database is empty, clear the chart and exit early
            if (transactions == null || transactions.isEmpty()) {
                pieChart.clear();
                pieChart.setCenterText("Total expenditure\nRM 0.00");
                tvAnalysis.setText("No chart data available yet.");
                return;
            }

            // --- DATA AGGREGATION ENGINE ---
            // Group transactions by their title/text to calculate the total per category
            Map<String, Float> categoryTotals = new HashMap<>();
            float totalSpending = 0f;

            for (Transaction t : transactions) {
                String categoryTitle = t.getCategory();
                float amount = (float) t.getAmount();

                categoryTotals.put(categoryTitle, categoryTotals.getOrDefault(categoryTitle, 0f) + amount);
                totalSpending += amount;
            }

            // --- CHART RENDERING ENGINE ---
            List<PieEntry> entries = new ArrayList<>();
            float highestAmount = 0f;
            String topCategory = "";

            // 1. Prepare your dynamic ArrayList for colors
            ArrayList<Integer> chartColors = new ArrayList<>();

            // 2. Define a strong palette of default fallback colors
            int[] defaultPalette = new int[] {
                    Color.parseColor("#3D82C4"), // Blue
                    Color.parseColor("#D34B56"), // Red
                    Color.parseColor("#E6B94A"), // Yellow
                    Color.parseColor("#6C5CE7"), // Purple
                    Color.parseColor("#00CEC9"), // Teal
                    Color.parseColor("#FD79A8"), // Pink
                    Color.parseColor("#00B894")  // Mint
            };

            int colorIndex = 0;

            // Convert the grouped data into PieChart slices
            for (Map.Entry<String, Float> mapEntry : categoryTotals.entrySet()) {
                float sliceValue = mapEntry.getValue();
                String sliceName = mapEntry.getKey();

                entries.add(new PieEntry(sliceValue, sliceName));

                // 3. Assign a color to this specific category
                // (In the future, you will query your database here to check if the user picked a custom color for 'sliceName')
                // For now, we safely loop through the default palette using the modulo operator (%)
                chartColors.add(defaultPalette[colorIndex % defaultPalette.length]);
                colorIndex++;

                // Simultaneously figure out which slice is the biggest for the analysis text
                if (sliceValue > highestAmount) {
                    highestAmount = sliceValue;
                    topCategory = sliceName;
                }
            }

            // 4. Feed the dynamic ArrayList to the dataset
            PieDataSet dataSet = new PieDataSet(entries, "Monthly Spending");
            dataSet.setColors(chartColors); // MPAndroidChart accepts an ArrayList natively!

            PieData data = new PieData(dataSet);
            pieChart.setData(data);

            // Update the center text with the real total amount
            pieChart.setCenterText(String.format("Total expenditure\nRM %.2f", totalSpending));

            // Refresh the chart to render it on screen
            pieChart.invalidate();

            // --- ANALYSIS TEXT GENERATOR ---
            int percentage = 0;
            if (totalSpending > 0) {
                percentage = Math.round((highestAmount / totalSpending) * 100);
            }

            String analysisText = "Your biggest expense this month was " + topCategory +
                    ", making up " + percentage + "% of your total spending.";

            tvAnalysis.setText(analysisText);
        });

        // 5. --- STICKY BOTTOM NAVIGATION LOGIC ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_stats);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
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
            return itemId == R.id.nav_stats;
        });
    }
}