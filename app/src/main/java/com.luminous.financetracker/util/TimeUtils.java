package com.luminous.financetracker.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    // FIX: Cache SimpleDateFormat using ThreadLocal to prevent memory churn on RecyclerView scrolls
    private static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            // FIX: Explicitly enforce default timezone to prevent accidental UTC shifts
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            return sdf;
        }
    };

    public static long getStartOfDay() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getStartOfWeek() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        // NOTE: In Malaysia, week start varies by state (e.g., Sunday in Johor, Monday in KL).
        // Relying on locale default is mathematically correct here, but ensure any custom UI
        // labels (like "Mon" to "Sun" charts) align with this dynamic start day.
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getStartOfMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String formatTimestamp(long timestamp) {
        return dateFormatter.get().format(new Date(timestamp));
    }

    // --- MULTI-YEAR FIX ---
    // Overloaded methods to accept a year parameter. The single-parameter version is retained
    // for backwards compatibility with the current Dashboard UI, defaulting to the current year.

    public static long getStartOfMonthIndex(int monthIndex) {
        int currentYear = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.YEAR);
        return getStartOfMonthIndex(currentYear, monthIndex);
    }

    public static long getStartOfMonthIndex(int year, int monthIndex) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_MONTH, 1); // Set day to 1 BEFORE setting month to avoid Feb 31st bug
        calendar.set(Calendar.MONTH, monthIndex);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getEndOfMonthIndex(int monthIndex) {
        int currentYear = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.YEAR);
        return getEndOfMonthIndex(currentYear, monthIndex);
    }

    public static long getEndOfMonthIndex(int year, int monthIndex) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_MONTH, 1); // Set day to 1 BEFORE setting month
        calendar.set(Calendar.MONTH, monthIndex);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}