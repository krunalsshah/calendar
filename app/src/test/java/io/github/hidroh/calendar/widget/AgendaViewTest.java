package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
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
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.EditActivity;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.content.EventCursor;
import io.github.hidroh.calendar.test.TestEventCursor;
import io.github.hidroh.calendar.test.shadows.ShadowLinearLayoutManager;
import io.github.hidroh.calendar.test.shadows.ShadowRecyclerView;
import io.github.hidroh.calendar.weather.Weather;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerView.class, ShadowLinearLayoutManager.class})
@SuppressWarnings({"unchecked", "ConstantConditions"})
@RunWith(RobolectricGradleTestRunner.class)
public class AgendaViewTest {
    private ActivityController<TestActivity> controller;
    private TestActivity activity;
    private AgendaView agendaView;
    private AgendaAdapter adapter;
    private final long todayMillis = CalendarUtils.today();
    private final long firstDayMillis = todayMillis -
            DateUtils.DAY_IN_MILLIS * AgendaAdapter.BLOCK_SIZE;
    private final long lastDayMillis = todayMillis +
            DateUtils.DAY_IN_MILLIS * (AgendaAdapter.BLOCK_SIZE - 1);
    private LinearLayoutManager layoutManager;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        activity = controller.create().start().resume().visible().get();
        agendaView = (AgendaView) activity.findViewById(R.id.agenda_view);
        adapter = (AgendaAdapter) agendaView.getAdapter();
        layoutManager = (LinearLayoutManager) agendaView.getLayoutManager();
    }

    @Test
    public void testInitialLayout() {
        // initial layout should have 2 blocks of BLOCK_SIZE (each with group + placeholder)
        assertThat(adapter.getItemCount()).isEqualTo(AgendaAdapter.BLOCK_SIZE * 2 * 2);
        // first visible item should be today by default
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasText(R.string.no_event);
    }

    @Test
    public void testPrepend() {
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.smoothScrollToPosition(0);
        assertHasDate(createBindViewHolder(0), firstDayMillis -
                DateUtils.DAY_IN_MILLIS * AgendaAdapter.BLOCK_SIZE);
    }

    @Test
    public void testAppend() {
        int lastPosition = adapter.getItemCount() - 1;
        createBindViewHolder(lastPosition);
        assertHasDate(createBindViewHolder(lastPosition - 1), lastDayMillis);
        ((ShadowRecyclerView) ShadowExtractor.extract(agendaView))
                .scrollToLastPosition();
        lastPosition = adapter.getItemCount() - 1;
        assertHasDate(createBindViewHolder(lastPosition - 1),
                lastDayMillis + DateUtils.DAY_IN_MILLIS * AgendaAdapter.BLOCK_SIZE);
    }

    @Test
    public void testPruneUponPrepending() {
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        agendaView.smoothScrollToPosition(0);
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        agendaView.smoothScrollToPosition(0);
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        agendaView.smoothScrollToPosition(0);
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
    }

    @Test
    public void testPruneUponAppending() {
        ShadowRecyclerView shadowAgendaView =
                (ShadowRecyclerView) ShadowExtractor.extract(agendaView);
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        shadowAgendaView.scrollToLastPosition();
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        shadowAgendaView.scrollToLastPosition();
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
        shadowAgendaView.scrollToLastPosition();
        assertThat(adapter.getItemCount()).isLessThanOrEqualTo(AgendaAdapter.MAX_SIZE * 2);
    }

    @Test
    public void testChangeSelectedDay() {
        long tomorrowMillis = todayMillis + DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(tomorrowMillis);
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                tomorrowMillis);
    }

    @Test
    public void testPrependSelectedDay() {
        long beforeFirstDayMillis = firstDayMillis - DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(beforeFirstDayMillis);
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                beforeFirstDayMillis);
        assertHasDate(createBindViewHolder(0), firstDayMillis -
                DateUtils.DAY_IN_MILLIS * AgendaAdapter.BLOCK_SIZE);
    }

    @Test
    public void testAppendSelectedDay() {
        long afterLastDayMillis = lastDayMillis + DateUtils.DAY_IN_MILLIS;
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                todayMillis);
        agendaView.setSelectedDay(afterLastDayMillis);
        assertHasDate(createBindViewHolder(layoutManager.findFirstVisibleItemPosition()),
                afterLastDayMillis);
        assertHasDate(createBindViewHolder(adapter.getItemCount() - 2),
                lastDayMillis + DateUtils.DAY_IN_MILLIS * AgendaAdapter.BLOCK_SIZE);
    }

    @Test
    public void testDayChangeListener() {
        AgendaView.OnDateChangeListener listener = mock(AgendaView.OnDateChangeListener.class);
        agendaView.setOnDateChangeListener(listener);

        // set day programmatically should not trigger listener
        agendaView.setSelectedDay(todayMillis + DateUtils.DAY_IN_MILLIS);
        verify(listener, never()).onSelectedDayChange(anyLong());

        // set day via scrolling should trigger listener
        agendaView.smoothScrollToPosition(1);
        verify(listener).onSelectedDayChange(anyLong());

        // scroll to an item of same last selected date should not trigger listener
        agendaView.smoothScrollToPosition(1);
        verify(listener).onSelectedDayChange(anyLong());
    }

    @Test
    public void testBindEmptyCursor() {
        // initial state
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString(R.string.no_event);

        // bind empty cursor should not replace placeholder
        TestEventCursor cursor = new TestEventCursor();
        adapter.bindEvents(todayMillis, cursor);
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString(R.string.no_event);
    }

    @Test
    public void testBindCursor() {
        // initial state
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString(R.string.no_event);

        // trigger cursor loading and binding
        long groupTime = firstDayMillis;
        TestEventCursor cursor = new TestEventCursor();
        cursor.addRow(new Object[]{1L, 1L, "Event 1", groupTime + 28800000, groupTime + 28800000, 0}); // 8AM UTC
        cursor.addRow(new Object[]{1L, 1L, "Event 2", groupTime, groupTime, 1}); // all day
        cursor.addRow(new Object[]{1L, 1L, "Event 3",
                groupTime -  DateUtils.DAY_IN_MILLIS * 2, groupTime, 0}); // multi day, end today
        cursor.addRow(new Object[]{1L, 1L, "Event 4", groupTime -  DateUtils.DAY_IN_MILLIS,
                groupTime + DateUtils.DAY_IN_MILLIS, 0}); // multi day, end tomorrow
        activity.cursors.put(groupTime, cursor);
        createBindViewHolder(0);

        // non empty cursor should replace placeholder and add extra item
        View item1 = createBindViewHolder(1).itemView;
        assertThat((TextView) item1.findViewById(R.id.text_view_time))
                .hasTextString(CalendarUtils.toTimeString(activity, groupTime + 28800000));
        assertThat((TextView) item1.findViewById(R.id.text_view_title))
                .hasTextString("Event 1");
        View item2 = createBindViewHolder(2).itemView;
        assertThat((TextView) item2.findViewById(R.id.text_view_time))
                .hasTextString(R.string.all_day);
        assertThat((TextView) item2.findViewById(R.id.text_view_title))
                .hasTextString("Event 2");
        View item3 = createBindViewHolder(3).itemView;
        assertThat((TextView) item3.findViewById(R.id.text_view_time))
                .hasTextString(activity.getString(R.string.end_time,
                        CalendarUtils.toTimeString(activity, groupTime)));
        assertThat((TextView) item3.findViewById(R.id.text_view_title))
                .hasTextString("Event 3");
        View item4 = createBindViewHolder(4).itemView;
        assertThat((TextView) item4.findViewById(R.id.text_view_time))
                .hasTextString(R.string.all_day);
        assertThat((TextView) item4.findViewById(R.id.text_view_title))
                .hasTextString("Event 4");

        // deactivate adapter should close cursor
        assertThat(cursor).isNotClosed();
        adapter.deactivate();
        assertThat(cursor).isClosed();
    }

    @Test
    public void testCursorContentChange() {
        // initial state
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString(R.string.no_event);

        // trigger cursor loading and binding
        long groupTime = firstDayMillis;
        TestEventCursor noEventCursor = new TestEventCursor();
        activity.cursors.put(groupTime, noEventCursor);
        createBindViewHolder(0);

        // bind empty cursor should not replace placeholder
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString(R.string.no_event);

        // trigger content change notification
        TestEventCursor multiEventCursor = new TestEventCursor();
        multiEventCursor.addRow(new Object[]{1L, 1L, "Event 1", groupTime + 1000, groupTime + 1000, 0});
        multiEventCursor.addRow(new Object[]{1L, 1L, "Event 2", groupTime + 2000, groupTime + 2000, 0});
        activity.cursors.put(groupTime, multiEventCursor);
        noEventCursor.notifyContentChange(false);

        // content change should deactivate prev cursor, update placeholder and add extra item
        assertThat(noEventCursor).isClosed();
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString("Event 1");
        assertThat((TextView) createBindViewHolder(2).itemView.findViewById(R.id.text_view_title))
                .hasTextString("Event 2");

        // trigger content change notification
        TestEventCursor singleEventCursor = new TestEventCursor();
        singleEventCursor.addRow(new Object[]{1L, 1L, "Event 3", groupTime + 3000, groupTime + 3000, 0});
        activity.cursors.put(groupTime, singleEventCursor);
        multiEventCursor.notifyContentChange(false);

        // content change should deactivate prev cursor, update existing item, remove deleted item
        assertThat(multiEventCursor).isClosed();
        assertThat((TextView) createBindViewHolder(1).itemView.findViewById(R.id.text_view_title))
                .hasTextString("Event 3");
        assertHasDate(createBindViewHolder(2), groupTime + DateUtils.DAY_IN_MILLIS);
    }

    @Test
    public void testStateRestoration() {
        agendaView.smoothScrollToPosition(0);
        int expected = AgendaAdapter.MAX_SIZE * 2; // prepended
        assertThat(adapter.getItemCount()).isEqualTo(expected);
        Parcelable savedState = agendaView.onSaveInstanceState();
        agendaView.onRestoreInstanceState(savedState);
        AgendaAdapter newAdapter = new AgendaAdapter(activity) { };
        agendaView.setAdapter(newAdapter);
        assertThat(newAdapter.getItemCount()).isEqualTo(expected);
    }

    @Test
    public void testItemClick() {
        createBindViewHolder(1).itemView.performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, EditActivity.class)
                .hasExtra(EditActivity.EXTRA_EVENT);
    }

    @Test
    public void testBindEmptyWeather() {
        Weather weather = new Weather(new String[0], new String[0]);
        agendaView.setWeather(weather);
        int todayPosition = layoutManager.findFirstVisibleItemPosition();
        assertThat(createBindViewHolder(todayPosition)
                .itemView
                .findViewById(R.id.weather))
                .isNotVisible();
        assertThat(createBindViewHolder(todayPosition + 2)
                .itemView
                .findViewById(R.id.weather))
                .isNotVisible();
    }

    @Test
    public void testBindFullWeather() {
        Weather weather = new Weather(
                new String[]{"cloudy", "86.0", "cloudy", "86.0", "cloudy", "86.0"},
                new String[]{"cloudy", "86.0", "cloudy", "86.0", "cloudy", "86.0"});
        agendaView.setWeather(weather);
        int todayPosition = layoutManager.findFirstVisibleItemPosition();
        assertThat(createBindViewHolder(todayPosition)
                .itemView
                .findViewById(R.id.weather))
                .isVisible();
        assertThat(createBindViewHolder(todayPosition + 2)
                .itemView
                .findViewById(R.id.weather))
                .isVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertHasDate(RecyclerView.ViewHolder viewHolder, long timeMillis) {
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text_view_title))
                .hasTextString(CalendarUtils.toDayString(RuntimeEnvironment.application, timeMillis));
    }

    private RecyclerView.ViewHolder createBindViewHolder(int position) {
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(agendaView,
                adapter.getItemViewType(position));
        adapter.bindViewHolder((AgendaAdapter.RowViewHolder) viewHolder, position);
        return viewHolder;
    }

    static class TestActivity extends AppCompatActivity {
        LongSparseArray<EventCursor> cursors = new LongSparseArray<>();

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            AgendaView agendaView = new AgendaView(this);
            agendaView.setId(R.id.agenda_view);
            agendaView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(agendaView);
            agendaView.setAdapter(new AgendaAdapter(this) {
                @Override
                protected void loadEvents(long timeMillis) {
                    bindEvents(timeMillis, cursors.get(timeMillis) != null ?
                            cursors.get(timeMillis) : new TestEventCursor());
                }
            });
        }
    }

}
