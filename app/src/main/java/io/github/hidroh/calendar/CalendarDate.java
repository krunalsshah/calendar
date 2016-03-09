package io.github.hidroh.calendar;

import java.util.GregorianCalendar;

/**
 * Light extension of {@link CalendarDate} that strips away time, keeping only date information
 */
public class CalendarDate extends GregorianCalendar {

    public static CalendarDate today() {
        CalendarDate calendar = new CalendarDate();
        CalendarUtils.stripTime(calendar);
        return calendar;
    }

    public static CalendarDate fromTime(long timeMillis) {
        if (timeMillis < 0) {
            return null;
        }
        CalendarDate calendarDate = new CalendarDate();
        calendarDate.setTimeInMillis(timeMillis);
        CalendarUtils.stripTime(calendarDate);
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
}
