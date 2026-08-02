package com.luminous.financetracker.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1: Fixed the ID! Link to rv_transactions instead of recycler_view
        RecyclerView recyclerView = findViewById(R.id.rv_transactions);

        // 2: Initialize your LayoutManager and attach it to the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3: Initialize the TransactionAdapter and attach it to the RecyclerView
        TransactionAdapter adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        // 4: Initialize the TransactionViewModel using ViewModelProvider
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // 5: Observe the LiveData from the ViewModel
        transactionViewModel.getAllTransactions().observe(this, new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                // adapter.setTransactions(transactions);
            }
        });

        // 6: --- STICKY BOTTOM NAVIGATION LOGIC ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Highlight the Home icon since we are on the Dashboard
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on the Home screen, do nothing
                return true;
            } else if (itemId == R.id.nav_stats) {
                // Navigate to Statistics
                startActivity(new Intent(getApplicationContext(), StatisticsActivity.class));
                overridePendingTransition(0, 0); // Removes the sliding animation
                finish(); // Close Dashboard so it doesn't pile up
                return true;
            } else if (itemId == R.id.nav_budget) {
                // Uncomment this when you create BudgetActivity!
                // startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                // overridePendingTransition(0, 0);
                // finish();
                return true;
            }
            return false;
        });
    }
}