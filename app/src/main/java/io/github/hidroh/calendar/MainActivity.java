package io.github.hidroh.calendar;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import io.github.hidroh.calendar.widget.AgendaAdapter;
import io.github.hidroh.calendar.widget.AgendaView;
import io.github.hidroh.calendar.widget.EventCalendarView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TOOLBAR_TOGGLE = "state:toolbarToggle";
    private static final String[] EVENTS_PROJECTION = new String[]{
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.TITLE
    };

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
        if (checkPermissions()) {
            loadEvents();
        } else {
            requestPermissions();
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAgendaView.setAdapter(null); // force detaching adapter
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (checkPermissions()) {
            loadEvents();
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
        }
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

    private boolean checkPermissions() {
        return (checkPermission(Manifest.permission.READ_CALENDAR) |
                checkPermission(Manifest.permission.WRITE_CALENDAR)) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR},
                0);
    }

    private void loadEvents() {
        mAgendaView.setAdapter(new AgendaCursorAdapter(this));
    }

    @VisibleForTesting
    protected int checkPermission(@NonNull String permission) {
        return ActivityCompat.checkSelfPermission(this, permission);
    }

    /**
     * Coordinator utility that synchronizes widgets as selected date changes
     */
    static class Coordinator {
        private static final String STATE_SELECTED_DATE = "state:selectedDate";

        private final EventCalendarView.OnChangeListener mCalendarListener
                = new EventCalendarView.OnChangeListener() {
            @Override
            public void onSelectedDayChange(long calendarDate) {
                sync(calendarDate, mCalendarView);
            }
        };
        private final AgendaView.OnDateChangeListener mAgendaListener
                = new AgendaView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(long dayMillis) {
                sync(dayMillis, mAgendaView);
            }
        };
        private TextView mTextView;
        private EventCalendarView mCalendarView;
        private AgendaView mAgendaView;
        private long mSelectedDayMillis = CalendarUtils.NO_TIME_MILLIS;

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
            if (mSelectedDayMillis < 0) {
                mSelectedDayMillis = CalendarUtils.today();
            }
            mCalendarView.setSelectedDay(mSelectedDayMillis);
            agendaView.setSelectedDay(mSelectedDayMillis);
            updateTitle(mSelectedDayMillis);
            calendarView.setOnChangeListener(mCalendarListener);
            agendaView.setOnDateChangeListener(mAgendaListener);
        }

        void saveState(Bundle outState) {
            outState.putLong(STATE_SELECTED_DATE, mSelectedDayMillis);
        }

        void restoreState(Bundle savedState) {
            mSelectedDayMillis = savedState.getLong(STATE_SELECTED_DATE,
                    CalendarUtils.NO_TIME_MILLIS);
        }

        private void sync(long dayMillis, View originator) {
            mSelectedDayMillis = dayMillis;
            if (originator != mCalendarView) {
                mCalendarView.setSelectedDay(dayMillis);
            }
            if (originator != mAgendaView) {
                mAgendaView.setSelectedDay(dayMillis);
            }
            updateTitle(dayMillis);
        }

        private void updateTitle(long dayMillis) {
            mTextView.setText(CalendarUtils.toMonthString(mTextView.getContext(), dayMillis));
        }
    }

    static class AgendaCursorAdapter extends AgendaAdapter {

        private final DayEventsQueryHandler mHandler;

        public AgendaCursorAdapter(Context context) {
            super(context);
            mHandler = new DayEventsQueryHandler(context.getContentResolver(), this);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            deactivate();
        }

        @Override
        protected void loadEvents(long timeMillis) {
            mHandler.startQuery(timeMillis, timeMillis + DateUtils.DAY_IN_MILLIS);
        }
    }

    static class DayEventsQueryHandler extends EventsQueryHandler {

        private final AgendaCursorAdapter mAgendaCursorAdapter;

        public DayEventsQueryHandler(ContentResolver cr, AgendaCursorAdapter agendaCursorAdapter) {
            super(cr);
            mAgendaCursorAdapter = agendaCursorAdapter;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mAgendaCursorAdapter.bindEvents((Long) cookie, cursor);
        }
    }


    static abstract class EventsQueryHandler extends AsyncQueryHandler {

        public EventsQueryHandler(ContentResolver cr) {
            super(cr);
        }

        protected final void startQuery(long startTimeMillis, long endTimeMillis) {
            startQuery(0, startTimeMillis,
                    CalendarContract.Events.CONTENT_URI,
                    EVENTS_PROJECTION,
                    "(" + CalendarContract.Events.DTSTART + ">=? AND " +
                            CalendarContract.Events.DTSTART + "<?) OR (" +
                            CalendarContract.Events.DTSTART + "<? AND " +
                            CalendarContract.Events.DTEND + ">=?) AND " +
                            CalendarContract.Events.DELETED + "=?",
                    new String[]{
                            String.valueOf(startTimeMillis),
                            String.valueOf(endTimeMillis),
                            String.valueOf(startTimeMillis),
                            String.valueOf(endTimeMillis),
                            "0"
                    },
                    CalendarContract.Events.DTSTART + " ASC");
        }
    }
}
