package com.luminous.financetracker.data.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget_table")
public class Budget {

    // TODO 3: Add the @PrimaryKey annotation.
    @PrimaryKey (autoGenerate = true)
    private int id;

    // TODO 4: Declare a variable to hold the budget limit.
    private double budget = 0;

    // TODO 5: (Optional) Declare a variable to represent the month or category.
    private String day = "", week = "", month = "", category = "";

    // TODO 6: Generate your Constructor.
    public Budget (double budget, String day, String week, String month, String category) {
        this.budget = budget;
        this.day = day;
        this.week = week;
        this.month = month;
        this.category = category;
    }

    // TODO 7: Generate Getters and Setters for ALL your variables.
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
