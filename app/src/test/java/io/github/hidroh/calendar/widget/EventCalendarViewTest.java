package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.assertj.android.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import java.util.Calendar;

import io.github.hidroh.calendar.CalendarDate;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.test.shadows.ShadowViewPager;

import static io.github.hidroh.calendar.test.assertions.CalendarAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Config(shadows = ShadowViewPager.class)
@RunWith(RobolectricGradleTestRunner.class)
public class EventCalendarViewTest {
    private ActivityController<TestActivity> controller;
    private EventCalendarView calendarView;
    private ShadowViewPager shadowCalendarView;
    private final Calendar nowCalendar = Calendar.getInstance();

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
        Assertions.assertThat(calendarView).hasChildCount(3);
        Assertions.assertThat(calendarView.getChildAt(0)).isInstanceOf(MonthView.class);
        assertThat(getCalendarAt(2))
                .isInSameMonthAs(nowCalendar)
                .isMonthsAfter(getCalendarAt(1), 1)
                .isMonthsBefore(getCalendarAt(3), 1);
    }

    @Test
    public void testShiftLeft() {
        // initial state
        Calendar expectedCalendar = Calendar.getInstance();
        int actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(2);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar);

        // swipe left, no shifting
        expectedCalendar.add(Calendar.MONTH, 1);
        shadowCalendarView.swipeLeft();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(3);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar)
                .isMonthsBefore(getCalendarAt(actual + 1), 1)
                .isMonthsAfter(getCalendarAt(actual - 1), 1);

        // swipe left, reach the end, should shift left to front
        expectedCalendar.add(Calendar.MONTH, 1);
        shadowCalendarView.swipeLeft();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(1);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar)
                .isMonthsBefore(getCalendarAt(actual + 1), 1)
                .isMonthsAfter(getCalendarAt(actual - 1), 1);
    }

    @Test
    public void testShiftRight() {
        // initial state
        Calendar expectedCalendar = Calendar.getInstance();
        int actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(2);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar);

        // swipe right, no shifting
        expectedCalendar.add(Calendar.MONTH, -1);
        shadowCalendarView.swipeRight();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(1);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar)
                .isMonthsBefore(getCalendarAt(actual + 1), 1)
                .isMonthsAfter(getCalendarAt(actual - 1), 1);

        // swipe right, reach the end, should shift right to end
        expectedCalendar.add(Calendar.MONTH, -1);
        shadowCalendarView.swipeRight();
        actual = calendarView.getCurrentItem();
        assertThat(actual).isEqualTo(3);
        assertThat(getCalendarAt(actual))
                .isInSameMonthAs(expectedCalendar)
                .isMonthsBefore(getCalendarAt(actual + 1), 1)
                .isMonthsAfter(getCalendarAt(actual - 1), 1);
    }

    @Test
    public void testChangeListener() {
        EventCalendarView.OnChangeListener listener = mock(EventCalendarView.OnChangeListener.class);
        calendarView.setOnChangeListener(listener);

        // swiping to change page, should generate notification
        shadowCalendarView.swipeLeft();
        verify(listener).onSelectedDayChange(any(CalendarDate.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private Calendar getCalendarAt(int position) {
        return ((MonthView) calendarView.getAdapter()
                .instantiateItem(calendarView, position)).mCalendarDate;
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
}
