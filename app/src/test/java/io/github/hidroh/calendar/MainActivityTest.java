package io.github.hidroh.calendar;

import android.content.ShadowAsyncQueryHandler;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
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
import org.robolectric.fakes.RoboCursor;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ActivityController;

import java.util.Arrays;

import io.github.hidroh.calendar.test.shadows.ShadowLinearLayoutManager;
import io.github.hidroh.calendar.test.shadows.ShadowRecyclerView;
import io.github.hidroh.calendar.test.shadows.ShadowViewPager;
import io.github.hidroh.calendar.widget.AgendaView;
import io.github.hidroh.calendar.widget.EventCalendarView;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("unchecked")
@Config(shadows = {ShadowViewPager.class, ShadowRecyclerView.class, ShadowLinearLayoutManager.class, ShadowAsyncQueryHandler.class})
@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityTest {
    private ActivityController<TestMainActivity> controller;
    private MainActivity activity;
    private CheckedTextView toggle;
    private EventCalendarView calendarView;
    private AgendaView agendaView;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestMainActivity.class);
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
                CalendarUtils.today()));

        // toggle on
        toggle.performClick();
        assertToggleOn();

        // toggle off
        toggle.performClick();
        assertToggleOff();
    }

    @Test
    public void testCalendarViewDayChange() {
        long firstDayNextMonth = CalendarUtils.addMonths(CalendarUtils.monthFirstDay(
                CalendarUtils.today()), 1);

        // initial state
        assertTitle(CalendarUtils.today());
        assertAgendaViewTopDay(CalendarUtils.today());

        // swipe calendar view left, should update title and scroll agenda view
        ((ShadowViewPager) ShadowExtractor.extract(calendarView)).swipeLeft();
        assertTitle(firstDayNextMonth);
        assertAgendaViewTopDay(firstDayNextMonth);
    }

    @Test
    public void testAgendaViewDayChange() {
        long topAgendaMonth = CalendarUtils.addMonths(CalendarUtils.today(), -2);

        // initial state
        int initialCalendarPage = calendarView.getCurrentItem();
        assertTitle(CalendarUtils.today());

        // scroll agenda view to top, should update title and swipe calendar view right
        agendaView.smoothScrollToPosition(0);
        assertTitle(topAgendaMonth);
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
        //noinspection ConstantConditions
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

    @Test
    public void testQueryDay() {
        RoboCursor cursor = new TestRoboCursor();
        cursor.setResults(new Object[][]{
                new Object[]{"Event 1", CalendarUtils.today()}
        });
        shadowOf(ShadowApplication.getInstance().getContentResolver())
                .setCursor(CalendarContract.Events.CONTENT_URI, cursor);

        // trigger loading from provider
        agendaView.getAdapter().bindViewHolder(agendaView.getAdapter()
                .createViewHolder(agendaView, 0), 0);

        // binding from provider should replace placeholder
        RecyclerView.ViewHolder viewHolder = agendaView.getAdapter()
                .createViewHolder(agendaView, 1);
        agendaView.getAdapter().bindViewHolder(viewHolder, 1);
        assertThat((TextView) viewHolder.itemView).hasTextString("Event 1");
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertTitle(long dayMillis) {
        assertThat(toggle).hasTextString(CalendarUtils.toMonthString(activity,
                dayMillis));
    }

    private void assertAgendaViewTopDay(long topDayMillis) {
        int topPosition = ((LinearLayoutManager) agendaView.getLayoutManager())
                .findFirstVisibleItemPosition();
        RecyclerView.ViewHolder viewHolder = agendaView.getAdapter()
                .createViewHolder(agendaView, topPosition);
        agendaView.getAdapter().bindViewHolder(viewHolder, topPosition);
        assertThat((TextView) viewHolder.itemView)
                .hasTextString(CalendarUtils.toDayString(activity,
                        topDayMillis));
    }

    private void assertToggleOn() {
        assertThat(toggle).isChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isVisible();
    }

    private void assertToggleOff() {
        assertThat(toggle).isNotChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isNotVisible();
    }

    static class TestMainActivity extends MainActivity {
        int permissionCheckResult = PackageManager.PERMISSION_GRANTED;
        @Override
        protected int checkPermission(@NonNull String permission) {
            return permissionCheckResult;
        }
    }

    static class TestRoboCursor extends RoboCursor {
        public TestRoboCursor() {
            setColumnNames(Arrays.asList(CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART));
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
    }
}
