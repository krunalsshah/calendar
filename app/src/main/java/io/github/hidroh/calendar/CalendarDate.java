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
}
