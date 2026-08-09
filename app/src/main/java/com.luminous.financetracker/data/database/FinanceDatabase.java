package com.luminous.financetracker.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.luminous.financetracker.data.entity.Budget;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.data.dao.BudgetDao;
import com.luminous.financetracker.util.Constants;
import com.luminous.financetracker.util.DateConverter;

// 1. Updated version to 3 for the new Transaction fields
@Database(entities = {Transaction.class, Budget.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class FinanceDatabase extends RoomDatabase {

    // 2. Link BOTH DAOs here
    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();

    private static volatile FinanceDatabase INSTANCE;

    public static FinanceDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FinanceDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    FinanceDatabase.class,
                                    Constants.DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}