package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import io.github.hidroh.calendar.CalendarDate;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.test.shadows.ShadowViewHolder;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
@Config(shadows = ShadowViewHolder.class)
@RunWith(RobolectricGradleTestRunner.class)
public class MonthViewTest {
    private ActivityController<TestActivity> controller;
    private MonthView monthView;
    private RecyclerView.Adapter adapter;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = controller.create().start().resume().visible().get();
        monthView = (MonthView) activity.findViewById(R.id.calendar_view);
        adapter = monthView.getAdapter();
        CalendarDate calendar = CalendarDate.today();
        calendar.set(2016, Calendar.MARCH, 1);
        monthView.setCalendar(calendar);
        // 7 header cells + 2 carried days from Feb + 31 days in March
        assertThat(adapter.getItemCount()).isEqualTo(7 + 31 + 2);
    }

    @Test
    public void testHeader() {
        RecyclerView.ViewHolder viewHolder = createBindViewHolder(0);
        assertThat(viewHolder.itemView).isInstanceOf(TextView.class);
        assertThat((TextView) viewHolder.itemView)
                .hasTextString(DateFormatSymbols.getInstance().getShortWeekdays()[Calendar.SUNDAY]);
    }

    @Test
    public void testEmptyContent() {
        RecyclerView.ViewHolder viewHolder = createBindViewHolder(7); // carried over from Feb
        assertThat(viewHolder.itemView).isInstanceOf(TextView.class);
        assertThat((TextView) viewHolder.itemView).isEmpty();
    }

    @Test
    public void testContent() {
        RecyclerView.ViewHolder viewHolder = createBindViewHolder(9); // 01-March-2016
        assertThat(viewHolder.itemView).isInstanceOf(TextView.class);
        assertThat((TextView) viewHolder.itemView).isNotEmpty();
    }

    @Test
    public void testDaySelectionChange() {
        MonthView.OnDateChangeListener listener = mock(MonthView.OnDateChangeListener.class);
        monthView.setOnDateChangeListener(listener);

        // clear selection
        monthView.setSelectedDay(null);
        verify(listener, never()).onSelectedDayChange(any(CalendarDate.class));

        // new selection outside current month, not triggered by users
        CalendarDate selection = CalendarDate.today();
        selection.set(2016, Calendar.APRIL, 1);
        monthView.setSelectedDay(selection);
        verify(listener, never()).onSelectedDayChange(any(CalendarDate.class));

        // new selection inside current month, not triggered by users
        selection.set(2016, Calendar.MARCH, 1);
        monthView.setSelectedDay(selection);
        verify(listener, never()).onSelectedDayChange(any(CalendarDate.class));

        // change selection via UI interaction, triggered by users
        RecyclerView.ViewHolder viewHolder = createBindViewHolder(10); // 02-March-2016
        viewHolder.itemView.performClick();
        verify(listener).onSelectedDayChange(any(CalendarDate.class));

        // change selection via UI interaction, triggered by users
        viewHolder = createBindViewHolder(11); // 03-March-2016
        viewHolder.itemView.performClick();
        verify(listener, times(2)).onSelectedDayChange(any(CalendarDate.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private RecyclerView.ViewHolder createBindViewHolder(int position) {
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(monthView,
                adapter.getItemViewType(position));
        ((ShadowViewHolder) ShadowExtractor.extract(viewHolder)).setAdapterPosition(position);
        adapter.bindViewHolder(viewHolder, position);
        return viewHolder;
    }

    static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            MonthView view = new MonthView(this);
            view.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            view.setId(R.id.calendar_view);
            setContentView(view);
        }
    }
}
