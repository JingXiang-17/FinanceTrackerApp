package com.luminous.financetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.luminous.financetracker.data.entity.Transaction;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transaction_table")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transaction_table WHERE timestamp = :date")
    List<Transaction> getTransactionsByDate(long date);

    @Query("SELECT * FROM transaction_table WHERE id = :transactionId LIMIT 1")
    Transaction getTransactionById(int transactionId);

    // FIX: Replaced hardcoded 'Fixed' with excludedCategory parameter
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_table WHERE timestamp >= :startTimestamp AND category != :excludedCategory")
    LiveData<Double> getTotalSpentSince(long startTimestamp, String excludedCategory);

    // FIX: Added excludedCategory parameter to protect the Statistics date range
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_table WHERE timestamp >= :startDate AND timestamp <= :endDate AND category != :excludedCategory")
    LiveData<Double> getTotalSpentByDateRange(long startDate, long endDate, String excludedCategory);

    // Synchronous total spent (For Background Budget Checking)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_table WHERE timestamp >= :startTimestamp AND category != :excludedCategory")
    double getTotalVariableSpentSinceSync(long startTimestamp, String excludedCategory);

    // For the Statistics screen (Includes EVERYTHING, even Fixed)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transaction_table WHERE timestamp >= :startDate AND timestamp <= :endDate")
    LiveData<Double> getAbsoluteTotalSpentByDateRange(long startDate, long endDate);
}