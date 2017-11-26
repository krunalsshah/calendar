package io.github.hidroh.calendar;

import android.annotation.SuppressLint;
import android.content.ShadowAsyncQueryHandler;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.CheckedTextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboCursor;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.calendar.content.CalendarCursor;
import io.github.hidroh.calendar.widget.CalendarSelectionView;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowAsyncQueryHandler.class})
@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityCalendarSelectionTest {

    private ActivityController<TestMainActivity> controller;
    private TestMainActivity activity;

    @Before
    public void setUp() {
        RoboCursor cursor = new TestRoboCursor();
        cursor.setResults(new Object[][]{
                new Object[]{1L, "My Calendar"},
                new Object[]{2L, "Birthdays"},
        });
        shadowOf(ShadowApplication.getInstance().getContentResolver())
                .setCursor(CalendarContract.Calendars.CONTENT_URI, cursor);
        controller = Robolectric.buildActivity(TestMainActivity.class);
        activity = controller.get();
    }

    @Test @Ignore
    public void testCalendarSelectionView() {
        // initial state: exclude calendar ID 2
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(CalendarUtils.PREF_CALENDAR_EXCLUSIONS, "2")
                .apply();
        controller.create().start().postCreate(null).resume();

        // calendar selection should have 1 checked, 2 unchecked
        CalendarSelectionView selectionView =
                (CalendarSelectionView) activity.findViewById(R.id.list_view_calendar);
        ResourceCursorAdapter adapter = (ResourceCursorAdapter) selectionView.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(2);
        assertTrue(selectionView.isItemChecked(0));
        assertFalse(selectionView.isItemChecked(1));

        // calendar selection view should bind calendar display name
        adapter.getCursor().moveToFirst();
        View itemView = adapter.newView(activity, adapter.getCursor(), selectionView);
        adapter.bindView(itemView, activity, adapter.getCursor());
        assertThat((CheckedTextView) itemView.findViewById(R.id.text_view_title))
                .hasTextString("My Calendar");
        controller.pause().stop().destroy();
    }

    @Test @Ignore
    public void testToggleCalendarSelection() {
        // initial state
        controller.create().start().postCreate(null).resume();
        CalendarSelectionView selectionView =
                (CalendarSelectionView) activity.findViewById(R.id.list_view_calendar);
        assertTrue(selectionView.isItemChecked(0));

        // clicking item should toggle its checked status
        shadowOf(selectionView).performItemClick(0);
        assertFalse(selectionView.isItemChecked(0));

        // clicking item should toggle its checked status
        shadowOf(selectionView).performItemClick(0);
        assertTrue(selectionView.isItemChecked(0));
        controller.pause().stop().destroy();
    }

    @Test @Ignore
    public void testPersistExclusions() {
        // initial state
        assertThat(PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(CalendarUtils.PREF_CALENDAR_EXCLUSIONS, null))
                .isNullOrEmpty();
        controller.create().start().postCreate(null).resume();

        // toggle off
        CalendarSelectionView selectionView =
                (CalendarSelectionView) activity.findViewById(R.id.list_view_calendar);
        shadowOf(selectionView).performItemClick(0);
        shadowOf(selectionView).performItemClick(1);

        // destroying activity should persist preference
        controller.pause().stop().destroy();
        assertThat(PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(CalendarUtils.PREF_CALENDAR_EXCLUSIONS, null))
                .contains("1")
                .contains(",")
                .contains("2");
    }

    @Test
    public void testCreateLocalCalendar() {
        shadowOf(ShadowApplication.getInstance().getContentResolver())
                .setCursor(CalendarContract.Calendars.CONTENT_URI, new TestRoboCursor());
        controller.create().start().postCreate(null).resume();
        List<ShadowContentResolver.InsertStatement> inserts =
                shadowOf(ShadowApplication.getInstance()
                        .getContentResolver())
                        .getInsertStatements();
        assertThat(inserts).hasSize(1);
        assertThat(inserts.get(0).getUri().toString())
                .contains(CalendarContract.Calendars.CONTENT_URI.toString());
        controller.pause().stop().destroy();
    }

    @After
    public void tearDown() {
    }

    @SuppressLint("Registered")
    static class TestMainActivity extends MainActivity {
        @Override
        protected boolean checkCalendarPermissions() {
            return true;
        }
    }

    static class TestRoboCursor extends RoboCursor {
        public TestRoboCursor() {
            setColumnNames(Arrays.asList(CalendarCursor.PROJECTION));
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            // no op
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            // no op
        }

        @Override
        public void registerContentObserver(ContentObserver observer) {
            // no op
        }

        @Override
        public void unregisterContentObserver(ContentObserver observer) {
            // no op
        }

        @Override
        public void setExtras(Bundle extras) {
            // no op
        }

        @Override
        public boolean isClosed() {
            return false;
        }
    }
}
