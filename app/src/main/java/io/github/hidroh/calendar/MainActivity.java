package io.github.hidroh.calendar;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import io.github.hidroh.calendar.widget.AgendaView;
import io.github.hidroh.calendar.widget.EventCalendarView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TOOLBAR_TOGGLE = "state:toolbarToggle";

    private final Coordinator mCoordinator = new Coordinator();
    private CheckedTextView mToolbarToggle;
    private EventCalendarView mCalendarView;
    private AgendaView mAgendaView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        setupContentView();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCoordinator.restoreState(savedInstanceState);
        if (savedInstanceState.getBoolean(STATE_TOOLBAR_TOGGLE, false)) {
            mToolbarToggle.performClick();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mCoordinator.coordinate(mToolbarToggle, mCalendarView, mAgendaView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCoordinator.saveState(outState);
        outState.putBoolean(STATE_TOOLBAR_TOGGLE, mToolbarToggle.isChecked());
    }

    private void setupContentView() {
        mToolbarToggle = (CheckedTextView) findViewById(R.id.toolbar_toggle);
        mToolbarToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mToolbarToggle.toggle();
                toggleCalendarView();
            }
        });
        mCalendarView = (EventCalendarView) findViewById(R.id.calendar_view);
        mAgendaView = (AgendaView) findViewById(R.id.agenda_view);
    }

    private void toggleCalendarView() {
        if (mToolbarToggle.isChecked()) {
            mCalendarView.setVisibility(View.VISIBLE);
        } else {
            mCalendarView.setVisibility(View.GONE);
        }
    }

    /**
     * Coordinator utility that synchronizes widgets as selected date changes
     */
    static class Coordinator {
        private static final String STATE_SELECTED_DATE = "state:selectedDate";

        private final EventCalendarView.OnChangeListener mCalendarListener
                = new EventCalendarView.OnChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarDate calendarDate) {
                sync(calendarDate, mCalendarView);
            }
        };
        private final AgendaView.OnDateChangeListener mAgendaListener
                = new AgendaView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarDate calendarDate) {
                sync(calendarDate, mAgendaView);
            }
        };
        private TextView mTextView;
        private EventCalendarView mCalendarView;
        private AgendaView mAgendaView;
        private CalendarDate mSelectedDate;

        /**
         * Set up widgets to be synchronized
         * @param textView      title
         * @param calendarView  calendar view
         * @param agendaView    agenda view
         */
        public void coordinate(@NonNull TextView textView,
                               @NonNull EventCalendarView calendarView,
                               @NonNull AgendaView agendaView) {
            if (mCalendarView != null) {
                mCalendarView.setOnChangeListener(null);
            }
            if (mAgendaView != null) {
                mAgendaView.setOnDateChangeListener(null);
            }
            mTextView = textView;
            mCalendarView = calendarView;
            mAgendaView = agendaView;
            if (mSelectedDate == null) {
                mSelectedDate = CalendarDate.today();
            }
            mCalendarView.setSelectedDay(mSelectedDate);
            agendaView.setSelectedDay(mSelectedDate);
            updateTitle(mSelectedDate);
            calendarView.setOnChangeListener(mCalendarListener);
            agendaView.setOnDateChangeListener(mAgendaListener);
        }

        void saveState(Bundle outState) {
            outState.putLong(STATE_SELECTED_DATE, mSelectedDate != null ?
                    mSelectedDate.getTimeInMillis() : -1);
        }

        void restoreState(Bundle savedState) {
            mSelectedDate = CalendarDate.fromTime(savedState.getLong(STATE_SELECTED_DATE, -1));
        }

        private void sync(@NonNull CalendarDate calendarDate, View originator) {
            mSelectedDate = calendarDate;
            if (originator != mCalendarView) {
                mCalendarView.setSelectedDay(calendarDate);
            }
            if (originator != mAgendaView) {
                mAgendaView.setSelectedDay(calendarDate);
            }
            updateTitle(calendarDate);
        }

        private void updateTitle(CalendarDate calendarDate) {
            mTextView.setText(CalendarUtils.toMonthString(mTextView.getContext(),
                    calendarDate.getTimeInMillis()));
        }
    }
}
