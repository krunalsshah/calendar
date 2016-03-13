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

    static class AgendaCursorAdapter extends AgendaAdapter {

        private final CalendarQueryHandler mHandler;

        public AgendaCursorAdapter(Context context) {
            super(context);
            mHandler = new CalendarQueryHandler(context.getContentResolver(), this);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            deactivate();
        }

        @Override
        protected void loadEvents(long timeMillis) {
            mHandler.startQuery(0, timeMillis,
                    CalendarContract.Events.CONTENT_URI,
                    EVENTS_PROJECTION,
                    "(" + CalendarContract.Events.DTSTART + ">=? AND " +
                            CalendarContract.Events.DTSTART + "<?) OR (" +
                            CalendarContract.Events.DTSTART + "<? AND " +
                            CalendarContract.Events.DTEND + ">=?) AND " +
                            CalendarContract.Events.DELETED + "=?",
                    new String[]{
                            String.valueOf(timeMillis),
                            String.valueOf(timeMillis + DateUtils.DAY_IN_MILLIS),
                            String.valueOf(timeMillis),
                            String.valueOf(timeMillis + DateUtils.DAY_IN_MILLIS),
                            "0"
                    },
                    CalendarContract.Events.DTSTART + " ASC");
        }
    }

    static class CalendarQueryHandler extends AsyncQueryHandler {

        private final AgendaCursorAdapter mAgendaCursorAdapter;

        public CalendarQueryHandler(ContentResolver cr, AgendaCursorAdapter agendaCursorAdapter) {
            super(cr);
            mAgendaCursorAdapter = agendaCursorAdapter;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mAgendaCursorAdapter.bindEvents((Long) cookie, cursor);
        }
    }
}
