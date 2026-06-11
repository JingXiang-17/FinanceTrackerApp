package com.luminous.financetracker.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.luminous.financetracker.data.entity.Transaction;
import java.util.List;

// This Dao follows a CRUD pattern (Create, Read, Update, Delete)
@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    // 1. Fetch ALL transactions
    @Query("SELECT * FROM transactions")
    List<Transaction> getAllTransactions();

    // 2. Fetch by date
    @Query("SELECT * FROM transactions WHERE date = :date")
    List<Transaction> getTransactionsByDate(long date);

    // 3. Update the transaction details if have typo or anything
    @Update
    void update(Transaction transaction);

    // 4. Delete a transaction
    @Delete
    void delete(Transaction transaction);


}