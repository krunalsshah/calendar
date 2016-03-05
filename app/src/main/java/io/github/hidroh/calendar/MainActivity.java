package io.github.hidroh.calendar;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import io.github.hidroh.calendar.widget.EventCalendarView;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_TOOLBAR_TOGGLE = "state:toolbarToggle";
    private ViewGroup mViewGroup;
    private CheckedTextView mToolbarToggle;
    private View mCalendarView;

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
            // TODO restore calendar view state
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
        // TODO save calendar view state
    }

    private void setupContentView() {
        mToolbarToggle = (CheckedTextView) findViewById(R.id.toolbar_toggle);
        mViewGroup = (ViewGroup) findViewById(R.id.linear_layout);
        mToolbarToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mToolbarToggle.toggle();
                toggleCalendarView();
            }
        });
        mCalendarView = new EventCalendarView(this);
        mCalendarView.setId(R.id.calendar_view);
        mCalendarView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // TODO update toolbar title with active month in calendar view
    }

    private void toggleCalendarView() {
        if (mToolbarToggle.isChecked()) {
            mViewGroup.addView(mCalendarView, 1); // below toolbar
        } else {
            mViewGroup.removeViewAt(1);
        }
    }
}
