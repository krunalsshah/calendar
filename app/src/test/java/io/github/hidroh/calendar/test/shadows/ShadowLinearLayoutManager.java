package io.github.hidroh.calendar.test.shadows;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(LinearLayoutManager.class)
public class ShadowLinearLayoutManager {
    private int firstVisiblePosition = RecyclerView.NO_POSITION;

    @Implementation
    public void scrollToPosition(int position) {
        firstVisiblePosition = position;
    }

    @Implementation
    public int findFirstVisibleItemPosition() {
        return firstVisiblePosition;
    }
}
