package com.luminous.financetracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import com.luminous.financetracker.data.entity.Budget;

@Dao
public interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBudget (Budget budget);

    // Get ALL budgets as a list
    @Query("SELECT * FROM budget_table")
    LiveData<List<Budget>> getAllBudgets();

    // Get a SPECIFIC budget by its category (e.g., "Food")
    @Query("SELECT * FROM budget_table WHERE category = :categoryName")
    LiveData<Budget> getBudgetByCategory(String categoryName);
}
