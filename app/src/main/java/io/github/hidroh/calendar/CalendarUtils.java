package io.github.hidroh.calendar;

import android.content.Context;
import android.support.v4.util.Pools;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class CalendarUtils {

    public static final long NO_TIME_MILLIS = -1;

    public static boolean isNotTime(long timeMillis) {
        return timeMillis == NO_TIME_MILLIS;
    }

    public static long today() {
        CalendarDate calendar = CalendarDate.today();
        long timeMillis = calendar.getTimeInMillis();
        calendar.recycle();
        return timeMillis;
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

    public static String toTimeString(Context context, long timeMillis) {
        return DateUtils.formatDateTime(context, timeMillis, DateUtils.FORMAT_SHOW_TIME);
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean sameMonth(long first, long second) {
        if (isNotTime(first) || isNotTime(second)) {
            return false; // not comparable
        }
        CalendarDate firstCalendar = CalendarDate.fromTime(first);
        CalendarDate secondCalendar = CalendarDate.fromTime(second);
        boolean same = firstCalendar.sameMonth(secondCalendar);
        firstCalendar.recycle();
        secondCalendar.recycle();
        return same;
    }

    public static int dayOfMonth(long timeMillis) {
        if (isNotTime(timeMillis)) {
            return -1;
        }
        CalendarDate calendar = CalendarDate.fromTime(timeMillis);
        //noinspection ConstantConditions
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.recycle();
        return day;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean monthBefore(long first, long second) {
        if (isNotTime(first) || isNotTime(second)) {
            return false;
        }
        CalendarDate firstCalendar = CalendarDate.fromTime(first);
        CalendarDate secondCalendar = CalendarDate.fromTime(second);
        boolean before = firstCalendar.monthBefore(secondCalendar);
        firstCalendar.recycle();
        secondCalendar.recycle();
        return before;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean monthAfter(long first, long second) {
        if (isNotTime(first) || isNotTime(second)) {
            return false;
        }
        CalendarDate firstCalendar = CalendarDate.fromTime(first);
        CalendarDate secondCalendar = CalendarDate.fromTime(second);
        boolean after = firstCalendar.monthAfter(secondCalendar);
        firstCalendar.recycle();
        secondCalendar.recycle();
        return after;
    }

    public static long addMonths(long timeMillis, int months) {
        if (isNotTime(timeMillis)) {
            return NO_TIME_MILLIS;
        }
        CalendarDate calendar = CalendarDate.fromTime(timeMillis);
        //noinspection ConstantConditions
        calendar.add(Calendar.MONTH, months);
        long result = calendar.getTimeInMillis();
        calendar.recycle();
        return result;
    }

    public static long monthFirstDay(long monthMillis) {
        if (isNotTime(monthMillis)) {
            return NO_TIME_MILLIS;
        }
        CalendarDate calendar = CalendarDate.fromTime(monthMillis);
        //noinspection ConstantConditions
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long result = calendar.getTimeInMillis();
        calendar.recycle();
        return result;
    }

    public static long monthLastDay(long monthMillis) {
        if (isNotTime(monthMillis)) {
            return NO_TIME_MILLIS;
        }
        CalendarDate calendar = CalendarDate.fromTime(monthMillis);
        //noinspection ConstantConditions
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        long result = calendar.getTimeInMillis();
        calendar.recycle();
        return result;
    }

    public static int monthSize(long monthMillis) {
        if (isNotTime(monthMillis)) {
            return 0;
        }
        CalendarDate calendar = CalendarDate.fromTime(monthMillis);
        //noinspection ConstantConditions
        int size = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.recycle();
        return size;
    }

    public static int monthFirstDayOffset(long monthMillis) {
        if (isNotTime(monthMillis)) {
            return 0;
        }
        CalendarDate calendar = CalendarDate.fromTime(monthMillis);
        //noinspection ConstantConditions
        int offset = calendar.get(Calendar.DAY_OF_WEEK) - calendar.getFirstDayOfWeek();
        calendar.recycle();
        return offset;
    }

    /**
     * Light extension of {@link CalendarDate} that strips away time, keeping only date information
     */
    private static class CalendarDate extends GregorianCalendar {

        private static Pools.SimplePool<CalendarDate> sPools = new Pools.SimplePool<>(5);

        private static CalendarDate obtain() {
            CalendarDate instance = sPools.acquire();
            return instance == null ? new CalendarDate() : instance;
        }

        public static CalendarDate today() {
            return fromTime(System.currentTimeMillis());
        }

        public static CalendarDate fromTime(long timeMillis) {
            if (timeMillis < 0) {
                return null;
            }
            CalendarDate calendarDate = CalendarDate.obtain();
            calendarDate.setTimeInMillis(timeMillis);
            calendarDate.stripTime();
            return calendarDate;
        }

        private CalendarDate() {
            super();
        }

        /**
         * Checks if this instance falls in some month before given instance
         * @param other    instance to check against
         * @return  true if this instance is at least 1 'month' before, false otherwise
         */
        public boolean monthBefore(CalendarDate other) {
            int day = other.get(DAY_OF_MONTH);
            other.set(DAY_OF_MONTH, 1);
            boolean before = getTimeInMillis() < other.getTimeInMillis();
            other.set(DAY_OF_MONTH, day);
            return before;
        }

        /**
         * Checks if this instance falls in some month after given instance
         * @param other    instance to check against
         * @return  true if this instance is at least 1 'month' after, false otherwise
         */
        public boolean monthAfter(CalendarDate other) {
            int day = other.get(DAY_OF_MONTH);
            other.set(DAY_OF_MONTH, other.getActualMaximum(DAY_OF_MONTH));
            boolean after = getTimeInMillis() > other.getTimeInMillis();
            other.set(DAY_OF_MONTH, day);
            return after;
        }

        public boolean sameMonth(CalendarDate other) {
            return get(YEAR) == other.get(YEAR) && get(MONTH) == other.get(MONTH);
        }

        void stripTime() {
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }

        void recycle() {
            sPools.release(this);
        }
    }
}
