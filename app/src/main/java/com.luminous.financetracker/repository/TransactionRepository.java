package com.luminous.financetracker.repository;

import androidx.lifecycle.LiveData;
import android.app.Application;

import com.luminous.financetracker.data.database.FinanceDatabase;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.util.Constants;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private TransactionDao transactionDao;
    private LiveData<List<Transaction>> allTransactions;

    private static final int NUMBER_OF_THREADS = 4;
    private ExecutorService executorService;

    public TransactionRepository(Application application) {
        FinanceDatabase db = FinanceDatabase.getDatabase(application);
        transactionDao = db.transactionDao();
        allTransactions = transactionDao.getAllTransactions();
        executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }

    public interface InsertCallback {
        void onInsertComplete();
    }

    public interface TransactionCallback {
        void onTransactionLoaded(Transaction transaction);
    }

    // --- WRITE OPERATIONS ---

    // FIX: Removed the no-callback insert() foot-gun.
    // All insertions must now acknowledge the callback parameter (can pass null if genuinely not needed).
    public void insert(Transaction transaction, InsertCallback callback) {
        executorService.execute(() -> {
            transactionDao.insert(transaction);
            if (callback != null) {
                callback.onInsertComplete();
            }
        });
    }

    public void update(Transaction transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void delete(Transaction transaction) {
        executorService.execute(() -> transactionDao.delete(transaction));
    }

    // --- READ OPERATIONS ---
    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    // Used for Budgeting (Excludes Fixed)
    public LiveData<Double> getTotalSpent(long startDate, long endDate) {
        return transactionDao.getTotalSpentByDateRange(startDate, endDate, Constants.CATEGORY_FIXED);
    }

    // Used for Budgeting (Excludes Fixed)
    public LiveData<Double> getTotalSpentSince(long startTimestamp) {
        return transactionDao.getTotalSpentSince(startTimestamp, Constants.CATEGORY_FIXED);
    }

    // Used for Statistics Screen (Includes Everything)
    public LiveData<Double> getAbsoluteTotalSpent(long startDate, long endDate) {
        return transactionDao.getAbsoluteTotalSpentByDateRange(startDate, endDate);
    }

    public void getTransactionById(int id, final TransactionCallback callback) {
        executorService.execute(() -> {
            Transaction transaction = transactionDao.getTransactionById(id);
            if (callback != null) {
                callback.onTransactionLoaded(transaction);
            }
        });
    }
}