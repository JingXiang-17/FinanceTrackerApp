package com.luminous.financetracker.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private TransactionViewModel transactionViewModel;
    private List<Transaction> currentTransactions = new ArrayList<>();

    private ActivityResultLauncher<Intent> exportCsvLauncher;
    private ActivityResultLauncher<Intent> importCsvLauncher;

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

        setupDataManagement();
        setupBottomNavigation();
    }

    private void setupDataManagement() {
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                currentTransactions = transactions;
            }
        });

        exportCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        writeCsvToUri(uri);
                    }
                });

        importCsvLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        readCsvFromUri(uri);
                    }
                });

        MaterialCardView cardExport = findViewById(R.id.card_export_csv);
        cardExport.setOnClickListener(v -> {
            if (currentTransactions.isEmpty()) {
                Toast.makeText(this, "No transactions to export!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "financetracker_export.csv");
            exportCsvLauncher.launch(intent);
        });

        MaterialCardView cardImport = findViewById(R.id.card_import_csv);
        cardImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            importCsvLauncher.launch(intent);
        });
    }

    // --- SETUP: DARK MODE ---
    // is currently commented out due to lazy to think of dark mode color palette
    /*private void setupDarkModeToggle() {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        SwitchCompat switchDarkMode = findViewById(R.id.switch_dark_mode);

        // 1. Determine if the app is currently in dark mode to set the switch's initial state
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchDarkMode.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        // 2. Listen for the user tapping the toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(newMode);
            prefs.edit().putInt("theme_mode", newMode).apply();
        });
    }*/
    private void setupBottomNavigation() {
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

    private void startLoadingWithDelay() {
        mainHandler.postDelayed(showProgressRunnable, 300);
    }

    private void stopLoading() {
        mainHandler.removeCallbacks(showProgressRunnable);
        if (progressSync != null) {
            progressSync.setVisibility(View.GONE);
        }
    }

    // --- PROPER CSV ESCAPING HELPER ---
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            // Escape double quotes by doubling them up, then wrap entire field in quotes
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void writeCsvToUri(android.net.Uri uri) {
        startLoadingWithDelay();

        new Thread(() -> {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

                writer.write("Title,Amount,Category,Timestamp,Notes,MerchantName,PaymentMethod\n");

                for (Transaction t : currentTransactions) {
                    String safeTitle = escapeCsvField(t.getText());
                    String safeCategory = escapeCsvField(t.getCategory());
                    String safeNotes = escapeCsvField(t.getNotes());
                    String safeMerchant = escapeCsvField(t.getMerchantName());
                    String safePayment = escapeCsvField(t.getPaymentMethod());

                    writer.write(safeTitle + "," + t.getAmount() + "," + safeCategory + "," +
                            t.getTimestamp() + "," + safeNotes + "," + safeMerchant + "," + safePayment + "\n");
                }

                writer.flush();
                writer.close();

                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Exported successfully!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "CSV Export failed", e);
                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // --- ROBUST CSV LINE PARSER ---
    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    sb.append('\"');
                    i++; // Skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private void readCsvFromUri(android.net.Uri uri) {
        startLoadingWithDelay();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                boolean isFirstLine = true;
                int importCount = 0;
                int duplicateCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    List<String> tokens = parseCsvLine(line);
                    if (tokens.size() >= 4) {
                        try {
                            String title = tokens.get(0);
                            double amount = Double.parseDouble(tokens.get(1));
                            String category = tokens.get(2);
                            long timestamp = Long.parseLong(tokens.get(3));

                            // FIX: Null-safe duplicate checker
                            boolean isDuplicate = false;
                            for (Transaction existing : currentTransactions) {
                                if (existing.getTimestamp() == timestamp && Objects.equals(existing.getText(), title)) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (isDuplicate) {
                                duplicateCount++;
                                continue;
                            }

                            String notes = (tokens.size() >= 5) ? tokens.get(4) : "";
                            String merchant = (tokens.size() >= 6) ? tokens.get(5) : "";
                            String payment = (tokens.size() >= 7) ? tokens.get(6) : "";

                            Transaction t = new Transaction(amount, title, category, timestamp);
                            t.setNotes(notes);
                            t.setMerchantName(merchant);
                            t.setPaymentMethod(payment);

                            transactionViewModel.insert(t);
                            importCount++;

                        } catch (NumberFormatException nfe) {
                            Log.w(TAG, "Skipping malformed row: " + line);
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
                Log.e(TAG, "CSV Import failed", e);
                mainHandler.post(() -> {
                    stopLoading();
                    Toast.makeText(SettingsActivity.this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}