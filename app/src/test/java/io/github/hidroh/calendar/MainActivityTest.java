package io.github.hidroh.calendar;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CheckedTextView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import java.util.Calendar;

import io.github.hidroh.calendar.test.shadows.ShadowLinearLayoutManager;
import io.github.hidroh.calendar.test.shadows.ShadowRecyclerView;
import io.github.hidroh.calendar.test.shadows.ShadowViewPager;
import io.github.hidroh.calendar.widget.AgendaView;
import io.github.hidroh.calendar.widget.EventCalendarView;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowViewPager.class, ShadowRecyclerView.class, ShadowLinearLayoutManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private CheckedTextView toggle;
    private EventCalendarView calendarView;
    private AgendaView agendaView;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(MainActivity.class);
        controller.create().start().postCreate(null).resume();
        activity = controller.get();
        toggle = (CheckedTextView) activity.findViewById(R.id.toolbar_toggle);
        calendarView = (EventCalendarView) activity.findViewById(R.id.calendar_view);
        agendaView = (AgendaView) activity.findViewById(R.id.agenda_view);
    }

    @Test
    public void testToolbarToggle() {
        // initial state
        assertToggleOff();
        assertThat(toggle).hasTextString(CalendarUtils.toMonthString(activity,
                CalendarDate.today().getTimeInMillis()));

        // toggle on
        toggle.performClick();
        assertToggleOn();

        // toggle off
        toggle.performClick();
        assertToggleOff();
    }

    @Test
    public void testCalendarViewDayChange() {
        CalendarDate firstDayNextMonth = CalendarDate.today();
        firstDayNextMonth.add(Calendar.MONTH, 1);
        firstDayNextMonth.set(Calendar.DAY_OF_MONTH, 1);

        // initial state
        assertTitle(CalendarDate.today());
        assertAgendaViewTopDay(CalendarDate.today());

        // swipe calendar view left, should update title and scroll agenda view
        ((ShadowViewPager) ShadowExtractor.extract(calendarView)).swipeLeft();
        assertTitle(firstDayNextMonth);
        assertAgendaViewTopDay(firstDayNextMonth);
    }

    @Test
    public void testAgendaViewDayChange() {
        CalendarDate prevMonth = CalendarDate.today();
        prevMonth.add(Calendar.MONTH, -1);

        // initial state
        int initialCalendarPage = calendarView.getCurrentItem();
        assertTitle(CalendarDate.today());

        // scroll agenda view to top, should update title and swipe calendar view right
        agendaView.smoothScrollToPosition(0);
        assertTitle(prevMonth);
        assertThat(calendarView.getCurrentItem()).isEqualTo(initialCalendarPage - 1);
    }

    @Test
    public void testStateRestoration() {
        // initial state
        assertToggleOff();

        // recreate
        shadowOf(activity).recreate();
        assertToggleOff();

        // toggle on
        toggle = (CheckedTextView) activity.findViewById(R.id.toolbar_toggle);
        toggle.performClick();
        assertToggleOn();

        // recreate
        shadowOf(activity).recreate();
        assertToggleOn();
    }

    @Test
    public void testOptionsItemBack() {
        assertThat(activity).isNotFinishing();
        activity.onCreateOptionsMenu(new RoboMenu(activity));
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertTitle(CalendarDate calendarDate) {
        assertThat(toggle).hasTextString(CalendarUtils.toMonthString(activity,
                calendarDate.getTimeInMillis()));
    }

    private void assertAgendaViewTopDay(CalendarDate topDay) {
        int topPosition = ((LinearLayoutManager) agendaView.getLayoutManager())
                .findFirstVisibleItemPosition();
        RecyclerView.ViewHolder viewHolder = agendaView.getAdapter()
                .createViewHolder(agendaView, topPosition);
        //noinspection unchecked
        agendaView.getAdapter().bindViewHolder(viewHolder, topPosition);
        assertThat((TextView) viewHolder.itemView)
                .hasTextString(CalendarUtils.toDayString(activity,
                        topDay.getTimeInMillis()));
    }

    private void assertToggleOn() {
        assertThat(toggle).isChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isVisible();
    }

    private void assertToggleOff() {
        assertThat(toggle).isNotChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isNotVisible();
    }
}
