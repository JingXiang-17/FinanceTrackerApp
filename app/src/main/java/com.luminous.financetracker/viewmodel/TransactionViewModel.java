package com.luminous.financetracker.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.repository.TransactionRepository;
import com.luminous.financetracker.util.BudgetAlertManager;

import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> allTransactions;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        allTransactions = repository.getAllTransactions();
    }

    // --- WRITE OPERATIONS ---

    public void insert(Transaction transaction) {
        repository.insert(transaction, () -> {
            FinanceDatabase db = FinanceDatabase.getDatabase(getApplication());
            BudgetAlertManager.checkBudgets(getApplication(), db.transactionDao());
        });
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    // --- READ OPERATIONS ---

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    // Returns variable spending only (For Budget bars/limits)
    public LiveData<Double> getTotalSpent(long startDate, long endDate) {
        return repository.getTotalSpent(startDate, endDate);
    }

    // Returns variable spending only (For Budget bars/limits)
    public LiveData<Double> getTotalSpentSince(long startTimestamp) {
        return repository.getTotalSpentSince(startTimestamp);
    }

    // Returns ALL spending (For Statistics/Pie Charts)
    public LiveData<Double> getAbsoluteTotalSpent(long startDate, long endDate) {
        return repository.getAbsoluteTotalSpent(startDate, endDate);
    }
}