package io.github.hidroh.calendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.Calendar;

import io.github.hidroh.calendar.content.EventCursor;
import io.github.hidroh.calendar.content.EventsQueryHandler;
import io.github.hidroh.calendar.weather.WeatherSyncService;
import io.github.hidroh.calendar.widget.AgendaAdapter;
import io.github.hidroh.calendar.widget.AgendaView;
import io.github.hidroh.calendar.widget.EventCalendarView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TOOLBAR_TOGGLE = "state:toolbarToggle";
    private static final int REQUEST_CODE_CALENDAR = 0;
    private static final int REQUEST_CODE_LOCATION = 1;

    private final SharedPreferences.OnSharedPreferenceChangeListener mWeatherChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    if (TextUtils.equals(key, WeatherSyncService.PREF_WEATHER_TODAY) ||
                            TextUtils.equals(key, WeatherSyncService.PREF_WEATHER_TOMORROW)) {
                        loadWeather();
                    }
                }
            };
    private final Coordinator mCoordinator = new Coordinator();
    private View mCoordinatorLayout;
    private CheckedTextView mToolbarToggle;
    private EventCalendarView mCalendarView;
    private AgendaView mAgendaView;
    private FloatingActionButton mFabAdd;
    private boolean mWeatherEnabled, mPendingWeatherEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpPreferences();
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        setUpContentView();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCoordinator.restoreState(savedInstanceState);
        if (savedInstanceState.getBoolean(STATE_TOOLBAR_TOGGLE, false)) {
            View toggleButton = findViewById(R.id.toolbar_toggle_frame);
            if (toggleButton != null) { // can be null as disabled in landscape
                toggleButton.performClick();
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mCoordinator.coordinate(mToolbarToggle, mCalendarView, mAgendaView);
        if (checkCalendarPermissions()) {
            loadEvents();
        } else {
            toggleEmptyView(true);
        }
        if (mWeatherEnabled && !checkLocationPermissions()) {
            explainLocationPermissions();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_weather).setChecked(mWeatherEnabled);
        switch (CalendarUtils.sWeekStart) {
            case Calendar.SATURDAY:
                menu.findItem(R.id.action_week_start_saturday).setChecked(true);
                break;
            case Calendar.SUNDAY:
                menu.findItem(R.id.action_week_start_sunday).setChecked(true);
                break;
            case Calendar.MONDAY:
                menu.findItem(R.id.action_week_start_monday).setChecked(true);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_today) {
            mCoordinator.reset();
            return true;
        }
        if (item.getItemId() == R.id.action_weather) {
            mPendingWeatherEnabled = !mWeatherEnabled;
            if (!mWeatherEnabled && !checkLocationPermissions()) {
                requestLocationPermissions();
            } else {
                toggleWeather();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_week_start_saturday ||
                item.getItemId() == R.id.action_week_start_sunday ||
                item.getItemId() == R.id.action_week_start_monday) {
            if (!item.isChecked()) {
                changeWeekStart(item.getItemId());
            }
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
        mCalendarView.deactivate();
        mAgendaView.setAdapter(null); // force detaching adapter
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mWeatherChangeListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_CALENDAR:
                if (checkCalendarPermissions()) {
                    toggleEmptyView(false);
                    loadEvents();
                } else {
                    toggleEmptyView(true);
                }
                break;
            case REQUEST_CODE_LOCATION:
                if (checkLocationPermissions()) {
                    toggleWeather();
                } else {
                    explainLocationPermissions();
                }
                break;
        }
    }

    private void setUpPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mWeatherEnabled = mPendingWeatherEnabled = sp.getBoolean(
                WeatherSyncService.PREF_WEATHER_ENABLED, false);
        CalendarUtils.sWeekStart = sp.getInt(CalendarUtils.PREF_WEEK_START, Calendar.SUNDAY);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mWeatherChangeListener);
    }

    private void setUpContentView() {
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mToolbarToggle = (CheckedTextView) findViewById(R.id.toolbar_toggle);
        View toggleButton = findViewById(R.id.toolbar_toggle_frame);
        if (toggleButton != null) { // can be null as disabled in landscape
            toggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mToolbarToggle.toggle();
                    toggleCalendarView();
                }
            });
        }
        mCalendarView = (EventCalendarView) findViewById(R.id.calendar_view);
        mAgendaView = (AgendaView) findViewById(R.id.agenda_view);
        mFabAdd = (FloatingActionButton) findViewById(R.id.fab);
        mFabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEvent();
            }
        });
        //noinspection ConstantConditions
        mFabAdd.hide();
    }

    private void toggleCalendarView() {
        if (mToolbarToggle.isChecked()) {
            mCalendarView.setVisibility(View.VISIBLE);
        } else {
            mCalendarView.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void toggleEmptyView(boolean visible) {
        if (visible) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
            findViewById(R.id.empty).bringToFront();
            findViewById(R.id.button_permission)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestCalendarPermissions();
                        }
                    });
        } else {
            findViewById(R.id.empty).setVisibility(View.GONE);
        }
    }

    private void changeWeekStart(@IdRes int selection) {
        switch (selection) {
            case R.id.action_week_start_saturday:
                CalendarUtils.sWeekStart = Calendar.SATURDAY;
                break;
            case R.id.action_week_start_sunday:
                CalendarUtils.sWeekStart = Calendar.SUNDAY;
                break;
            case R.id.action_week_start_monday:
                CalendarUtils.sWeekStart = Calendar.MONDAY;
                break;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(CalendarUtils.PREF_WEEK_START, CalendarUtils.sWeekStart)
                .apply();
        supportInvalidateOptionsMenu();
        mCoordinator.reset();
    }

    private void createEvent() {
        startActivity(new Intent(this, EditActivity.class));
    }

    private void loadEvents() {
        mFabAdd.show();
        mCalendarView.setCalendarAdapter(new CalendarCursorAdapter(this));
        mAgendaView.setAdapter(new AgendaCursorAdapter(this));
        loadWeather();
    }

    private void toggleWeather() {
        mWeatherEnabled = mPendingWeatherEnabled;
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, mWeatherEnabled)
                .apply();
        supportInvalidateOptionsMenu();
        loadWeather();
    }

    private void loadWeather() {
        mAgendaView.setWeather(mWeatherEnabled ? WeatherSyncService.getSyncedWeather(this) : null);
    }

    @VisibleForTesting
    protected boolean checkCalendarPermissions() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) |
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)) ==
                PackageManager.PERMISSION_GRANTED;
    }

    @VisibleForTesting
    protected boolean checkLocationPermissions() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) |
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) ==
                PackageManager.PERMISSION_GRANTED;
    }

    @VisibleForTesting
    protected void requestCalendarPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR},
                REQUEST_CODE_CALENDAR);
    }

    @VisibleForTesting
    protected void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
    }

    private void explainLocationPermissions() {
        Snackbar.make(mCoordinatorLayout, R.string.location_permission_required,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.grant_access, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocationPermissions();
                    }
                })
                .show();
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

        void reset() {
            mSelectedDayMillis = CalendarUtils.today();
            if (mCalendarView != null) {
                mCalendarView.reset();
            }
            if (mAgendaView != null) {
                mAgendaView.reset();
            }
            updateTitle(mSelectedDayMillis);
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

        @VisibleForTesting
        final DayEventsQueryHandler mHandler;

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
            mHandler.startQuery(timeMillis, timeMillis, timeMillis + DateUtils.DAY_IN_MILLIS);
        }
    }

    static class CalendarCursorAdapter extends EventCalendarView.CalendarAdapter {
        private final MonthEventsQueryHandler mHandler;

        public CalendarCursorAdapter(Context context) {
            mHandler = new MonthEventsQueryHandler(context.getContentResolver(), this);
        }

        @Override
        protected void loadEvents(long monthMillis) {
            long startTimeMillis = CalendarUtils.monthFirstDay(monthMillis),
                    endTimeMillis = startTimeMillis + DateUtils.DAY_IN_MILLIS *
                            CalendarUtils.monthSize(monthMillis);
            mHandler.startQuery(monthMillis, startTimeMillis, endTimeMillis);
        }
    }

    static class DayEventsQueryHandler extends EventsQueryHandler {

        private final AgendaCursorAdapter mAgendaCursorAdapter;

        public DayEventsQueryHandler(ContentResolver cr, AgendaCursorAdapter agendaCursorAdapter) {
            super(cr);
            mAgendaCursorAdapter = agendaCursorAdapter;
        }

        @Override
        protected void handleQueryComplete(int token, Object cookie, EventCursor cursor) {
            mAgendaCursorAdapter.bindEvents((Long) cookie, cursor);
        }
    }

    static class MonthEventsQueryHandler extends EventsQueryHandler {

        private final CalendarCursorAdapter mAdapter;

        public MonthEventsQueryHandler(ContentResolver cr, CalendarCursorAdapter adapter) {
            super(cr);
            mAdapter = adapter;
        }

        @Override
        protected void handleQueryComplete(int token, Object cookie, EventCursor cursor) {
            mAdapter.bindEvents((Long) cookie, cursor);
        }
    }
}
