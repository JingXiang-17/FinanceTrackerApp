package com.luminous.financetracker.data.database;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.data.dao.TransactionDao;
import com.luminous.financetracker.util.Constants;
import com.luminous.financetracker.util.DateConverter;

@Database(entities = {Transaction.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class FinanceDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    private static volatile FinanceDatabase INSTANCE;

    public static FinanceDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FinanceDatabase.class) {
                if (INSTANCE == null) {
                    RoomDatabase.Builder<FinanceDatabase> builder = Room.databaseBuilder(
                            context.getApplicationContext(),
                            FinanceDatabase.class,
                            Constants.DATABASE_NAME);

                    boolean isDebug = (context.getApplicationInfo().flags
                            & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

                    if (isDebug) {
                        // Debug builds only: safe to wipe and recreate on schema mismatch.
                        builder.fallbackToDestructiveMigration();
                    }
                    // Release builds: no fallback. If a real Migration isn't supplied
                    // for this version bump, Room will throw IllegalStateException
                    // at runtime instead of silently deleting user data — this is
                    // intentional per CLAUDE.md's migration policy.

                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }
}