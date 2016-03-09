package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import io.github.hidroh.calendar.CalendarDate;
import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.test.shadows.ShadowLinearLayoutManager;
import io.github.hidroh.calendar.test.shadows.ShadowRecyclerView;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Config(shadows = {ShadowRecyclerView.class, ShadowLinearLayoutManager.class})
@SuppressWarnings({"unchecked", "ConstantConditions"})
@RunWith(RobolectricGradleTestRunner.class)
public class AgendaViewTest {
    private ActivityController<TestActivity> controller;
    private AgendaView agendaView;
    private RecyclerView.Adapter adapter;
    private final long todayMillis = CalendarDate.today().getTimeInMillis();
    private final long firstDayMillis = todayMillis - DateUtils.DAY_IN_MILLIS * 31;
    private final long lastDayMillis = todayMillis + DateUtils.DAY_IN_MILLIS * 31 * 2 - 1;
    private LinearLayoutManager layoutManager;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = controller.create().start().resume().visible().get();
        agendaView = (AgendaView) activity.findViewById(R.id.agenda_view);
        adapter = agendaView.getAdapter();
        layoutManager = (LinearLayoutManager) agendaView.getLayoutManager();
    }

    @Test
    public void testInitialLayout() {
        // initial layout should have 3 blocks of 31 days
        assertThat(adapter.getItemCount()).isEqualTo(31 * 2 * 3);
        // first visible item should not be first item to allow user to scroll up
        int topPosition = layoutManager.findFirstVisibleItemPosition();
        assertThat(topPosition).isGreaterThan(0);
        // first visible item should be today by default
        assertHasDate(createBindViewHolder(topPosition), todayMillis);
        assertThat((TextView) createBindViewHolder(topPosition + 1).itemView)
                .hasText(R.string.no_event);
    }

    @Test
    public void testPrepend() {
        assertHasDate(createBindViewHolder(0), firstDayMillis);
        assertHasDate(createBindViewHolder(0), firstDayMillis - DateUtils.DAY_IN_MILLIS * 31);
    }

    @Test
    public void testAppend() {
        int lastPosition = adapter.getItemCount() - 1;
        assertHasDate(createBindViewHolder(lastPosition - 1), lastDayMillis);
        createBindViewHolder(lastPosition);
        assertHasDate(createBindViewHolder(lastPosition - 1),
                lastDayMillis + DateUtils.DAY_IN_MILLIS * 31);
    }

    @Test
    public void testChangeSelectedDay() {
        long tomorrowMillis = todayMillis + DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(CalendarDate.fromTime(tomorrowMillis));
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                tomorrowMillis);
    }

    @Test
    public void testPrependSelectedDay() {
        long beforeFirstDayMillis = firstDayMillis - DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(CalendarDate.fromTime(beforeFirstDayMillis));
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                beforeFirstDayMillis);
        assertHasDate(createBindViewHolder(0), firstDayMillis - DateUtils.DAY_IN_MILLIS * 31);
    }

    @Test
    public void testAppendSelectedDay() {
        long afterLastDayMillis = lastDayMillis + DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(CalendarDate.fromTime(afterLastDayMillis));
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                afterLastDayMillis);
        assertHasDate(createBindViewHolder(adapter.getItemCount() - 2),
                lastDayMillis + DateUtils.DAY_IN_MILLIS * 31);
    }

    @Test
    public void testDayChangeListener() {
        AgendaView.OnDateChangeListener listener = mock(AgendaView.OnDateChangeListener.class);
        agendaView.setOnDateChangeListener(listener);

        // set day programmatically should not trigger listener
        agendaView.setSelectedDay(CalendarDate.fromTime(todayMillis + DateUtils.DAY_IN_MILLIS));
        verify(listener, never()).onSelectedDayChange(any(CalendarDate.class));

        // set day via scrolling should trigger listener
        agendaView.smoothScrollToPosition(0);
        verify(listener).onSelectedDayChange(any(CalendarDate.class));

        // scroll to an item of same last selected date should not trigger listener
        agendaView.smoothScrollToPosition(1);
        verify(listener).onSelectedDayChange(any(CalendarDate.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertHasDate(RecyclerView.ViewHolder viewHolder, long timeMillis) {
        assertThat((TextView) viewHolder.itemView)
                .hasTextString(CalendarUtils.toDayString(RuntimeEnvironment.application, timeMillis));
    }

    private RecyclerView.ViewHolder createBindViewHolder(int position) {
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(agendaView,
                adapter.getItemViewType(position));
        adapter.bindViewHolder(viewHolder, position);
        return viewHolder;
    }

    static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            AgendaView agendaView = new AgendaView(this);
            agendaView.setId(R.id.agenda_view);
            agendaView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(agendaView);
        }
    }
}
