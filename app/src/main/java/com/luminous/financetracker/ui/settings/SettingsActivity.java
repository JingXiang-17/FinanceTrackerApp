package com.luminous.financetracker.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Initialize ViewModel and cache the latest list of transactions for exporting
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
                }
        );

        // 3. Setup the Import Launcher (Waits for user to pick a CSV file)
        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        readCsvFromUri(uri);
                    }
                }
        );

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

    // --- CSV WRITE LOGIC ---
    private void writeCsvToUri(Uri uri) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

            // Write CSV Header
            writer.write("Title,Amount,Category,Timestamp\n");

            // Write Data Rows
            for (Transaction t : currentTransactions) {
                // Strip commas from text to prevent breaking the CSV format
                String safeTitle = (t.getText() != null) ? t.getText().replace(",", "") : "";
                String safeCategory = (t.getCategory() != null) ? t.getCategory().replace(",", "") : "";

                writer.write(safeTitle + "," + t.getAmount() + "," + safeCategory + "," + t.getTimestamp() + "\n");
            }

            writer.flush();
            writer.close();
            Toast.makeText(this, "Exported successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- CSV READ LOGIC ---
    private void readCsvFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            boolean isFirstLine = true;
            int importCount = 0;

            while ((line = reader.readLine()) != null) {
                // Skip the header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 4) {
                    try {
                        String title = tokens[0];
                        double amount = Double.parseDouble(tokens[1]);
                        String category = tokens[2];
                        long timestamp = Long.parseLong(tokens[3]);

                        // Recreate the transaction and insert into the database
                        Transaction t = new Transaction(amount, title, category, timestamp);
                        transactionViewModel.insert(t);
                        importCount++;
                    } catch (NumberFormatException nfe) {
                        // Skip corrupted rows silently
                    }
                }
            }

            reader.close();
            Toast.makeText(this, "Imported " + importCount + " transactions!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}