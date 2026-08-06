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
    @Query("SELECT * FROM transaction_table")
    LiveData<List<Transaction>> getAllTransactions();

    // 2. Fetch by date
    @Query("SELECT * FROM transaction_table WHERE timestamp = :date")
    List<Transaction> getTransactionsByDate(long date);

    // 3. Calculate total spent since a specific time
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_table WHERE timestamp >= :startTimestamp")
    LiveData<Double> getTotalSpentSince(long startTimestamp);

    // 4. Update the transaction details if have typo or anything
    @Update
    void update(Transaction transaction);

    // 5. Delete a transaction
    @Delete
    void delete(Transaction transaction);

    // 6. Retrieve a specific entity by its unique ID for Edit Mode
    @Query("SELECT * FROM transaction_table WHERE id = :transactionId LIMIT 1")
    Transaction getTransactionById(int transactionId);

    // 7. Calculate total spent for the dynamic summation card
    @Query("SELECT SUM(amount) FROM transaction_table WHERE timestamp >= :startDate AND timestamp <= :endDate")
    LiveData<Double> getTotalSpentByDateRange(long startDate, long endDate);

    // 8. Synchronous total spent (for Background Budget checking)
    @Query("SELECT SUM(amount) FROM transaction_table WHERE timestamp >= :startTime")
    double getTotalSpentSinceSync(long startTime);
}