package com.luminous.financetracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {

    // Define the primary key field (auto-generated)
    @PrimaryKey(autoGenerate = true)
    private int id;

    // Define fields for amount, category, date, and description
    private double amount;
    private String text;
    private long timestamp;

    // Create a constructor to initialize these fields
    public Transaction () {
        this.amount=0;
        this.text="";
        this.timestamp=0;
    }

    public Transaction (double amount, String text,  long timestamp) {
        this.amount=amount;
        this.text=text;
        this.timestamp=timestamp;
    }

    // Create Getters for all fields
    // Hint: Room needs these to read the data
    // Create Setters for all fields (if you need to update data later)
    // Hint: Room uses these to modify data
    public int getId () {
        return this.id;
    }

    public void setId (int id) {
        this.id=id;
    }

    public double getAmount () {
        return this.amount;
    }

    public void setAmount (double amount) {
        this.amount=amount;
    }

    public String getText () {
        return this.text;
    }

    public void setText (String text) {
        this.text=text;
    }

    public long getTimestamp () {
        return this.timestamp;
    }

    public void setTimestamp (long timestamp) {
        this.timestamp=timestamp;
    } 

}