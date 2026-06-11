package com.luminous.financetracker.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.data.entity.Transaction;

@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // This connects your DAO to the database
    public abstract TransactionDao transactionDao();

    // The "Manager" (Singleton) instance
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // This creates the actual database file on the phone
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "finance_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}