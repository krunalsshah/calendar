package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.test.TestEventCursor;
import io.github.hidroh.calendar.test.shadows.ShadowViewPager;

import static io.github.hidroh.calendar.test.assertions.DayTimeAssert.assertThat;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ConstantConditions")
@Config(shadows = ShadowViewPager.class)
@RunWith(RobolectricGradleTestRunner.class)
public class EventCalendarViewTest {
    private ActivityController<TestActivity> controller;
    private EventCalendarView calendarView;
    private ShadowViewPager shadowCalendarView;
    private long todayMillis = CalendarUtils.today();

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = controller.create().start().resume().visible().get();
        calendarView = (EventCalendarView) activity.findViewById(R.id.calendar_view);
        shadowCalendarView = (ShadowViewPager) ShadowExtractor.extract(calendarView);
    }

    @Test
    public void testMonthData() {
        // initial state: 1 active, 2 hidden and 2 uninitialized
        assertThat(calendarView).hasChildCount(3);
        assertThat(calendarView.getChildAt(0)).isInstanceOf(MonthView.class);
        assertThat(getMonthAt(2))
                .isInSameMonthAs(todayMillis)
                .isMonthsAfter(getMonthAt(1), 1)
                .isMonthsBefore(getMonthAt(3), 1);
    }

    @Test
    public void testSwipeLeftChangeMonth() {
        // initial state
        long expected = CalendarUtils.today();
        int actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(2);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected);

        // swipe left, no shifting
        // changing month by swiping should automatically set selected day to 1st day
        expected = CalendarUtils.addMonths(expected, 1);
        shadowCalendarView.swipeLeft();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(3);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected)
                .isMonthsBefore(getMonthAt(actual + 1), 1)
                .isMonthsAfter(getMonthAt(actual - 1), 1)
                .isFirstDayOf(expected);

        // swipe left, reach the end, should shift left to front
        expected = CalendarUtils.addMonths(expected, 1);
        shadowCalendarView.swipeLeft();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(1);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected)
                .isMonthsBefore(getMonthAt(actual + 1), 1)
                .isMonthsAfter(getMonthAt(actual - 1), 1);
    }

    @Test
    public void testSwipeRightChangeMonth() {
        // initial state
        long expected = CalendarUtils.today();
        int actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(2);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected);

        // swipe right, no shifting
        // changing month by swiping should automatically set selected day to 1st day
        expected = CalendarUtils.addMonths(expected, -1);
        shadowCalendarView.swipeRight();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(1);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected)
                .isMonthsBefore(getMonthAt(actual + 1), 1)
                .isMonthsAfter(getMonthAt(actual - 1), 1)
                .isFirstDayOf(expected);

        // swipe right, reach the end, should shift right to end
        expected = CalendarUtils.addMonths(expected, -1);
        shadowCalendarView.swipeRight();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(3);
        assertThat(getMonthAt(actual))
                .isInSameMonthAs(expected)
                .isMonthsBefore(getMonthAt(actual + 1), 1)
                .isMonthsAfter(getMonthAt(actual - 1), 1);
    }

    @Test
    public void testChangeActiveMonthSelectedDay() {
        // setting day in same month should not change page
        long firstDay = CalendarUtils.monthFirstDay(todayMillis);
        calendarView.setSelectedDay(firstDay);
        assertThat(getMonthAt(calendarView.getCurrentItem()))
                .isInSameMonthAs(todayMillis);

        // setting day in same month should not change page
        long lastDay = CalendarUtils.monthLastDay(todayMillis);
        calendarView.setSelectedDay(lastDay);
        assertThat(getMonthAt(calendarView.getCurrentItem()))
                .isInSameMonthAs(todayMillis);

        // setting day in same month should not change page
        long middleDay = firstDay + DateUtils.DAY_IN_MILLIS * 15;
        calendarView.setSelectedDay(middleDay);
        assertThat(getMonthAt(calendarView.getCurrentItem()))
                .isInSameMonthAs(todayMillis);
    }

    @Test
    public void testChangeSelectedDayToPreviousMonth() {
        long middleDayPrevMonth = CalendarUtils.addMonths(
                CalendarUtils.monthFirstDay(todayMillis), -1) + DateUtils.DAY_IN_MILLIS * 15;

        // setting day in previous month should swipe to left page
        // changing month programmatically should NOT automatically set selected day to 1st day
        calendarView.setSelectedDay(middleDayPrevMonth);
        assertThat(getSelectedDay())
                .isInSameMonthAs(middleDayPrevMonth)
                .isNotFirstDayOf(middleDayPrevMonth);
    }

    @Test
    public void testChangeSelectedDayToNextMonth() {
        long middleDayNextMonth = CalendarUtils.addMonths(
                CalendarUtils.monthFirstDay(todayMillis), 1) + DateUtils.DAY_IN_MILLIS * 15;

        // setting day in next month should swipe to right page
        // changing month programmatically should NOT automatically set selected day to 1st day
        calendarView.setSelectedDay(middleDayNextMonth);
        assertThat(getSelectedDay())
                .isInSameMonthAs(middleDayNextMonth)
                .isNotFirstDayOf(middleDayNextMonth);
    }

    @Test
    public void testNotifyListener() {
        EventCalendarView.OnChangeListener listener = mock(EventCalendarView.OnChangeListener.class);
        calendarView.setOnChangeListener(listener);

        // swiping to change page, should generate notification
        shadowCalendarView.swipeLeft();
        verify(listener).onSelectedDayChange(anyLong());

        // changing month programmatically, should not generate notification
        calendarView.setSelectedDay(todayMillis);
        verify(listener).onSelectedDayChange(anyLong());

        // TODO test changing day from month view, should generate notification
    }

    @Test
    public void testBindCursor() {
        // setting calendar adapter should load and bind cursor
        TestEventCursor cursor = new TestEventCursor();
        cursor.addRow(new Object[]{1L, 1L, "Event 1", todayMillis, todayMillis, 0});
        TestCalendarAdapter testAdapter = new TestCalendarAdapter();
        testAdapter.cursor = cursor;
        calendarView.setCalendarAdapter(testAdapter);
        assertThat(cursor).isNotClosed();
        assertTrue(cursor.hasContentObserver());

        // deactivating should close cursor and unregister content observer
        calendarView.deactivate();
        assertThat(cursor).isClosed();
        assertFalse(cursor.hasContentObserver());
    }

    @Test
    public void testCursorContentChange() {
        // setting calendar adapter should load and bind cursor
        TestEventCursor cursor = new TestEventCursor();
        cursor.addRow(new Object[]{1L, 1L, "Event 1", todayMillis, todayMillis, 0});
        TestCalendarAdapter testAdapter = new TestCalendarAdapter();
        testAdapter.cursor = cursor;
        calendarView.setCalendarAdapter(testAdapter);
        assertThat(cursor).isNotClosed();
        assertTrue(cursor.hasContentObserver());

        // content change should load and bind new cursor, deactivate existing cursor
        TestEventCursor updatedCursor = new TestEventCursor();
        testAdapter.cursor = updatedCursor;
        cursor.notifyContentChange(false);
        assertThat(cursor).isClosed();
        assertFalse(cursor.hasContentObserver());
        assertThat(updatedCursor).isNotClosed();
        assertTrue(updatedCursor.hasContentObserver());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private long getMonthAt(int position) {
        return ((MonthViewPagerAdapter) calendarView.getAdapter())
                .mViews.get(position).mMonthMillis;
    }

    private long getSelectedDay() {
        return ((MonthViewPagerAdapter) calendarView.getAdapter()).mSelectedDayMillis;
    }

    static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EventCalendarView calendarView = new EventCalendarView(this);
            calendarView.setId(R.id.calendar_view);
            calendarView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(calendarView);
        }
    }

    static class TestCalendarAdapter extends EventCalendarView.CalendarAdapter {
        TestEventCursor cursor = new TestEventCursor();

        @Override
        protected void loadEvents(long monthMillis) {
            bindEvents(monthMillis, cursor);
        }
    }
}
