package com.luminous.financetracker.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.data.entity.Budget;
import com.luminous.financetracker.repository.TransactionRepository;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private TransactionRepository repository;
    private LiveData<List<Transaction>> allTransactions;
    private LiveData <List<Budget>> budget;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        // Initialize your LiveData list of transactions here
        allTransactions = repository.getAllTransactions();
        budget = repository.getBudget();
    }

    // Expose methods for the UI to observe data or insert transactions
    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    // --- Budget Methods ---
    public LiveData<List<Budget>> getBudget() { return budget; }
    public LiveData<Budget> getBudgetByCategory(String categoryName) { return repository.getBudgetByCategory(categoryName); }
    public void insertBudget(Budget newBudget) { repository.insertBudget(newBudget); }
}