package com.luminous.financetracker.ui.statistics;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// MPAndroidChart Imports
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

// Navigation and Data Imports
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.MonthAdapter;
import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.settings.SettingsActivity;
import com.luminous.financetracker.util.CustomPieMarker;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.Toast;

import android.content.SharedPreferences;
import android.text.Html;

public class StatisticsActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private int currentSelectedMonth = Calendar.getInstance().get(Calendar.MONTH);
    private List<Transaction> allTransactionsCache = new ArrayList<>();

    // Date Range Toggle Logic
    private boolean isCustomDateRange = false;
    private long customStartDate = 0L;
    private long customEndDate = 0L;

    // UI Elements
    private PieChart pieChart;
    private TextView tvAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // 1. Link the XML views
        pieChart = findViewById(R.id.chart_spending);
        tvAnalysis = findViewById(R.id.tv_analysis);

        // --- NEW: Link Shimmer and Real Content Views ---
        ShimmerFrameLayout shimmerContainer = findViewById(R.id.shimmer_view_container);
        View realContentLayout = findViewById(R.id.real_content_layout);

        // 2. Setup Month Selector RecyclerView
        RecyclerView rvMonths = findViewById(R.id.rv_months);
        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        TextView tvTimePeriod = findViewById(R.id.tv_time_period);

        MonthAdapter monthAdapter = new MonthAdapter(monthIndex -> {
            currentSelectedMonth = monthIndex;
            isCustomDateRange = false; // Turn off custom filter when a month is clicked
            tvTimePeriod.setText("Monthly View");
            updateChartData();
        });
        rvMonths.setAdapter(monthAdapter);
        rvMonths.scrollToPosition(currentSelectedMonth);

        // --- NEW: Trigger Material Date Range Picker ---
        findViewById(R.id.btn_date_filter).setOnClickListener(v -> showDateRangePicker(tvTimePeriod));

        // 3. Configure the Donut chart
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleRadius(50f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.BLACK);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);

        // 4. Initialize ViewModel & Observe Data
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                // --- NEW: Stop Shimmer and show Real Content ---
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
                realContentLayout.setVisibility(View.VISIBLE);

                allTransactionsCache = transactions; // Cache the data
                updateChartData(); // Process and draw the chart
            }
        });

        // 5. --- STICKY BOTTOM NAVIGATION LOGIC ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_stats);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0); finish(); return true;
            }
            return itemId == R.id.nav_stats;
        });
    }

    // --- CHART PROCESSING ENGINE ---
    private void updateChartData() {
        // 1. Filter transactions based on active mode
        List<Transaction> filteredTransactions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        int todayYear = cal.get(Calendar.YEAR);
        int todayDay = cal.get(Calendar.DAY_OF_YEAR);
        float todayTotalSpending = 0f;

        for (Transaction t : allTransactionsCache) {
            long tTime = t.getTimestamp();
            Calendar tCal = Calendar.getInstance();
            tCal.setTimeInMillis(tTime);

            boolean matchesFilter = false;

            // --- THE TOGGLE ---
            if (isCustomDateRange) {
                if (tTime >= customStartDate && tTime <= customEndDate) {
                    matchesFilter = true;
                }
            } else {
                if (tCal.get(Calendar.MONTH) == currentSelectedMonth) {
                    matchesFilter = true;
                }
            }

            if (matchesFilter) {
                filteredTransactions.add(t);
            }

            // Always track today's spending for the budget calculation
            if (tCal.get(Calendar.YEAR) == todayYear && tCal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                todayTotalSpending += t.getAmount();
            }
        }

        // 2. Safeguard for empty months (Draw Grey Chart)
        if (filteredTransactions.isEmpty()) {
            List<PieEntry> emptyEntries = new ArrayList<>();
            emptyEntries.add(new PieEntry(1f, "")); // 100% dummy slice

            PieDataSet emptyDataSet = new PieDataSet(emptyEntries, "");
            emptyDataSet.setColor(Color.parseColor("#E0E0E0")); // Light Grey
            emptyDataSet.setDrawValues(false);

            PieData emptyData = new PieData(emptyDataSet);
            pieChart.setData(emptyData);

            pieChart.setDrawEntryLabels(false);
            pieChart.setHighlightPerTapEnabled(false); // Prevents the white box from showing
            pieChart.setCenterText("Total\nExpense:\nRM 0.00");
            pieChart.invalidate();

            tvAnalysis.setText("No chart data available for this month.");
            return;
        }

        // --- NEW: Re-enable tapping if the data IS found ---
        pieChart.setHighlightPerTapEnabled(true);

        // 3. Aggregate Data
        Map<String, Float> categoryTotals = new HashMap<>();
        float totalSpending = 0f;

        for (Transaction t : filteredTransactions) {
            String categoryTitle = t.getCategory();
            float amount = (float) t.getAmount();
            categoryTotals.put(categoryTitle, categoryTotals.getOrDefault(categoryTitle, 0f) + amount);
            totalSpending += amount;
        }

        // 4. Prepare Chart Entries and Colors
        List<PieEntry> entries = new ArrayList<>();
        float highestAmount = 0f;
        String topCategory = "";
        ArrayList<Integer> chartColors = new ArrayList<>();

        Map<String, Integer> categoryColorMap = new HashMap<>();
        categoryColorMap.put("Food & Beverages", Color.parseColor("#D34B56"));
        categoryColorMap.put("Transport", Color.parseColor("#3D82C4"));
        categoryColorMap.put("Entertainment", Color.parseColor("#6C5CE7"));
        categoryColorMap.put("Others", Color.parseColor("#E6B94A"));
        int defaultFallbackColor = Color.parseColor("#888888");

        for (Map.Entry<String, Float> mapEntry : categoryTotals.entrySet()) {
            float sliceValue = mapEntry.getValue();
            String sliceName = mapEntry.getKey();

            entries.add(new PieEntry(sliceValue, sliceName));
            chartColors.add(categoryColorMap.getOrDefault(sliceName, defaultFallbackColor));

            if (sliceValue > highestAmount) {
                highestAmount = sliceValue;
                topCategory = sliceName;
            }
        }

        // 5. Render Dataset
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors);
        dataSet.setSelectionShift(8f);
        dataSet.setDrawValues(false); // Hide default values

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false); // Hide category labels on the chart

        // Keep the center text static
        pieChart.setCenterText(String.format("Total\nExpense:\nRM %.2f", totalSpending));
        pieChart.setOnChartValueSelectedListener(null); // Clear out the old listener

        // Attach the White Box Marker
        CustomPieMarker marker = new CustomPieMarker(this, R.layout.custom_marker_view);
        marker.setTotalSpending(totalSpending); // Pass the total so it can do the percentage math
        marker.setChartView(pieChart);
        pieChart.setMarker(marker);

        pieChart.invalidate(); // Redraw chart

        // 6. --- COMPREHENSIVE ANALYSIS TEXT GENERATOR ---
        int percentage = Math.round((highestAmount / totalSpending) * 100);
        String richText;

        if (isCustomDateRange) {
            // Simplified insight for custom ranges (budget comparisons don't make sense here)
            richText = "During this period, your biggest expense was <b>" + topCategory +
                    "</b>, making up <b>" + percentage + "%</b> of your spending.<br><br>" +
                    "You spent a total of <b>RM " + String.format("%.2f", totalSpending) + "</b>.";
        } else {
            // Original budget insight for full months
            SharedPreferences sharedPreferences = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);
            float monthlyLimit = sharedPreferences.getFloat("limit_2", 1000.0f);
            float dailyLimit = sharedPreferences.getFloat("limit_0", 30.0f);

            float monthlyDiff = monthlyLimit - totalSpending;
            float dailyDiff = dailyLimit - todayTotalSpending;

            String monthColor = monthlyDiff >= 0 ? "#00B894" : "#D34B56";
            String monthAction = monthlyDiff >= 0 ? "less than" : "<b><font color='#D34B56'>MORE</font></b> than";
            String monthFormatted = String.format("RM %.2f", Math.abs(monthlyDiff));

            String dayColor = dailyDiff >= 0 ? "#00B894" : "#D34B56";
            String dayAction = dailyDiff >= 0 ? "less than" : "<b><font color='#D34B56'>MORE</font></b> than";
            String dayFormatted = String.format("RM %.2f", Math.abs(dailyDiff));

            richText = "Your biggest expense this month was <b>" + topCategory + "</b>, making up <b>" + percentage + "%</b> of your total spending.<br><br>" +
                    "Today, you spent <font color='" + dayColor + "'><b>" + dayFormatted + "</b></font> " + dayAction + " your daily budget.<br><br>" +
                    "For this month, you are <font color='" + monthColor + "'><b>" + monthFormatted + "</b></font> " + monthAction + " your monthly budget.";
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvAnalysis.setText(Html.fromHtml(richText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvAnalysis.setText(Html.fromHtml(richText));
        }
    }

    private void showDateRangePicker(TextView tvTimePeriod) {
        MaterialDatePicker<Pair<Long, Long>> datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Range (Max 1 Month)")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                long start = selection.first;
                long end = selection.second;

                // Check if duration exceeds 31 days (31 days * 24h * 60m * 60s * 1000ms)
                if ((end - start) > 2678400000L) {
                    Toast.makeText(this, "Please select a range of 1 month or less.", Toast.LENGTH_SHORT).show();
                    return;
                }

                isCustomDateRange = true;
                customStartDate = start;
                // Add 23 hours, 59 mins, 59 secs to include the entire end day
                customEndDate = end + 86399999L;

                // Format the text to match "08 Jul 26 - 06 Aug 26"
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy", Locale.getDefault());
                tvTimePeriod.setText(sdf.format(start) + " - " + sdf.format(end));

                updateChartData();
            }
        });
        datePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }
}