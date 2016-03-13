package io.github.hidroh.calendar.test.shadows;

import android.support.v7.widget.LinearLayoutManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(LinearLayoutManager.class)
public class ShadowLinearLayoutManager {
    private static final int TOTAL_VISIBLE = 10; // assume we can layout 10 items on screen
    private int firstVisiblePosition = 0;
    private int lastVisiblePosition = TOTAL_VISIBLE - 1;

    @Implementation
    public void scrollToPosition(int position) {
        firstVisiblePosition = position;
        lastVisiblePosition = position + TOTAL_VISIBLE - 1;
    }

    @Implementation
    public int findFirstVisibleItemPosition() {
        return firstVisiblePosition;
    }


    @Implementation
    public int findLastVisibleItemPosition() {
        return lastVisiblePosition;
    }

    public void scrollToLastPosition(int position) {
        lastVisiblePosition = position;
        firstVisiblePosition = position - TOTAL_VISIBLE + 1;
    }
}
