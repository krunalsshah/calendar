package io.github.hidroh.calendar.content;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.provider.CalendarContract;

import io.github.hidroh.calendar.CalendarUtils;

public abstract class EventsQueryHandler extends AsyncQueryHandler {

    public static final String[] PROJECTION = new String[]{
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
    };
    private static final String SORT = CalendarContract.Events.DTSTART + " ASC";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String INT_TRUE = "1";
    private static final String INT_FALSE = "0";
    private static final String ALL_DAY = CalendarContract.Events.ALL_DAY + "=?";
    private static final String DELETED = CalendarContract.Events.DELETED + "=?";
    // select events that starts within query range
    private static final String START_WITHIN = "(" +
            CalendarContract.Events.DTSTART + ">=?" + AND +
            CalendarContract.Events.DTSTART + "<?" +
            ")";
    // select events that starts before but end within or after query range
    private static final String START_BEF_END_WITHIN_AFTER = "(" +
            CalendarContract.Events.DTSTART + "<?" + AND +
            CalendarContract.Events.DTEND + ">?" +
            ")";
    // select non all-day events
    private static final String SELECTION_NON_ALL_DAY_EVENTS = "(" +
            ALL_DAY + AND +
            "(" + START_WITHIN + OR + START_BEF_END_WITHIN_AFTER + ")" +
            ")";
    // select all-day events
    private static final String SELECTION_ALL_DAY_EVENTS = "(" +
            ALL_DAY + AND +
            "(" + START_WITHIN + OR + START_BEF_END_WITHIN_AFTER + ")" +
            ")";
    // select non-deleted events from either set
    private static final String SELECTION = DELETED + AND +
            "(" + SELECTION_NON_ALL_DAY_EVENTS + OR + SELECTION_ALL_DAY_EVENTS + ")";

    public EventsQueryHandler(ContentResolver cr) {
        super(cr);
    }

    public final void startQuery(Object cookie, long startTimeMillis, long endTimeMillis) {
        String utcStart = String.valueOf(CalendarUtils.toUtcTimeZone(startTimeMillis)),
                utcEnd = String.valueOf(CalendarUtils.toUtcTimeZone(endTimeMillis)),
                localStart = String.valueOf(startTimeMillis),
                localEnd = String.valueOf(endTimeMillis);
        String[] args = new String[]{
                INT_FALSE, // not deleted
                INT_FALSE, // not all day
                localStart,
                localEnd,
                localStart,
                localStart,
                INT_TRUE, // all day
                utcStart,
                utcEnd,
                utcStart,
                utcStart
        };
        startQuery(0, cookie, CalendarContract.Events.CONTENT_URI, PROJECTION, SELECTION, args, SORT);
    }
}
