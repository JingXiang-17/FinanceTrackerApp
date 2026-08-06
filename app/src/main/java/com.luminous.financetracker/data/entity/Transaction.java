package com.luminous.financetracker.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction_table")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private String text; // We will use this as the Main Title
    private String category;
    private long timestamp;

    // --- NEW FIELDS ---
    private String paymentMethod;
    private String merchantName;
    private String notes;

    // --- UI STATE (Ignored by Room) ---
    @Ignore
    private boolean isExpanded = false;

    // Default Constructor for Room
    public Transaction(double amount, String text, String category, long timestamp, String paymentMethod, String merchantName, String notes) {
        this.amount = amount;
        this.text = text;
        this.category = category;
        this.timestamp = timestamp;
        this.paymentMethod = paymentMethod;
        this.merchantName = merchantName;
        this.notes = notes;
    }

    // Backwards-compatible constructor so old code doesn't break
    @Ignore
    public Transaction(double amount, String text, String category, long timestamp) {
        this(amount, text, category, timestamp, "Unknown", "Unknown", "");
    }

    // --- GETTERS & SETTERS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}