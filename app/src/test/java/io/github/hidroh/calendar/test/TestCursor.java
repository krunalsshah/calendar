package io.github.hidroh.calendar.test;

import android.database.ContentObserver;
import android.database.MatrixCursor;

import io.github.hidroh.calendar.content.EventsQueryHandler;

public class TestCursor extends MatrixCursor {
    private ContentObserver contentObserver;

    public TestCursor() {
        super(EventsQueryHandler.PROJECTION);
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
