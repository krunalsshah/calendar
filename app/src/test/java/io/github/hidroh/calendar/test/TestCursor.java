package io.github.hidroh.calendar.test;

import android.database.ContentObserver;
import android.database.MatrixCursor;
import android.provider.CalendarContract;

public class TestCursor extends MatrixCursor {
    private ContentObserver contentObserver;

    public TestCursor() {
        super(new String[]{CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY});
    }

    @Override
    public void addRow(Object[] columnValues) {
        super.addRow(columnValues);
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        contentObserver = observer;
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        contentObserver = null;
    }

    public void notifyContentChange(boolean selfChange) {
        if (contentObserver != null) {
            contentObserver.onChange(selfChange);
        }
    }

    public boolean hasContentObserver() {
        return contentObserver != null;
    }
}
