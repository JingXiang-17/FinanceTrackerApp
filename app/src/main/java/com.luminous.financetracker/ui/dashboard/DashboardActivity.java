package com.luminous.financetracker.ui.dashboard;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.adapter.TransactionAdapter;
import com.luminous.financetracker.viewmodel.TransactionViewModel;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1: Link your RecyclerView from the XML layout using findViewById()
        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        // 2: Initialize your LayoutManager and attach it to the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3: Initialize the TransactionAdapter and attach it to the RecyclerView
        TransactionAdapter adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        // 4: Initialize the TransactionViewModel using ViewModelProvider
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // 5: Observe the LiveData from the ViewModel.
        // When data changes, push the new list to the adapter!
        transactionViewModel.getAllTransactions().observe(this, new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                // adapter.setTransactions(transactions);
            }
        });
    }
}