# FinanceTrackerApp

An offline-first, privacy-focused Android finance tracker built with Java, XML, and Room.

This app is being developed for personal use, but feel free to fork it if you want.

---

## Quick Navigation
- [Project Goals](#project-goals)
- [Current Progress](#current-progress)
- [Planned Features](#planned-features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Roadmap](#roadmap)
- [Release Notes](#release-notes)

---

## Project Goals

- Track daily expenses in one place
- Keep all financial data stored locally on-device
- Reduce manual entry through notification-based transaction capture
- Maintain a responsive UI with background database work
- Add budget tracking alongside transaction logging

## Current Progress

The current codebase includes core data, budgeting, and automation foundations:

- **Room database setup** with a singleton `FinanceDatabase`
- **`Transaction` entity** for storing expense and fixed-spending records
- **`TransactionDao`** for database operations
- **Repository layer** using background threads to prevent UI race conditions
- **`TransactionViewModel`** to expose transaction data to the UI
- **`DashboardActivity`** wired with a `RecyclerView`, custom adapters, and profile image storage
- **Notification listener service** that filters supported finance apps and parses transaction amounts from notifications
- **Edit/delete support** including swipe-to-delete functionality

## Planned Features

The app is built around a streamlined expense-tracking workflow:

- **Automated expense capture** from supported banking and e-wallet notifications
- **Manual transaction entry** for cash purchases and unsupported payments
- **Dashboard list view** to display today's transactions and search history ("See All")
- **Budget views** for tracking spending limits and daily targets
- **Edit and delete actions** for correcting logged entries
- **Total spent summary** for the selected time period

## Tech Stack

- **Language:** Java
- **UI:** Android XML (with Material 3 & Shimmer effects)
- **Database:** Room / SQLite
- **Concurrency:** ExecutorService
- **Platform:** Android (Min SDK 24, Target SDK 34)

## Architecture Overview

The app follows a clean separation of concerns:

- **Entity:** `Transaction`
- **DAO:** `TransactionDao`
- **Database:** `FinanceDatabase`
- **Repository:** `TransactionRepository`
- **ViewModel:** `TransactionViewModel`
- **Service:** `NotificationListener`
- **UI:** `DashboardActivity`, `TransactionAdapter`, `CategoryAdapter`, `MonthAdapter`

## Roadmap

Future improvements planned for later phases:

- Income tracking & advanced parsing (Roadmap v1.7+)
- Category tagging refinement
- Charts and spending analytics
- Export to CSV or PDF
- Dark mode support

## Note

This project is actively evolving. Core stable features are documented above in the latest release.

---

## Release Notes

### v1.1.0

#### 1. Added Features
- **Fixed spending:** Excluded from budget calculations, but included in total spending and pie chart rendering.
- **Profile picture:** Added support for uploading and storing profile pictures locally.

#### 2. Fixed Bugs
- Fixed the positioning of the "No transaction" empty state.
- Fixed "Today's transaction" filtering and the "See All" function.
- Fixed the calendar dropdown bug on the Statistics page.
- Fixed the dashboard top "today's spending" bug (now restricts view strictly to today's spending rather than the full month).
- Refactored Notification Listener parsing methods.
- Refactored files to utilize `Constants.java` (centralizing hardcoded strings).

#### 3. Changes in UI
- **Month selector:** Changed from rigid square boxes to a modern "pill" design with custom accent colors.
- Removed the 31-day limit for the date picker on the Statistics page.
- Changed "Monthly View" to "Calendar Date Picker" on the Statistics page.
- Added a bottom navigation bar with a bubble indicator showing the active page.

#### 4. Deletions
- Removed `BudgetDao.java` and `Budget.java` (replaced by the current streamlined budget system).
- Temporarily removed Dark Mode to reserve for a future release roadmap.    

### v1.1.1    
#### 1. Fixed bugs
- Fixed the logic of `NotificationListener.java` (Expense, Cash in and Merchant regex)