package io.github.hidroh.calendar.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import io.github.hidroh.calendar.R;

/**
 * Custom widget to display a grid of days in a month, represented by a {@link Calendar}
 */
public class MonthView extends RecyclerView {
    private static final int SPANS_COUNT = 7; // days in week
    private Calendar mCalendar;

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

    private void init() {
        setLayoutManager(new GridLayoutManager(getContext(), SPANS_COUNT));
        setHasFixedSize(true);
        setCalendar(Calendar.getInstance());
    }

    /**
     * Set month to display
     * @param calendar    calendar object representing month to display
     */
    void setCalendar(Calendar calendar) {
        mCalendar = calendar;
        setAdapter(new EventAdapter(calendar));
    }

    /**
     * Get calendar object currently being displayed
     * @return  displayed calendar
     */
    Calendar getCalendar() {
        return mCalendar;
    }

    static class EventAdapter extends Adapter<CellViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_CONTENT = 1;
        private final String[] mWeekdays;
        private final int mStartOffset;
        private final int mDays;

        public EventAdapter(Calendar cal) {
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
            } else if (holder instanceof ContentViewHolder) {
                if (position < mStartOffset) {
                    ((ContentViewHolder) holder).textView.setText(null);
                } else {
                    String day = String.valueOf(position - mStartOffset + 1);
                    ((ContentViewHolder) holder).textView.setText(day);
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
}
