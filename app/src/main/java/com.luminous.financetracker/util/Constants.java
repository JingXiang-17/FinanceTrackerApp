package com.luminous.financetracker.util;

public class Constants {
    // Database Name
    public static final String DATABASE_NAME = "finance_tracker";

    // Intent Actions or Broadcast Keys (if passing data between your service and UI)
    public static final String ACTION_NEW_TRANSACTION = "com.luminous.financetracker.NEW_TRANSACTION";

    // Shared Preferences Keys (for app settings, dark mode, or onboarding flags)
    public static final String PREF_NAME = "finance_tracker_prefs";
    public static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";

    // Private constructor to prevent instantiation
    private Constants() {}
}