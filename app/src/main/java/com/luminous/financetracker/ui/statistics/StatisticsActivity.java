package com.luminous.financetracker.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// MPAndroidChart Imports
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import com.luminous.financetracker.R;
import java.util.ArrayList;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Links this Activity to your Statistics XML layout
        setContentView(R.layout.activity_statistics);

        // 1. Link the XML views to your Java file
        PieChart pieChart = findViewById(R.id.chart_spending);
        TextView tvAnalysis = findViewById(R.id.tv_analysis);

        // 2. Create the dummy data entries for the mockup
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(98.9f, "Research"));
        entries.add(new PieEntry(47.2f, "Health"));
        entries.add(new PieEntry(51.4f, "Costs"));

        // 3. Create a dataset and assign colors to the slices
        PieDataSet dataSet = new PieDataSet(entries, "Monthly Spending");
        dataSet.setColors(new int[] {
                Color.parseColor("#3D82C4"), // Blue
                Color.parseColor("#D34B56"), // Red
                Color.parseColor("#E6B94A")  // Yellow
        });

        // 4. Feed the dataset to the chart
        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // 5. Configure the visual styling to make it a "Donut"
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(70f); // Size of the center hole
        pieChart.setCenterText("Total expenditure\nRM 197.50"); // The text inside the hole
        pieChart.setCenterTextSize(12f);
        pieChart.getDescription().setEnabled(false); // Hides default description label
        pieChart.getLegend().setEnabled(false); // Hides the default legend so you can use your custom XML legend

        // 6. Refresh the chart to render it on screen
        pieChart.invalidate();

        // 7. --- ANALYSIS ENGINE LOGIC ---
        float totalSpending = 0f;
        float highestAmount = 0f;
        String topCategory = "";

        // Loop through the chart data to find the total and the biggest slice
        for (PieEntry entry : entries) {
            totalSpending += entry.getValue(); // Add up the total

            if (entry.getValue() > highestAmount) {
                highestAmount = entry.getValue();
                topCategory = entry.getLabel(); // Save the name of the biggest category
            }
        }

        // Calculate the percentage (safeguard against dividing by zero)
        int percentage = 0;
        if (totalSpending > 0) {
            percentage = Math.round((highestAmount / totalSpending) * 100);
        }

        // Format the final string and push it to the UI
        String analysisText = "Your biggest expense this month was " + topCategory +
                ", making up " + percentage + "% of your total spending.";

        tvAnalysis.setText(analysisText);
    }
}