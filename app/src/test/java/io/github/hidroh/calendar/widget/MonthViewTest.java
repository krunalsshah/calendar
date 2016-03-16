package io.github.hidroh.calendar.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
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

import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.test.TestCursor;
import io.github.hidroh.calendar.test.shadows.ShadowViewHolder;
import io.github.hidroh.calendar.text.style.CircleSpan;
import io.github.hidroh.calendar.text.style.UnderDotSpan;

import static io.github.hidroh.calendar.test.assertions.SpannableStringAssert.assertThat;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
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
    private MonthView.GridAdapter adapter;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        TestActivity activity = controller.create().start().resume().visible().get();
        monthView = (MonthView) activity.findViewById(R.id.calendar_view);
        //noinspection ConstantConditions
        adapter = (MonthView.GridAdapter) monthView.getAdapter();
        monthView.setCalendar(createDayMillis(2016, Calendar.MARCH, 1));
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
        monthView.setSelectedDay(CalendarUtils.NO_TIME_MILLIS);
        verify(listener, never()).onSelectedDayChange(anyLong());

        // new selection outside current month, not triggered by users
        long selection = createDayMillis(2016, Calendar.APRIL, 1);
        monthView.setSelectedDay(selection);
        verify(listener, never()).onSelectedDayChange(anyLong());

        // new selection inside current month, not triggered by users
        selection = createDayMillis(2016, Calendar.MARCH, 1);
        monthView.setSelectedDay(selection);
        verify(listener, never()).onSelectedDayChange(anyLong());

        // change selection via UI interaction, triggered by users
        RecyclerView.ViewHolder viewHolder = createBindViewHolder(10); // 02-March-2016
        viewHolder.itemView.performClick();
        verify(listener).onSelectedDayChange(anyLong());

        // change selection via UI interaction, triggered by users
        viewHolder = createBindViewHolder(11); // 03-March-2016
        viewHolder.itemView.performClick();
        verify(listener, times(2)).onSelectedDayChange(anyLong());
    }

    @Test
    public void testBindSelectedDay() {
        // initial state
        CharSequence actual = ((TextView) createBindViewHolder(10).itemView)
                .getText(); // 02-March-2016
        assertThat(actual).isInstanceOf(SpannableString.class);
        assertThat((SpannableString) actual).doesNotHaveSpan(CircleSpan.class);

        // selecting day should circle it
        monthView.setSelectedDay(createDayMillis(2016, Calendar.MARCH, 2));
        actual = ((TextView) createBindViewHolder(10).itemView).getText(); // 02-March-2016
        assertThat(actual).isInstanceOf(SpannableString.class);
        assertThat((SpannableString) actual).hasSpan(CircleSpan.class);
    }

    @Test
    public void testSwapCursor() {
        TestCursor cursor = new TestCursor();
        long day14 = createDayMillis(2016, Calendar.MARCH, 14),
                day15 = createDayMillis(2016, Calendar.MARCH, 15),
                day17 = createDayMillis(2016, Calendar.MARCH, 17),
                day20 = createDayMillis(2016, Calendar.MARCH, 20),
                day21 = createDayMillis(2016, Calendar.MARCH, 21);
        cursor.addRow(new Object[]{"Event 1", day14, day17, 0}); // multi day
        cursor.addRow(new Object[]{"Event 1", day15, day15, 0}); // single day
        cursor.addRow(new Object[]{"Event 1", day20, day21, 1}); // all day
        monthView.swapCursor(cursor);
        assertThat(adapter.mEvents)
                .hasSize(5)
                .contains(13, 14, 15, 16, 19);

        // swapping the same cursor should not alter bound events
        monthView.swapCursor(cursor);
        assertThat(adapter.mEvents)
                .hasSize(5)
                .contains(13, 14, 15, 16, 19);

        // swapping new cursor should rebind existing events
        TestCursor updatedCursor = new TestCursor();
        updatedCursor.addRow(new Object[]{"Event 1", day20, day21, 1}); // all day
        monthView.swapCursor(updatedCursor);
        assertThat(adapter.mEvents)
                .hasSize(1)
                .contains(19);

        // swapping empty cursor should clear all existing events
        TestCursor emptyCursor = new TestCursor();
        monthView.swapCursor(emptyCursor);
        assertThat(adapter.mEvents).isEmpty();
    }

    @Test
    public void testBindCursor() {
        // initial state
        CharSequence actual = ((TextView) createBindViewHolder(10).itemView)
                .getText(); // 02-March-2016
        assertThat(actual).isInstanceOf(SpannableString.class);
        assertThat((SpannableString) actual).doesNotHaveSpan(UnderDotSpan.class);

        // swapping cursor should decorate it
        TestCursor cursor = new TestCursor();
        long day2 = createDayMillis(2016, Calendar.MARCH, 2);
        cursor.addRow(new Object[]{"Event 1", day2, day2, 0});
        monthView.swapCursor(cursor);
        actual = ((TextView) createBindViewHolder(10).itemView).getText(); // 02-March-2016
        assertThat(actual).isInstanceOf(SpannableString.class);
        assertThat((SpannableString) actual).hasSpan(UnderDotSpan.class);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private RecyclerView.ViewHolder createBindViewHolder(int position) {
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(monthView,
                adapter.getItemViewType(position));
        ((ShadowViewHolder) ShadowExtractor.extract(viewHolder)).adapterPosition = position;
        adapter.bindViewHolder((MonthView.CellViewHolder) viewHolder, position);
        return viewHolder;
    }

    private long createDayMillis(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTimeInMillis();
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
