package com.luminous.financetracker.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Import your files here (Adjust package names if yours are slightly different)
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.util.Constants;
import com.luminous.financetracker.util.DateConverter;

// 1. Define the entities (tables) in your database
@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
// 2. THIS WIRES UP YOUR DATE CONVERTER!
@TypeConverters({DateConverter.class})
public abstract class FinanceDatabase extends RoomDatabase {

    // Link to your DAO (Data Access Object)
    public abstract TransactionDao transactionDao();

    // Singleton instance to prevent multiple instances of the database opening at the same time
    private static volatile FinanceDatabase INSTANCE;

    public static FinanceDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FinanceDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    FinanceDatabase.class,
                                    Constants.DATABASE_NAME) // 3. THIS WIRES UP YOUR CONSTANTS!
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}