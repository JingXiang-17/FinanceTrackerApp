package com.luminous.financetracker.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget_table")
public class Budget {

    @PrimaryKey (autoGenerate = true)
    private int id;
    private double budget = 0;
    private String day = "", week = "", month = "", category = "";


    public Budget (double budget, String day, String week, String month, String category) {
        this.budget = budget;
        this.day = day;
        this.week = week;
        this.month = month;
        this.category = category;
    }

    public int getId () {
        return this.id;
    }
    public void setId (int id) {
        this.id = id;
    }
    public double getBudget () {
        return this.budget;
    }
    public void setBudget (double budget) {
        this.budget = budget;
    }
    public String getDay () {
        return this.day;
    }
    public void setDay (String day) {
        this.day = day;
    }
    public String getWeek () {
        return this.week;
    }
    public void setWeek (String week) {
        this.week = week;
    }
    public String getMonth () {
        return this.month;
    }
    public void setMonth (String month) {
        this.month = month;
    }
    public String getCategory () {
        return this.category;
    }
    public void setCategory (String category) {
        this.category = category;
    }
}
