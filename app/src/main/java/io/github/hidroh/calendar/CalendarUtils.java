package io.github.hidroh.calendar;

import android.content.Context;
import android.text.format.DateUtils;

import java.util.Calendar;

public class CalendarUtils {

    public static void stripTime(CalendarDate calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static String toDayString(Context context, long timeMillis) {
        return DateUtils.formatDateTime(context, timeMillis,
                DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_NO_YEAR);
    }

    public static String toMonthString(Context context, long timeMillis) {
        return DateUtils.formatDateRange(context, timeMillis, timeMillis,
                DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_NO_MONTH_DAY |
                        DateUtils.FORMAT_SHOW_YEAR);
    }
}
