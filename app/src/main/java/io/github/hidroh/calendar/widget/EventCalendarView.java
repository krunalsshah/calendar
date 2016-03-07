package io.github.hidroh.calendar.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.github.hidroh.calendar.CalendarDate;

/**
 * A custom CalendarDate View, in the form of circular {@link ViewPager}
 * that supports month change event and state restoration.
 *
 * The {@link ViewPager} recycles adapter item views as users scroll
 * to first or last item.
 */
public class EventCalendarView extends ViewPager {

    private final MonthView.OnDateChangeListener mDateChangeListener =
            new MonthView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(@NonNull CalendarDate calendarDate) {
                    mAdapter.setSelectedDay(getCurrentItem(), calendarDate, false);
                    if (mListener != null) {
                        mListener.onSelectedDayChange(calendarDate);
                    }
                }
            };
    private final MonthViewPagerAdapter mAdapter = new MonthViewPagerAdapter(mDateChangeListener);
    private OnChangeListener mListener;
    private int mPendingCurrentItem = -1; // represent current item to be set programmatically

    /**
     * Callback interface for calendar view change events
     */
    public interface OnChangeListener {
        /**
         * Fired when selected day has been changed via UI interaction
         * @param calendar    calendar object that represents selected day
         */
        void onSelectedDayChange(@NonNull CalendarDate calendar);
    }

    public EventCalendarView(Context context) {
        this(context, null);
    }

    public EventCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // make this ViewPager's height WRAP_CONTENT
        View child = mAdapter.mViews.get(getCurrentItem());
        if (child != null) {
            child.measure(widthMeasureSpec, heightMeasureSpec);
            int height = child.getMeasuredHeight();
            setMeasuredDimension(getMeasuredWidth(), height);
        }
    }

    /**
     * Sets listener to be notified upon calendar view change events
     * @param listener    listener to be notified
     */
    public void setOnChangeListener(OnChangeListener listener) {
        mListener = listener;
    }

    /**
     * Sets selected day, automatically move to next/previous month
     * if given day is not within active month
     * @param selectedDay   new selected day
     */
    public void setSelectedDay(@NonNull CalendarDate selectedDay) {
        // TODO assume here that min left month < selected day < max right month
        // TODO or the change to selected day would be triggered sequentially
        // TODO as agenda view is being scrolled (no flinging/skipping dates)
        int current = mAdapter.mSelectedDay.get(Calendar.DAY_OF_MONTH),
                first = 1,
                last = mAdapter.mSelectedDay.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (current == first && selectedDay.before(mAdapter.mSelectedDay)) {
            mPendingCurrentItem = getCurrentItem() - 1;
            setCurrentItem(mPendingCurrentItem, true);
        } else if (current == last && selectedDay.after(mAdapter.mSelectedDay)) {
            mPendingCurrentItem = getCurrentItem() + 1;
            setCurrentItem(mPendingCurrentItem, true);
        }
        mAdapter.setSelectedDay(getCurrentItem(), selectedDay, true);
    }

    private void init() {
        setAdapter(mAdapter);
        addOnPageChangeListener(new SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                CalendarDate calendar = mAdapter.getCalendar(position);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                mAdapter.setSelectedDay(position, calendar, true);
                // only notify listener if change is not triggered programmatically (i.e. no pending)
                if (mPendingCurrentItem == position) {
                    mPendingCurrentItem = -1; // clear pending
                } else if (mListener != null) {
                    mListener.onSelectedDayChange(calendar);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    // shift and recycle pages if we are currently at last or first,
                    // ensure that users can peek hidden pages on 2 sides
                    int position = getCurrentItem(), first = 0, last = mAdapter.getCount() - 1;
                    if (position == last) {
                        mAdapter.shiftLeft();
                        setCurrentItem(first + 1, false);
                    } else if (position == 0) {
                        mAdapter.shiftRight();
                        setCurrentItem(last - 1, false);
                    } else {
                        // rebind neighbours due to shifting
                        if (getCurrentItem() > 0) {
                            mAdapter.bind(getCurrentItem() - 1);
                        }
                        if (getCurrentItem() < mAdapter.getCount() - 1) {
                            mAdapter.bind(getCurrentItem() + 1);
                        }
                    }
                }
            }
        });
        setCurrentItem(mAdapter.getCount() / 2);
    }

    /**
     * A circular {@link PagerAdapter}, with a view pool of 5 items:
     * buffer, left, [active], right, buffer
     * Upon user scrolling to a buffer view, {@link ViewPager#setCurrentItem(int)}
     * should be called to wrap around and shift active view to the next non-buffer
     * @see #shiftLeft()
     * @see #shiftRight()
     */
    static class MonthViewPagerAdapter extends PagerAdapter {
        private static final String STATE_MONTH = "state:month";
        private static final String STATE_YEAR = "state:year";
        private static final String STATE_SELECTED_YEAR = "state:selectedYear";
        private static final String STATE_SELECTED_MONTH = "state:selectedMonth";
        private static final String STATE_SELECTED_DAY = "state:selectedDay";
        static final int ITEM_COUNT = 5; // buffer, left, active, right, buffer

        final List<MonthView> mViews = new ArrayList<>(getCount());
        final CalendarDate mSelectedDay = CalendarDate.today();
        private final List<CalendarDate> mCalendars = new ArrayList<>(getCount());
        private final MonthView.OnDateChangeListener mListener;

        public MonthViewPagerAdapter(MonthView.OnDateChangeListener listener) {
            mListener = listener;
            int mid = ITEM_COUNT / 2;
            for (int i = 0; i < getCount(); i++) {
                CalendarDate calendar = CalendarDate.today();
                calendar.add(Calendar.MONTH, i - mid);
                mCalendars.add(calendar);
                mViews.add(null);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            MonthView view = new MonthView(container.getContext());
            view.setLayoutParams(new ViewPager.LayoutParams());
            view.setOnDateChangeListener(mListener);
            mViews.set(position, view);
            container.addView(view); // views are not added in same order as adapter items
            bind(position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((MonthView) object).setOnDateChangeListener(null);
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return ITEM_COUNT;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            Bundle bundle = new Bundle();
            bundle.putInt(STATE_MONTH, mCalendars.get(0).get(Calendar.MONTH));
            bundle.putInt(STATE_YEAR, mCalendars.get(0).get(Calendar.YEAR));
            bundle.putInt(STATE_SELECTED_YEAR, mSelectedDay.get(Calendar.YEAR));
            bundle.putInt(STATE_SELECTED_MONTH, mSelectedDay.get(Calendar.MONTH));
            bundle.putInt(STATE_SELECTED_DAY, mSelectedDay.get(Calendar.DAY_OF_MONTH));
            return bundle;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            Bundle savedState = (Bundle) state;
            if (savedState == null) {
                return;
            }
            int month = savedState.getInt(STATE_MONTH),
                    year = savedState.getInt(STATE_YEAR),
                    selectedYear = savedState.getInt(STATE_SELECTED_YEAR),
                    selectedMonth = savedState.getInt(STATE_SELECTED_MONTH),
                    selectedDay = savedState.getInt(STATE_SELECTED_DAY);
            mSelectedDay.set(selectedYear, selectedMonth, selectedDay);
            for (int i = 0; i < getCount(); i++) {
                CalendarDate calendar = CalendarDate.today();
                calendar.set(year, month, 1);
                calendar.add(Calendar.MONTH, i);
                mCalendars.set(i, calendar);
            }
        }

        /**
         * Sets selected day for page at given position, which either sets new selected day
         * if it falls within that month, or unsets previously selected day if any
         * @param position       page position
         * @param selectedDay    selected day
         * @param notifySelf     true to rebind page at given position, false otherwise
         */
        void setSelectedDay(int position, @NonNull CalendarDate selectedDay, boolean notifySelf) {
            mSelectedDay.set(selectedDay.get(Calendar.YEAR), selectedDay.get(Calendar.MONTH),
                    selectedDay.get(Calendar.DAY_OF_MONTH));
            if (notifySelf) {
                bindSelectedDay(position);
            }
            if (position > 0) {
                bindSelectedDay(position - 1);
            }
            if (position < getCount() - 1) {
                bindSelectedDay(position + 1);
            }
        }

        CalendarDate getCalendar(int position) {
            return mCalendars.get(position);
        }

        /**
         * shift Jan, Feb, Mar, Apr, [May] to Apr, [May], Jun, Jul, Aug
         * rebind views in view pool if needed
         */
        void shiftLeft() {
            for (int i = 0; i < getCount() - 2; i++) {
                CalendarDate first = mCalendars.remove(0);
                first.add(Calendar.MONTH, getCount());
                mCalendars.add(first);
            }
            // rebind current item (2nd) and 2 adjacent items
            for (int i = 0; i <= 2; i++) {
                bind(i);
            }
        }

        /**
         * shift [Jan], Feb, Mar, Apr, May to Oct, Nov, Dec, [Jan], Feb
         * rebind views in view pool if needed
         */
        void shiftRight() {
            for (int i = 0; i < getCount() - 2; i++) {
                CalendarDate last = mCalendars.remove(getCount() - 1);
                last.add(Calendar.MONTH, -getCount());
                mCalendars.add(0, last);
            }
            // rebind current item (2nd to last) and 2 adjacent items
            for (int i = 0; i <= 2; i++) {
                bind(getCount() - 1 - i);
            }
        }

        void bind(int position) {
            if (mViews.get(position) != null) {
                mViews.get(position).setCalendar(mCalendars.get(position));
            }
            bindSelectedDay(position);
        }

        private void bindSelectedDay(int position) {
            if (mViews.get(position) != null) {
                mViews.get(position).setSelectedDay(mSelectedDay);
            }
        }
    }
}
