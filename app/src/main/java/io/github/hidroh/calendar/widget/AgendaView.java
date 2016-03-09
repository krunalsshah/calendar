package io.github.hidroh.calendar.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.text.AllCapsTransformationMethod;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.calendar.CalendarDate;
import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.R;

public class AgendaView extends RecyclerView {
    private static final String STATE_VIEW = "state:view";
    private static final String STATE_ADAPTER = "state:adapter";
    private OnDateChangeListener mListener;
    private AgendaAdapter mAdapter;
    private final CalendarDate mSelectedDate = CalendarDate.today();
    // represent top scroll position to be set programmatically
    private int mPendingScrollPosition = NO_POSITION;
    private long mPrevTimeMillis = -1;

    /**
     * Callback interface for active (top) date change event
     */
    public interface OnDateChangeListener {
        /**
         * Fired when active (top) date has been changed via UI interaction
         * @param calendarDate  calendar object representing new active (top) day
         */
        void onSelectedDayChange(@NonNull CalendarDate calendarDate);
    }

    public AgendaView(Context context) {
        this(context, null);
    }

    public AgendaView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AgendaView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle outState = new Bundle();
        outState.putParcelable(STATE_VIEW, super.onSaveInstanceState());
        outState.putBundle(STATE_ADAPTER, mAdapter.saveState());
        return outState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedState = (Bundle) state;
        mAdapter.restoreState(savedState.getBundle(STATE_ADAPTER));
        super.onRestoreInstanceState(savedState.getParcelable(STATE_VIEW));
    }

    @Override
    public void onScrolled(int dx, int dy) {
        notifyDateChange();
    }

    /**
     * Sets listener to be notified when active (top) date in agenda changes
     * @param listener  listener to be notified
     */
    public void setOnDateChangeListener(OnDateChangeListener listener) {
        mListener = listener;
    }

    /**
     * Sets active (top) date to be displayed
     * @param calendarDate  calendar object representing new active (top) date
     */
    public void setSelectedDay(@NonNull CalendarDate calendarDate) {
        // TODO assume here that given date is at most 1 month from our range
        // TODO as it is triggered by a ViewPager swipe
        mPendingScrollPosition = mAdapter.getPosition(getContext(), calendarDate.getTimeInMillis());
        if (mPendingScrollPosition >= 0) {
            smoothScrollToPosition(mPendingScrollPosition);
        }
    }

    private void init() {
        setHasFixedSize(true);
        setLayoutManager(new AgendaLinearLayoutManager(getContext()));
        addItemDecoration(new DividerItemDecoration(getContext()));
        mAdapter = new AgendaAdapter(getContext());
        setAdapter(mAdapter);
        getLayoutManager().scrollToPosition(AgendaAdapter.MONTH_SIZE * 2); // start of current month
    }

    private void notifyDateChange() {
        int position = ((LinearLayoutManager) getLayoutManager())
                .findFirstVisibleItemPosition();
        if (position < 0) {
            return;
        }
        long timeMillis = mAdapter.getItem(position).timeMillis;
        if (mPrevTimeMillis != timeMillis) {
            mPrevTimeMillis = timeMillis;
            mSelectedDate.setTimeInMillis(timeMillis);
            // only notify listener if scroll is not triggered programmatically (i.e. no pending)
            if (mPendingScrollPosition == NO_POSITION && mListener != null) {
                mListener.onSelectedDayChange(mSelectedDate);
            }
        }
        if (mPendingScrollPosition == position) {
            mPendingScrollPosition = NO_POSITION; // clear pending
        }
    }

    /**
     * 'Unlimited' adapter that rotate and recycle blocks of items
     * as users scroll to top or bottom
     */
    static class AgendaAdapter extends RecyclerView.Adapter<RowViewHolder> {
        private static final String STATE_BASE_TIME_MILLIS = "state:baseTimeMillis";
        private static final String STATE_PREV_MONTH = "state:prevMonth";
        private static final String STATE_CURR_MONTH = "state:currMonth";
        private static final String STATE_NEXT_MONTH = "state:nextMonth";
        private static final int MONTH_SIZE = 31;
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_CONTENT = 1;

        private ArrayList<AgendaItem> mCurrMonth = new ArrayList<>(MONTH_SIZE * 2),
                mPrevMonth = new ArrayList<>(MONTH_SIZE * 2),
                mNextMonth = new ArrayList<>(MONTH_SIZE * 2);
        private long mBaseTimeMillis; // start day of middle block
        private final LayoutInflater mInflater;
        private final Handler mHandler = new Handler();

        public AgendaAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            mBaseTimeMillis = CalendarDate.today().getTimeInMillis();
            generate(context, mPrevMonth, -MONTH_SIZE);
            generate(context, mCurrMonth, 0);
            generate(context, mNextMonth, MONTH_SIZE);
        }

        @Override
        public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    return new HeaderViewHolder(mInflater.inflate(R.layout.list_item_header,
                            parent, false));
                case VIEW_TYPE_CONTENT:
                default:
                    return new ContentViewHolder(mInflater.inflate(R.layout.list_item_content,
                            parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RowViewHolder holder, int position) {
            holder.textView.setText(getItem(position).title);
            if (position == 0) {
                postPrepend(holder.textView.getContext());
            } else if (position == getItemCount() - 1) {
                postAppend(holder.textView.getContext());
            }
        }

        @Override
        public int getItemCount() {
            return mPrevMonth.size() + mCurrMonth.size() + mNextMonth.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof AgendaHeader) {
                return VIEW_TYPE_HEADER;
            } else { // AgendaEvent
                return VIEW_TYPE_CONTENT;
            }
        }

        Bundle saveState() {
            Bundle outState = new Bundle();
            outState.putLong(STATE_BASE_TIME_MILLIS, mBaseTimeMillis);
            outState.putParcelableArrayList(STATE_PREV_MONTH, mPrevMonth);
            outState.putParcelableArrayList(STATE_CURR_MONTH, mCurrMonth);
            outState.putParcelableArrayList(STATE_NEXT_MONTH, mNextMonth);
            return outState;
        }

        void restoreState(Bundle savedState) {
            if (savedState == null) {
                return;
            }
            mBaseTimeMillis = savedState.getLong(STATE_BASE_TIME_MILLIS);
            mPrevMonth = savedState.getParcelableArrayList(STATE_PREV_MONTH);
            mCurrMonth = savedState.getParcelableArrayList(STATE_CURR_MONTH);
            mNextMonth = savedState.getParcelableArrayList(STATE_NEXT_MONTH);
        }

        int getPosition(Context context, long timeMillis) {
            int start, end;
            if (timeMillis < getItem(0).timeMillis) {
                prepend(context);
                start = 0;
                end = mPrevMonth.size();
            } else if (timeMillis > getItem(getItemCount() - 1).timeMillis) {
                append(context);
                start = mPrevMonth.size() + mCurrMonth.size();
                end = getItemCount();
            } else {
                start = 0;
                end = getItemCount();
            }
            // TODO improve searching
            for (int i = start; i < end; i++) {
                if (getItem(i).timeMillis == timeMillis) {
                    return i;
                }
            }
            return NO_POSITION;
        }

        private AgendaItem getItem(int position) {
            if (position < mPrevMonth.size()) {
                return mPrevMonth.get(position);
            }
            if (position < mPrevMonth.size() + mCurrMonth.size()) {
                return mCurrMonth.get(position - mPrevMonth.size());
            }
            return mNextMonth.get(position - mCurrMonth.size() - mPrevMonth.size());
        }

        private void postPrepend(final Context context) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    prepend(context);
                }
            });
        }

        /**
         * Rotate [Feb, Mar, Apr*] to [Jan*, Feb, Mar]
         */
        private void prepend(Context context) {
            notifyItemRangeRemoved(mPrevMonth.size() + mCurrMonth.size(), mNextMonth.size());
            ArrayList<AgendaItem> prepended = mNextMonth;
            mNextMonth = mCurrMonth;
            mCurrMonth = mPrevMonth;
            mPrevMonth = prepended;
            mBaseTimeMillis -= DateUtils.DAY_IN_MILLIS * MONTH_SIZE;
            generate(context, prepended, -MONTH_SIZE);
            notifyItemRangeInserted(0, prepended.size());
        }

        private void postAppend(final Context context) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    append(context);
                }
            });
        }

        /**
         * Rotate [Feb*, Mar, Apr] to [Mar, Apr, May*]
         */
        private void append(Context context) {
            notifyItemRangeRemoved(0, mPrevMonth.size());
            ArrayList<AgendaItem> appended = mPrevMonth;
            mPrevMonth = mCurrMonth;
            mCurrMonth = mNextMonth;
            mNextMonth = appended;
            mBaseTimeMillis += DateUtils.DAY_IN_MILLIS * MONTH_SIZE;
            generate(context, appended, MONTH_SIZE);
            notifyItemRangeInserted(mPrevMonth.size() + mCurrMonth.size(), appended.size());
        }

        private void generate(Context context, List<AgendaItem> list, int offset) {
            list.clear();
            for (int i = offset; i < offset + MONTH_SIZE; i++) {
                long timeMillis = mBaseTimeMillis + DateUtils.DAY_IN_MILLIS * i;
                list.add(new AgendaHeader(context, timeMillis));
                list.add(new AgendaEvent(context, timeMillis));
            }
        }
    }

    static abstract class RowViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        public RowViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    static class HeaderViewHolder extends RowViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
            textView.setTransformationMethod(
                    new AllCapsTransformationMethod(textView.getContext()));
        }
    }

    static class ContentViewHolder extends RowViewHolder {

        public ContentViewHolder(View itemView) {
            super(itemView);
        }
    }

    static abstract class AgendaItem implements Parcelable {
        final String title;
        final long timeMillis;

        AgendaItem(String title, long timeMillis) {
            this.title = title;
            this.timeMillis = timeMillis;
        }

        AgendaItem(Parcel source) {
            title = source.readString();
            timeMillis = source.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeLong(timeMillis);
        }
    }

    static class AgendaHeader extends AgendaItem {
        public static Creator<AgendaHeader> CREATOR = new Creator<AgendaHeader>() {
            @Override
            public AgendaHeader createFromParcel(Parcel source) {
                return new AgendaHeader(source);
            }

            @Override
            public AgendaHeader[] newArray(int size) {
                return new AgendaHeader[size];
            }
        };

        public AgendaHeader(Context context, long timeMillis) {
            super(CalendarUtils.toDayString(context, timeMillis), timeMillis);
        }

        private AgendaHeader(Parcel source) {
            super(source);
        }
    }

    static class AgendaEvent extends AgendaItem {
        public static Creator<AgendaEvent> CREATOR = new Creator<AgendaEvent>() {
            @Override
            public AgendaEvent createFromParcel(Parcel source) {
                return new AgendaEvent(source);
            }

            @Override
            public AgendaEvent[] newArray(int size) {
                return new AgendaEvent[size];
            }
        };


        public AgendaEvent(Context context, long timeMillis) {
            super(context.getString(R.string.no_event), timeMillis);
        }

        private AgendaEvent(Parcel source) {
            super(source);
        }
    }

    static class DividerItemDecoration extends ItemDecoration {
        private final Paint mPaint;
        private final int mSize;

        public DividerItemDecoration(Context context) {
            mSize = context.getResources().getDimensionPixelSize(R.dimen.divider_size);
            mPaint = new Paint();
            mPaint.setColor(ContextCompat.getColor(context, R.color.colorDivider));
            mPaint.setStrokeWidth(mSize);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            int top, left = 0, right = parent.getMeasuredWidth();
            for (int i = 0; i < parent.getChildCount(); i++) {
                top = parent.getChildAt(i).getTop() - mSize / 2;
                c.drawLine(left, top, right, top, mPaint);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            if (parent.getChildAdapterPosition(view) > 0) {
                outRect.top = mSize;
            }
        }
    }

    /**
     * Light extension to {@link LinearLayoutManager} that overrides smooth scroller to
     * always snap to start
     */
    static class AgendaLinearLayoutManager extends LinearLayoutManager {

        public AgendaLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView,
                                           RecyclerView.State state,
                                           int position) {
            RecyclerView.SmoothScroller smoothScroller =
                    new LinearSmoothScroller(recyclerView.getContext()) {
                        @Override
                        public PointF computeScrollVectorForPosition(int targetPosition) {
                            return AgendaLinearLayoutManager.this
                                    .computeScrollVectorForPosition(targetPosition);
                        }

                        @Override
                        protected int getVerticalSnapPreference() {
                            return SNAP_TO_START; // override base class behavior
                        }
                    };
            smoothScroller.setTargetPosition(position);
            startSmoothScroll(smoothScroller);
        }
    }
}
