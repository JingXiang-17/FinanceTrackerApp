package com.luminous.financetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.luminous.financetracker.data.entity.Transaction;
import java.util.List;

// This Dao follows a CRUD pattern (Create, Read, Update, Delete)
@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    // 1. Fetch ALL transactions
    @Query("SELECT * FROM transactions")
    LiveData<List<Transaction>> getAllTransactions();

    // 2. Fetch by date
    @Query("SELECT * FROM transactions WHERE timestamp = :date")
    List<Transaction> getTransactionsByDate(long date);

    // --- FIXED: Changed transaction_table to transactions ---
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE timestamp >= :startTimestamp")
    LiveData<Double> getTotalSpentSince(long startTimestamp);
    // --------------------------------------------------------

    // 3. Update the transaction details if have typo or anything
    @Update
    void update(Transaction transaction);

    // 4. Delete a transaction
    @Delete
    void delete(Transaction transaction);

    // 5. Retrieve a specific entity by its unique ID for Edit Mode
    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    Transaction getTransactionById(int transactionId);

    // 6. Calculate total spent for the dynamic summation card
    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startDate AND timestamp <= :endDate")
    LiveData<Double> getTotalSpentByDateRange(long startDate, long endDate);
}