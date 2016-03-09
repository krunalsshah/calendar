package io.github.hidroh.calendar.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import io.github.hidroh.calendar.CalendarDate;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.text.style.CircleSpan;

/**
 * Custom widget to display a grid of days in a month, represented by a {@link Calendar}
 */
class MonthView extends RecyclerView {
    private static final int SPANS_COUNT = 7; // days in week
    private final CalendarDate mSelectedDay = CalendarDate.today();
    @VisibleForTesting CalendarDate mCalendarDate;
    private EventAdapter mAdapter;
    private OnDateChangeListener mListener;

    /**
     * Callback interface for date selection events
     */
    interface OnDateChangeListener {
        /**
         * Fired when a new selection has been made via UI interaction
         * @param calendarDate  calendar object representing selected day
         */
        void onSelectedDayChange(@NonNull CalendarDate calendarDate);
    }

    public MonthView(Context context) {
        this(context, null);
    }

    public MonthView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Sets listener to be notified when day selection changes
     * @param listener  listener to be notified
     */
    void setOnDateChangeListener(OnDateChangeListener listener) {
        mListener = listener;
    }

    private void init() {
        setLayoutManager(new GridLayoutManager(getContext(), SPANS_COUNT));
        setHasFixedSize(true);
        setCalendar(CalendarDate.today());
    }

    /**
     * Sets month to display
     * @param calendarDate  calendar object representing month to display
     */
    void setCalendar(@NonNull CalendarDate calendarDate) {
        mCalendarDate = calendarDate;
        mAdapter = new EventAdapter(calendarDate);
        mAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                if (mListener == null) {
                    return;
                }
                if (payload instanceof SelectionPayload) {
                    mSelectedDay.set(mCalendarDate.get(Calendar.YEAR),
                            mCalendarDate.get(Calendar.MONTH),
                            ((SelectionPayload) payload).dayOfMonth);
                    mListener.onSelectedDayChange(mSelectedDay);
                }
            }
        });
        setAdapter(mAdapter);
    }

    /**
     * Sets selected day if it falls within this month, unset any previously selected day otherwise
     * @param selectedDay    selected day or null
     */
    void setSelectedDay(@Nullable CalendarDate selectedDay) {
        if (mCalendarDate == null) {
            return;
        }
        if (selectedDay == null) {
            mAdapter.setSelectedDay(null);
        } else if (mCalendarDate.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                mCalendarDate.get(Calendar.MONTH) == selectedDay.get(Calendar.MONTH)) {
            mAdapter.setSelectedDay(selectedDay);
        } else {
            mAdapter.setSelectedDay(null);
        }
    }

    static class EventAdapter extends Adapter<CellViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_CONTENT = 1;
        private final String[] mWeekdays;
        private final int mStartOffset;
        private final int mDays;
        private int mSelectedPosition = -1;

        public EventAdapter(CalendarDate cal) {
            mWeekdays = DateFormatSymbols.getInstance().getShortWeekdays();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            mStartOffset = cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek() + SPANS_COUNT;
            mDays = mStartOffset + cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        @Override
        public CellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    return new HeaderViewHolder(inflater.inflate(
                            R.layout.grid_item_header, parent, false));
                case VIEW_TYPE_CONTENT:
                default:
                    return new ContentViewHolder(inflater.inflate(
                            R.layout.grid_item_content, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(CellViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).textView.setText(
                        mWeekdays[position + Calendar.SUNDAY]);
            } else { // holder instanceof ContentViewHolder
                if (position < mStartOffset) {
                    ((ContentViewHolder) holder).textView.setText(null);
                } else {
                    final int adapterPosition = holder.getAdapterPosition();
                    TextView textView = ((ContentViewHolder) holder).textView;
                    String day = String.valueOf(adapterPosition - mStartOffset + 1);
                    SpannableString spannable = new SpannableString(day);
                    if (mSelectedPosition == adapterPosition) {
                        spannable.setSpan(new CircleSpan(textView.getContext()), 0,
                                day.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    textView.setText(spannable, TextView.BufferType.SPANNABLE);
                    textView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setSelectedPosition(adapterPosition, true);
                        }
                    });
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < SPANS_COUNT) {
                return VIEW_TYPE_HEADER;
            }
            return VIEW_TYPE_CONTENT;
        }

        @Override
        public int getItemCount() {
            return mDays;
        }

        void setSelectedDay(@Nullable CalendarDate selectedDay) {
            setSelectedPosition(selectedDay == null ? -1 :
                    mStartOffset + selectedDay.get(Calendar.DAY_OF_MONTH) - 1, false);
        }

        private void setSelectedPosition(int position, boolean notifyObservers) {
            int last = mSelectedPosition;
            if (position == last) {
                return;
            }
            mSelectedPosition = position;
            if (last >= 0) {
                notifyItemChanged(last);
            }
            if (position >= 0) {
                notifyItemChanged(position, notifyObservers ?
                        new SelectionPayload(mSelectedPosition - mStartOffset + 1) : null);
            }
        }
    }

    static abstract class CellViewHolder extends ViewHolder {

        public CellViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderViewHolder extends CellViewHolder {

        final TextView textView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    static class ContentViewHolder extends CellViewHolder {

        final TextView textView;

        public ContentViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    static class SelectionPayload {
        final int dayOfMonth;

        public SelectionPayload(int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }
    }
}
