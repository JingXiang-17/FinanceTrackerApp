# FinanceTrackerApp

An offline-first, privacy-focused Android finance tracker built with Java, XML, and Room.

This app is being developed for personal use, but feel free to fork it if you want.

## Project Goals

- Track daily expenses in one place
- Keep all financial data stored locally on-device
- Reduce manual entry through notification-based transaction capture
- Maintain a responsive UI with background database work

## Current Progress

The current codebase already includes the core data and automation foundations:

- **Room database setup** with a singleton `AppDatabase`
- **`Transaction` entity** for storing expense records
- **`TransactionDao`** with CRUD operations and total-spent queries
- **Repository layer** using `ExecutorService` for background database writes
- **Notification listener service** that filters supported finance apps and parses transaction amounts from notifications
- **Edit and delete support** at the data layer for transaction management

## Planned Features

The app is being built around a simple expense-tracking workflow:

- **Automated expense capture** from supported banking and e-wallet notifications
- **Manual transaction entry** for cash purchases and unsupported payments
- **Dashboard list view** to display all transactions
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

- **Entity:** `Transaction`
- **DAO:** `TransactionDao`
- **Database:** `AppDatabase`
- **Repository:** `TransactionRepository`
- **Service:** `NotificationListener`

## Roadmap

Future improvements planned for later phases:

- Income tracking
- Category tagging
- Charts and spending analytics
- Export to CSV or PDF

## Note

This project is still in progress, so some UI and user-facing features described above may not yet be implemented.
