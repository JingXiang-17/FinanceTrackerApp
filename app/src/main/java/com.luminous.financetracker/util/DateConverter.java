package com.luminous.financetracker.util;

import androidx.room.TypeConverter;
import java.util.Date;

public class DateConverter {

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        // Convert database long timestamp to Date object
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        // Convert Date object to long timestamp for Room storage
        return date == null ? null : date.getTime();
    }
}