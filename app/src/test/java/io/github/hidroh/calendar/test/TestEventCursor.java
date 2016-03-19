package io.github.hidroh.calendar.test;

import android.database.ContentObserver;
import android.database.MatrixCursor;

import io.github.hidroh.calendar.content.EventCursor;

public class TestEventCursor extends EventCursor {
    private ContentObserver contentObserver;

    public TestEventCursor() {
        super(new MatrixCursor(EventCursor.PROJECTION));
    }

    public void addRow(Object[] columnValues) {
        ((MatrixCursor) getWrappedCursor()).addRow(columnValues);
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
