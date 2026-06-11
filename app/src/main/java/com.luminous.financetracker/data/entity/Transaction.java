package com.luminous.financetracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// TODO: Add the Entity annotation with the table name here
@Entity(tableName = "transactions")
public class Transaction {

    // TODO: Define the primary key field (auto-generated)
    @PrimaryKey(autoGenerate = true)
    private int id;

    // TODO: Define fields for amount, category, date, and description
    // Hint: Use double for amount, String for transaction detail, and maybe a long for date/timestamp
    private double amount;
    private String transactionDetail;
    private long timestamp;

    // TODO: Create a constructor to initialize these fields
    public Transaction () {
        this.amount=0;
        this.transactionDetail="";
        this.timestamp=0;
    }

    public Transaction (double amount, String transactionDetail,  long timestamp) {
        this.amount=amount;
        this.transactionDetail=transactionDetail;
        this.timestamp=timestamp;
    }

    // TODO: Create Getters for all fields
    // Hint: Room needs these to read the data
    // TODO: Create Setters for all fields (if you need to update data later)
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

    public String getTransactionDetail () {
        return this.transactionDetail;
    }

    public void setTransactionDetail (String transactionDetail) {
        this.transactionDetail=transactionDetail;
    }

    public long getTimestamp () {
        return this.timestamp;
    }

    public void setTimestamp (long timestamp) {
        this.timestamp=timestamp;
    } 

}