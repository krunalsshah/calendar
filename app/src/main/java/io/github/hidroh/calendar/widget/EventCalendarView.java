package io.github.hidroh.calendar.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.calendar.CalendarUtils;

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
                public void onSelectedDayChange(long dayMillis) {
                    // this should come from a page, only notify its neighbors
                    mAdapter.setSelectedDay(getCurrentItem(), dayMillis, false);
                    notifyDayChange(dayMillis);
                }
            };
    private final MonthViewPagerAdapter mAdapter = new MonthViewPagerAdapter(mDateChangeListener);
    private OnChangeListener mListener;

    /**
     * Callback interface for calendar view change events
     */
    public interface OnChangeListener {
        /**
         * Fired when selected day has been changed via UI interaction
         * @param dayMillis    selected day in milliseconds
         */
        void onSelectedDayChange(long dayMillis);
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
     * TODO assume that min left month < selectedDay < max right month
     * @param dayMillis   new selected day in milliseconds
     */
    public void setSelectedDay(long dayMillis) {
        // notify active page and its neighbors
        int position = getCurrentItem();
        if (CalendarUtils.monthBefore(dayMillis, mAdapter.mSelectedDayMillis)) {
            mAdapter.setSelectedDay(position - 1, dayMillis, true);
            setCurrentItem(position - 1, true);
        } else if (CalendarUtils.monthAfter(dayMillis, mAdapter.mSelectedDayMillis)) {
            mAdapter.setSelectedDay(position + 1, dayMillis, true);
            setCurrentItem(position + 1, true);
        } else {
            mAdapter.setSelectedDay(position, dayMillis, true);
        }
    }

    private void init() {
        setAdapter(mAdapter);
        setCurrentItem(mAdapter.getCount() / 2);
        addOnPageChangeListener(new SimpleOnPageChangeListener() {
            public boolean mDragging = false; // indicate if page change is from user

            @Override
            public void onPageSelected(int position) {
                if (mDragging) {
                    // sequence: IDLE -> (DRAGGING) -> SETTLING -> onPageSelected -> IDLE
                    // ensures that this will always be triggered before syncPages() for position
                    toFirstDay(position);
                    notifyDayChange(mAdapter.getMonth(position));
                }
                mDragging = false;
                // trigger same scroll state changed logic, which would not be fired if not visible
                if (getVisibility() != VISIBLE) {
                    onPageScrollStateChanged(SCROLL_STATE_IDLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    syncPages(getCurrentItem());
                } else if (state == SCROLL_STATE_DRAGGING) {
                    mDragging = true;
                }
            }
        });
    }

    private void toFirstDay(int position) {
        mAdapter.setSelectedDay(position, CalendarUtils.monthFirstDay(mAdapter.getMonth(position)), true);
    }

    private void notifyDayChange(long dayMillis) {
        if (mListener != null) {
            mListener.onSelectedDayChange(dayMillis);
        }
    }

    /**
     * shift and recycle pages if we are currently at last or first,
     * ensure that users can peek hidden pages on 2 sides
     * @param position  current item position
     */
    private void syncPages(int position) {
        int first = 0, last = mAdapter.getCount() - 1;
        if (position == last) {
            mAdapter.shiftLeft();
            setCurrentItem(first + 1, false);
        } else if (position == 0) {
            mAdapter.shiftRight();
            setCurrentItem(last - 1, false);
        } else {
            // rebind neighbours due to shifting
            if (position > 0) {
                mAdapter.bind(position - 1);
            }
            if (position < mAdapter.getCount() - 1) {
                mAdapter.bind(position + 1);
            }
        }
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
        private static final String STATE_FIRST_MONTH_MILLIS = "state:month";
        private static final String STATE_SELECTED_DAY_MILLIS = "state:selectedDay";
        static final int ITEM_COUNT = 5; // buffer, left, active, right, buffer

        @VisibleForTesting final List<MonthView> mViews = new ArrayList<>(getCount());
        @VisibleForTesting long mSelectedDayMillis = CalendarUtils.today();
        private final List<Long> mMonths = new ArrayList<>(getCount());
        private final MonthView.OnDateChangeListener mListener;

        public MonthViewPagerAdapter(MonthView.OnDateChangeListener listener) {
            mListener = listener;
            int mid = ITEM_COUNT / 2;
            long todayMillis = CalendarUtils.monthFirstDay(CalendarUtils.today());
            for (int i = 0; i < getCount(); i++) {
                mMonths.add(CalendarUtils.addMonths(todayMillis, i - mid));
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
            bundle.putLong(STATE_FIRST_MONTH_MILLIS, mMonths.get(0));
            bundle.putLong(STATE_SELECTED_DAY_MILLIS, mSelectedDayMillis);
            return bundle;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            Bundle savedState = (Bundle) state;
            if (savedState == null) {
                return;
            }
            mSelectedDayMillis = savedState.getLong(STATE_SELECTED_DAY_MILLIS);
            long firstMonthMillis = savedState.getInt(STATE_FIRST_MONTH_MILLIS);
            for (int i = 0; i < getCount(); i++) {
                mMonths.set(i, CalendarUtils.addMonths(firstMonthMillis, i));
            }
        }

        /**
         * Sets selected day for page at given position, which either sets new selected day
         * if it falls within that month, or unsets previously selected day if any
         * @param position      page position
         * @param dayMillis     selected day in milliseconds
         * @param notifySelf    true to rebind page at given position, false otherwise
         */
        void setSelectedDay(int position, long dayMillis, boolean notifySelf) {
            mSelectedDayMillis = dayMillis;
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

        long getMonth(int position) {
            return mMonths.get(position);
        }

        /**
         * shift Jan, Feb, Mar, Apr, [May] to Apr, [May], Jun, Jul, Aug
         * rebind views in view pool if needed
         */
        void shiftLeft() {
            for (int i = 0; i < getCount() - 2; i++) {
                mMonths.add(CalendarUtils.addMonths(mMonths.remove(0), getCount()));
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
                mMonths.add(0, CalendarUtils.addMonths(mMonths.remove(getCount() - 1), -getCount()));
            }
            // rebind current item (2nd to last) and 2 adjacent items
            for (int i = 0; i <= 2; i++) {
                bind(getCount() - 1 - i);
            }
        }

        void bind(int position) {
            if (mViews.get(position) != null) {
                mViews.get(position).setCalendar(mMonths.get(position));
            }
            bindSelectedDay(position);
        }

        private void bindSelectedDay(int position) {
            if (mViews.get(position) != null) {
                mViews.get(position).setSelectedDay(mSelectedDayMillis);
            }
        }
    }
}
