package io.github.hidroh.calendar;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;

import java.util.Calendar;

import io.github.hidroh.calendar.widget.EventCalendarView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TOOLBAR_TOGGLE = "state:toolbarToggle";
    private CheckedTextView mToolbarToggle;
    private EventCalendarView mCalendarView;

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
        if (savedInstanceState.getBoolean(STATE_TOOLBAR_TOGGLE, false)) {
            mToolbarToggle.performClick();
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
        mCalendarView.setOnChangeListener(new EventCalendarView.OnChangeListener() {
            @Override
            public void onSelectedMonthChange(@NonNull Calendar calendar) {
                updateTitle(calendar);
            }

            @Override
            public void onSelectedDayChange(@NonNull Calendar calendar) {
                // TODO
            }
        });
    }

    private void updateTitle(Calendar calendar) {
        final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                | DateUtils.FORMAT_SHOW_YEAR;
        final long millis = calendar.getTimeInMillis();
        mToolbarToggle.setText(DateUtils.formatDateRange(MainActivity.this,
                millis, millis, flags));
    }

    private void toggleCalendarView() {
        if (mToolbarToggle.isChecked()) {
            mCalendarView.setVisibility(View.VISIBLE);
        } else {
            mCalendarView.setVisibility(View.GONE);
        }
    }
}
