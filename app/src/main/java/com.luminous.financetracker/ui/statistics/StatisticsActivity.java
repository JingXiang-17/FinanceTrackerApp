package com.luminous.financetracker.ui.statistics;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.luminous.financetracker.util.Constants;
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

    private static final String TAG = "StatisticsActivity";
    private static final long MILLIS_IN_DAY_MINUS_ONE = 86399999L; // 23:59:59.999

    private TransactionViewModel transactionViewModel;
    private int currentSelectedMonth = Calendar.getInstance().get(Calendar.MONTH);
    private int currentSelectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private List<Transaction> allTransactionsCache = new ArrayList<>();

    // Date Range Toggle Logic
    private boolean isCustomDateRange = false;
    private long customStartDate = 0L;
    private long customEndDate = 0L;
    private MonthAdapter monthAdapter;
    private RecyclerView rvMonths;

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

        ShimmerFrameLayout shimmerContainer = findViewById(R.id.shimmer_view_container);
        View realContentLayout = findViewById(R.id.real_content_layout);

        // 2. Setup Month Selector RecyclerView (Updated to use year and month context)
        rvMonths = findViewById(R.id.rv_months);
        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        TextView tvTimePeriod = findViewById(R.id.tv_time_period);

        tvTimePeriod.setText("Calendar Date Picker");
        tvTimePeriod.setOnClickListener(v -> showDateRangePicker(tvTimePeriod));

        // FIX: Upgraded callback signature to match MonthAdapter's (year, monthIndex) contract
        monthAdapter = new MonthAdapter((year, monthIndex) -> {
            currentSelectedYear = year;
            currentSelectedMonth = monthIndex;
            isCustomDateRange = false;
            tvTimePeriod.setText("Calendar Date Picker");

            monthAdapter.setSelectedIndex(monthIndex);
            updateChartData();
        });
        rvMonths.setAdapter(monthAdapter);
        rvMonths.scrollToPosition(currentSelectedMonth);

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
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
                realContentLayout.setVisibility(View.VISIBLE);

                allTransactionsCache = transactions;
                updateChartData();
            }
        });

        // 5. STICKY BOTTOM NAVIGATION LOGIC
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

    // --- CHART PROCESSING ENGINE ---
    private void updateChartData() {
        List<Transaction> filteredTransactions = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        int todayYear = cal.get(Calendar.YEAR);
        int todayDay = cal.get(Calendar.DAY_OF_YEAR);

        float todayBudgetableSpending = 0f;

        for (Transaction t : allTransactionsCache) {
            long tTime = t.getTimestamp();
            Calendar tCal = Calendar.getInstance();
            tCal.setTimeInMillis(tTime);

            boolean matchesFilter = false;

            if (isCustomDateRange) {
                if (tTime >= customStartDate && tTime <= customEndDate) {
                    matchesFilter = true;
                }
            } else {
                // FIX: Guard against cross-year misalignment by checking both year and month
                if (tCal.get(Calendar.YEAR) == currentSelectedYear && tCal.get(Calendar.MONTH) == currentSelectedMonth) {
                    matchesFilter = true;
                }
            }

            if (matchesFilter) {
                filteredTransactions.add(t);
            }

            if (tCal.get(Calendar.YEAR) == todayYear && tCal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                if (!Constants.CATEGORY_FIXED.equalsIgnoreCase(t.getCategory())) {
                    todayBudgetableSpending += t.getAmount();
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            List<PieEntry> emptyEntries = new ArrayList<>();
            emptyEntries.add(new PieEntry(1f, ""));

            PieDataSet emptyDataSet = new PieDataSet(emptyEntries, "");
            emptyDataSet.setColor(Color.parseColor("#E0E0E0"));
            emptyDataSet.setDrawValues(false);

            PieData emptyData = new PieData(emptyDataSet);
            pieChart.setData(emptyData);

            pieChart.setDrawEntryLabels(false);
            pieChart.setHighlightPerTapEnabled(false);
            pieChart.setCenterText("Total\nExpense:\nRM 0.00");
            pieChart.invalidate();

            tvAnalysis.setText("No chart data available for this period.");
            return;
        }

        pieChart.setHighlightPerTapEnabled(true);

        // 3. Aggregate Data
        Map<String, Float> categoryTotals = new HashMap<>();
        float totalSpending = 0f;
        float budgetableSpending = 0f;
        float fixedSpendingTotal = 0f;

        for (Transaction t : filteredTransactions) {
            String categoryTitle = t.getCategory();
            float amount = (float) t.getAmount();

            categoryTotals.put(categoryTitle, categoryTotals.getOrDefault(categoryTitle, 0f) + amount);
            totalSpending += amount;

            // FIX: Enforce Constants.CATEGORY_FIXED instead of raw string literal
            if (Constants.CATEGORY_FIXED.equalsIgnoreCase(categoryTitle)) {
                fixedSpendingTotal += amount;
            } else {
                budgetableSpending += amount;
            }
        }

        // 4. Prepare Chart Entries and Colors
        List<PieEntry> entries = new ArrayList<>();
        float highestAmount = 0f;
        String topCategory = "";
        ArrayList<Integer> chartColors = new ArrayList<>();

        Map<String, Integer> categoryColorMap = new HashMap<>();
        categoryColorMap.put(Constants.CATEGORY_FIXED, Color.parseColor("#A498FA"));
        categoryColorMap.put(Constants.CATEGORY_DINING, Color.parseColor("#FC5B68"));
        categoryColorMap.put(Constants.CATEGORY_TRANSPORT, Color.parseColor("#FFB12B"));
        categoryColorMap.put(Constants.CATEGORY_ENTERTAINMENT, Color.parseColor("#5BB1EB"));
        categoryColorMap.put(Constants.CATEGORY_SHOPPING, Color.parseColor("#EB73D3"));
        categoryColorMap.put(Constants.CATEGORY_OTHERS, Color.parseColor("#53CF95"));
        int defaultFallbackColor = Color.parseColor("#B4B4B4");

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
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);

        pieChart.setCenterText(String.format("Total\nExpense:\nRM %.2f", totalSpending));
        pieChart.setOnChartValueSelectedListener(null);

        CustomPieMarker marker = new CustomPieMarker(this, R.layout.custom_marker_view);
        marker.setTotalSpending(totalSpending);
        marker.setChartView(pieChart);
        pieChart.setMarker(marker);

        pieChart.invalidate();

        // 6. Comprehensive Analysis Text Generator
        int percentage = (totalSpending > 0) ? Math.round((highestAmount / totalSpending) * 100) : 0;
        String richText;

        if (isCustomDateRange) {
            richText = "During this period, your biggest expense was <b>" + topCategory +
                    "</b>, making up <b>" + percentage + "%</b> of your spending.<br><br>" +
                    "Fixed spending: <b>RM " + String.format("%.2f", fixedSpendingTotal) + "</b>.<br><br>" +
                    "You spent a total of <b>RM " + String.format("%.2f", totalSpending) + "</b>.";
        } else {
            // FIX: Use Constants for SharedPreferences and limit keys
            SharedPreferences sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
            float monthlyLimit = sharedPreferences.getFloat(Constants.KEY_LIMIT_1, 1000.0f);
            float dailyLimit = sharedPreferences.getFloat(Constants.KEY_LIMIT_0, 30.0f);

            float monthlyDiff = monthlyLimit - budgetableSpending;
            float dailyDiff = dailyLimit - todayBudgetableSpending;

            String monthColor = monthlyDiff >= 0 ? "#00B894" : "#D34B56";
            String monthAction = monthlyDiff >= 0 ? "less than" : "<b><font color='#D34B56'>MORE</font></b> than";
            String monthFormatted = String.format("RM %.2f", Math.abs(monthlyDiff));

            String dayColor = dailyDiff >= 0 ? "#00B894" : "#D34B56";
            String dayAction = dailyDiff >= 0 ? "less than" : "<b><font color='#D34B56'>MORE</font></b> than";
            String dayFormatted = String.format("RM %.2f", Math.abs(dailyDiff));

            richText = "Your biggest expense this month was <b>" + topCategory + "</b>, making up <b>" + percentage
                    + "%</b> of your total spending.<br><br>" +
                    "Fixed spending: <b>RM " + String.format("%.2f", fixedSpendingTotal) + "</b> (Excluded from budget).<br><br>" +
                    "Today, your variable spending is <font color='" + dayColor + "'><b>" + dayFormatted + "</b></font> " + dayAction
                    + " your daily budget.<br><br>" +
                    "For this month, your variable spending is <font color='" + monthColor + "'><b>" + monthFormatted + "</b></font> "
                    + monthAction + " your monthly budget.";
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvAnalysis.setText(Html.fromHtml(richText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            tvAnalysis.setText(Html.fromHtml(richText));
        }
    }

    private void showDateRangePicker(TextView tvTimePeriod) {
        try {
            MaterialDatePicker<Pair<Long, Long>> datePicker = MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select Custom Range")
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection.first != null && selection.second != null) {
                    long start = selection.first;
                    long end = selection.second;

                    isCustomDateRange = true;
                    customStartDate = start;
                    customEndDate = end + MILLIS_IN_DAY_MINUS_ONE; // FIX: Replaced raw magic number

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy", Locale.getDefault());
                    tvTimePeriod.setText(sdf.format(start) + " - " + sdf.format(end));

                    Calendar startCal = Calendar.getInstance();
                    startCal.setTimeInMillis(start);

                    Calendar endCal = Calendar.getInstance();
                    endCal.setTimeInMillis(end);

                    if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                            startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)) {
                        int targetMonth = startCal.get(Calendar.MONTH);
                        currentSelectedYear = startCal.get(Calendar.YEAR);
                        monthAdapter.setSelectedIndex(targetMonth);
                        rvMonths.smoothScrollToPosition(targetMonth);
                    } else {
                        monthAdapter.setSelectedIndex(-1);
                    }

                    updateChartData();
                }
            });

            datePicker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");

        } catch (Exception e) {
            // FIX: Replaced printStackTrace() with professional Log.e logging
            Log.e(TAG, "Failed to show MaterialDatePicker", e);
            Toast.makeText(this, "Calendar Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}