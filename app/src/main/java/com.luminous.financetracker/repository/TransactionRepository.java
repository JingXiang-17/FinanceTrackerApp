package com.luminous.financetracker.repository;

import androidx.lifecycle.LiveData;
import android.app.Application;
import com.luminous.financetracker.data.AppDatabase;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.data.entity.Budget;
import com.luminous.financetracker.data.dao.BudgetDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private TransactionDao transactionDao;
    private LiveData<List<Transaction>> allTransactions;
    private BudgetDao budgetDao;
    private LiveData<List<Budget>> currentBudget;
    // A background thread pool so database operations don't freeze the screen
    private static final int NUMBER_OF_THREADS = 4;
    private ExecutorService executorService;

    public TransactionRepository(Application application) {
        // Initialize the database instance
        AppDatabase db = AppDatabase.getDatabase(application);
        // Link the DAO
        transactionDao = db.transactionDao();
        allTransactions = transactionDao.getAllTransactions();
        budgetDao = db.budgetDao();
        currentBudget = budgetDao.getAllBudgets();
        // Create a background thread worker
        executorService = Executors.newSingleThreadExecutor();
    }

    // --- WRITE OPERATIONS (Background Thread) ---

    public void insert(Transaction transaction) {
        // TODO: Tell the executorService to execute a background task
        // TODO: Inside that task, call transactionDao.insert(transaction)
        executorService.execute(()->{
            transactionDao.insert(transaction);
        });
    }

    public void update(Transaction transaction) {
        // TODO: Do the same for update
        executorService.execute(()->{
            transactionDao.update(transaction);
        });
    }

    public void delete(Transaction transaction) {
        // TODO: Do the same for delete
        executorService.execute(()->{
            transactionDao.delete(transaction);
        });
    }

    // --- READ OPERATIONS ---

    public LiveData<List<Transaction>> getAllTransactions() {
        // NOTE: Returning data from a background thread to the UI is complex.
        return allTransactions;
    }

    public LiveData<Double> getTotalSpent(long startDate, long endDate) {
        // LiveData automatically handles its own background threading for reads
        return transactionDao.getTotalSpentByDateRange(startDate, endDate);
    }

    public void getTransactionById(int id, final TransactionCallback callback) {
        // Dispatched to background thread via ExecutorService as specified in the PDF
        executorService.execute(() -> {
            Transaction transaction = transactionDao.getTransactionById(id);
            // In Java, we use a callback interface to send the single object back to the Main Thread
            if (callback != null) {
                callback.onTransactionLoaded(transaction);
            }
        });
    }

    // A simple interface needed to pass the single transaction back from the background thread
    public interface TransactionCallback {
        void onTransactionLoaded(Transaction transaction);
    }

    // --- Budget Methods ---

    // TODO 3: Create a getter for the currentBudget LiveData
    public LiveData<List<Budget>> getBudget() {
        return currentBudget;
    }
    public LiveData<Budget> getBudgetByCategory(String categoryName) {
        return budgetDao.getBudgetByCategory(categoryName);
    }

    // TODO 4: Create an insert method for the budget that runs on the background thread
    public void insertBudget(Budget budget) {
        executorService.execute(() -> {
            budgetDao.insertBudget(budget);
        });
    }
}