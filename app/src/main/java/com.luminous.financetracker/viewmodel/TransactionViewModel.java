package com.luminous.financetracker.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.repository.TransactionRepository;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private TransactionRepository repository;
    private LiveData<List<Transaction>> allTransactions;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        // Initialize your LiveData list of transactions here
        allTransactions = repository.getAllTransactions();
    }

    // Expose methods for the UI to observe data or insert transactions
    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }
}
