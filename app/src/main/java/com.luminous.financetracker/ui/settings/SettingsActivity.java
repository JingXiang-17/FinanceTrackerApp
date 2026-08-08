package com.luminous.financetracker.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.ui.budget.BudgetActivity;
import com.luminous.financetracker.ui.dashboard.DashboardActivity;
import com.luminous.financetracker.ui.statistics.StatisticsActivity;
import com.luminous.financetracker.viewmodel.TransactionViewModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private TransactionViewModel transactionViewModel;
    private List<Transaction> currentTransactions = new ArrayList<>();

    // Activity Result Launchers for file picking
    private ActivityResultLauncher<Intent> exportCsvLauncher;
    private ActivityResultLauncher<Intent> importCsvLauncher;

    // UI and Delay Logic
    private ProgressBar progressSync;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable showProgressRunnable = () -> {
        if (progressSync != null) {
            progressSync.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        progressSync = findViewById(R.id.progress_sync);

        // 1. Initialize ViewModel and cache the latest list of transactions for
        // exporting
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                currentTransactions = transactions;
            }
        });

        // 2. Setup the Export Launcher (Waits for user to choose WHERE to save)
        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        writeCsvToUri(uri);
                    }
                });

        // 3. Setup the Import Launcher (Waits for user to pick a CSV file)
        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        readCsvFromUri(uri);
                    }
                });

        // --- EXPORT BUTTON CLICK ---
        MaterialCardView cardExport = findViewById(R.id.card_export_csv);
        cardExport.setOnClickListener(v -> {
            if (currentTransactions.isEmpty()) {
                Toast.makeText(this, "No transactions to export!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Ask Android to create a new document
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "financetracker_export.csv");
            exportCsvLauncher.launch(intent);
        });

        // --- IMPORT BUTTON CLICK ---
        MaterialCardView cardImport = findViewById(R.id.card_import_csv);
        cardImport.setOnClickListener(v -> {
            // Ask Android to open a document
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // Using */* because some phones don't map text/csv properly
            importCsvLauncher.launch(intent);
        });

        // --- STICKY BOTTOM NAVIGATION LOGIC ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_stats) {
                startActivity(new Intent(getApplicationContext(), StatisticsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return itemId == R.id.nav_settings;
        });
    }

    // --- PROGRESS TOGGLES ---
    private void startLoadingWithDelay() {
        // Trigger the spinner to show up only if the task takes longer than 300ms
        mainHandler.postDelayed(showProgressRunnable, 300);
    }

    private void stopLoading() {
        // Cancel the delayed trigger, and hide the spinner if it's already showing
        mainHandler.removeCallbacks(showProgressRunnable);
        if (progressSync != null) {
            progressSync.setVisibility(View.GONE);
        }
    }

    // --- CSV WRITE LOGIC ---
    private void writeCsvToUri(Uri uri) {
        startLoadingWithDelay();

        // Run the heavy lifting on a background thread so the UI doesn't freeze
        new Thread(() -> {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

                // Write full CSV Header with all 7 fields
                writer.write("Title,Amount,Category,Timestamp,Notes,MerchantName,PaymentMethod\n");

                // Write Data Rows
                for (Transaction t : currentTransactions) {
                    // Strip commas from text to prevent breaking the CSV format
                    String safeTitle = (t.getText() != null) ? t.getText().replace(",", "") : "";
                    String safeCategory = (t.getCategory() != null) ? t.getCategory().replace(",", "") : "";
                    String safeNotes = (t.getNotes() != null) ? t.getNotes().replace(",", "") : "";
                    String safeMerchant = (t.getMerchantName() != null) ? t.getMerchantName().replace(",", "") : "";
                    String safePayment = (t.getPaymentMethod() != null) ? t.getPaymentMethod().replace(",", "") : "";

                    writer.write(safeTitle + "," + t.getAmount() + "," + safeCategory + "," +
                            t.getTimestamp() + "," + safeNotes + "," + safeMerchant + "," + safePayment + "\n");
                }

                writer.flush();
                writer.close();

                // Post success UI update back to main thread
                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Exported successfully!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                // Post failure UI update back to main thread
                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
            }
        }).start();
    }

    // --- CSV READ LOGIC ---
    private void readCsvFromUri(Uri uri) {
        startLoadingWithDelay();

        // Run on background thread
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                boolean isFirstLine = true;
                int importCount = 0;
                int duplicateCount = 0; // Keep track of skipped duplicates

                while ((line = reader.readLine()) != null) {
                    // Skip the header row
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    // Split by comma, passing -1 to keep empty trailing fields
                    String[] tokens = line.split(",", -1);
                    if (tokens.length >= 4) {
                        try {
                            String title = tokens[0];
                            double amount = Double.parseDouble(tokens[1]);
                            String category = tokens[2];
                            long timestamp = Long.parseLong(tokens[3]);

                            // --- NEW: DUPLICATE CHECKER ---
                            boolean isDuplicate = false;
                            for (Transaction existing : currentTransactions) {
                                // Check if the timeframe (timestamp) and title match
                                if (existing.getTimestamp() == timestamp && existing.getText().equals(title)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (isDuplicate) {
                                duplicateCount++;
                                continue; // Skip this row and move to the next one
                            }
                            // ------------------------------

                            // Safely grab the optional fields (allows backwards compatibility with 4-column
                            // CSVs)
                            String notes = (tokens.length >= 5) ? tokens[4] : "";
                            String merchant = (tokens.length >= 6) ? tokens[5] : "";
                            String payment = (tokens.length >= 7) ? tokens[6] : "";

                            // Recreate the transaction and set the extra fields
                            Transaction t = new Transaction(amount, title, category, timestamp);
                            t.setNotes(notes);
                            t.setMerchantName(merchant);
                            t.setPaymentMethod(payment);

                            // Insert into the database
                            transactionViewModel.insert(t);
                            importCount++;

                        } catch (NumberFormatException nfe) {
                            // Skip corrupted rows silently
                        }
                    }
                }
                reader.close();

                final int finalImportCount = importCount;
                final int finalDuplicateCount = duplicateCount;

                mainHandler.post(() -> {
                    stopLoading();
                    String message = "Imported " + finalImportCount + " new transactions!";
                    if (finalDuplicateCount > 0) {
                        message += " (" + finalDuplicateCount + " duplicates skipped)";
                    }
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
            }
        }).start();
    }
}