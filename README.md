# FinanceTrackerApp

An offline-first, privacy-focused Android finance tracker built with Java, XML, and Room.

This app is being developed for personal use, but feel free to fork it if you want.

## Project Goals

- Track daily expenses in one place
- Keep all financial data stored locally on-device
- Reduce manual entry through notification-based transaction capture
- Maintain a responsive UI with background database work
- Add budget tracking alongside transaction logging

## Current Progress

The current codebase already includes the core data, budgeting, and automation foundations:

- **Room database setup** with a singleton `AppDatabase`
- **`Transaction` and `Budget` entities** for storing expense and budget records
- **`TransactionDao` and `BudgetDao`** for database operations
- **Repository layer** using `ExecutorService` for background writes
- **`TransactionViewModel`** to expose transaction and budget data to the UI
- **`DashboardActivity`** wired to a `RecyclerView` and `TransactionAdapter`
- **Notification listener service** that filters supported finance apps and parses transaction amounts from notifications
- **Edit/delete support** at the data layer for transaction management

## Planned Features

The app is being built around a simple expense-tracking workflow:

- **Automated expense capture** from supported banking and e-wallet notifications
- **Manual transaction entry** for cash purchases and unsupported payments
- **Dashboard list view** to display all transactions
- **Budget views** for tracking spending limits by category or time period
- **Edit and delete actions** for correcting logged entries
- **Total spent summary** for the selected time period

## Tech Stack

- **Language:** Java
- **UI:** Android XML
- **Database:** Room / SQLite
- **Concurrency:** ExecutorService
- **Platform:** Android

## Architecture Overview

The app follows a clean separation of concerns:

- **Entity:** `Transaction`, `Budget`
- **DAO:** `TransactionDao`, `BudgetDao`
- **Database:** `AppDatabase`
- **Repository:** `TransactionRepository`
- **ViewModel:** `TransactionViewModel`
- **Service:** `NotificationListener`
- **UI:** `DashboardActivity`, `TransactionAdapter`

## Roadmap

Future improvements planned for later phases:

- Income tracking
- Category tagging
- Charts and spending analytics
- Export to CSV or PDF
- Better budget management and summaries

## Note

This project is still in progress, so some UI and user-facing features described above may not yet be fully implemented.
