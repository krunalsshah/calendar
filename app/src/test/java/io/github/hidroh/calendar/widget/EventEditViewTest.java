package io.github.hidroh.calendar.widget;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDatePickerDialog;
import org.robolectric.util.ActivityController;

import java.util.Calendar;
import java.util.TimeZone;

import io.github.hidroh.calendar.CalendarUtils;
import io.github.hidroh.calendar.R;
import io.github.hidroh.calendar.content.CalendarCursor;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class EventEditViewTest {
    private ActivityController<TestActivity> controller;
    private TestActivity activity;
    private EventEditView view;
    private EditText editTextTitle;
    private SwitchCompat switchAllDay;
    private TextView textViewStartDate;
    private TextView textViewStartTime;
    private TextView textViewEndDate;
    private TextView textViewEndTime;
    private TextView textViewCalendar;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestActivity.class);
        activity = controller.create().start().resume().get();
        view = (EventEditView) activity.findViewById(R.id.event_edit_view);
        //noinspection ConstantConditions
        editTextTitle = (EditText) view.findViewById(R.id.edit_text_title);
        switchAllDay = (SwitchCompat) view.findViewById(R.id.switch_all_day);
        textViewStartDate = (TextView) view.findViewById(R.id.text_view_start_date);
        textViewStartTime = (TextView) view.findViewById(R.id.text_view_start_time);
        textViewEndDate = (TextView) view.findViewById(R.id.text_view_end_date);
        textViewEndTime = (TextView) view.findViewById(R.id.text_view_end_time);
        textViewCalendar = (TextView) view.findViewById(R.id.text_view_calendar);
    }

    @Test
    public void testNewEvent() {
        assertThat(editTextTitle).isEmpty();
        assertFalse(switchAllDay.isChecked());
        assertThat(textViewStartDate).isNotEmpty();
        assertThat(textViewStartTime).isNotEmpty();
        assertThat(textViewEndDate).isNotEmpty();
        assertThat(textViewEndTime).isNotEmpty();
        assertThat(textViewCalendar).isEmpty();
        assertFalse(view.getEvent().hasId());
        assertFalse(view.getEvent().hasCalendarId());
    }

    @Test
    public void testExistingAllDayEvent() {
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 0, 0),
                end = createTimeMillis(2016, Calendar.MARCH, 19, 0, 0);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(true)
                .build());
        assertThat(editTextTitle)
                .hasTextString("title");
        assertTrue(switchAllDay.isChecked());
        assertHasDateString(textViewStartDate, start);
        assertHasTimeString(textViewStartTime, start);
        assertHasDateString(textViewEndDate, end);
        assertHasTimeString(textViewEndTime, end);
        // calendar name to be set independently
        assertThat(textViewCalendar).isEmpty();
    }

    @Test
    public void testExistingNonAllDayEvent() {
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 19, 12, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());
        assertFalse(switchAllDay.isChecked());
    }

    @Test
    public void testCalendarName() {
        view.setSelectedCalendar("My birthdays");
        assertThat(textViewCalendar)
                .hasTextString("My birthdays");
    }

    @Test
    public void testEditStartDate() {
        // initial event timing
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 19, 8, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());

        // clicking label should show date picker dialog
        textViewStartDate.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(alertDialog).isInstanceOf(DatePickerDialog.class);

        // selecting date should set date label, should not change time
        ShadowDatePickerDialog shadowDatePickerDialog = shadowOf((DatePickerDialog) alertDialog);
        shadowDatePickerDialog.getOnDateSetListenerCallback()
                .onDateSet(null, 2016, Calendar.APRIL, 15);
        long expected = createTimeMillis(2016, Calendar.APRIL, 15, 0, 0);
        assertHasDateString(textViewStartDate, expected);
        assertHasTimeString(textViewStartTime, start);

        // setting start date > end date should advance end date time = start date time as well
        assertHasDateString(textViewEndDate, expected);
        assertHasTimeString(textViewEndTime, start);

        // set start date backwards should not change end date time
        textViewStartDate.performClick();
        shadowOf((DatePickerDialog) ShadowDatePickerDialog.getLatestAlertDialog())
                .getOnDateSetListenerCallback()
                .onDateSet(null, 2016, Calendar.MARCH, 30);
        long newExpected = createTimeMillis(2016, Calendar.MARCH, 30, 0, 0);
        assertHasDateString(textViewStartDate, newExpected);
        assertHasTimeString(textViewStartTime, start);
        assertHasDateString(textViewEndDate, expected);
        assertHasTimeString(textViewEndTime, start);
    }

    @Test
    public void testEditEndDate() {
        // initial event timing
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 19, 8, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());

        // selecting date should set date label, should not change time
        textViewEndDate.performClick();
        shadowOf((DatePickerDialog) ShadowDatePickerDialog.getLatestAlertDialog())
                .getOnDateSetListenerCallback()
                .onDateSet(null, 2016, Calendar.MARCH, 15);
        long expected = createTimeMillis(2016, Calendar.MARCH, 15, 0, 0);
        assertHasDateString(textViewEndDate, expected);
        assertHasTimeString(textViewEndTime, end);

        // setting end date < start date should retract start date time = end date time as well
        assertHasDateString(textViewStartDate, expected);
        assertHasTimeString(textViewStartTime, end);

        // set end date forwards should not change start date time
        textViewEndDate.performClick();
        shadowOf((DatePickerDialog) ShadowDatePickerDialog.getLatestAlertDialog())
                .getOnDateSetListenerCallback()
                .onDateSet(null, 2016, Calendar.MARCH, 30);
        long newExpected = createTimeMillis(2016, Calendar.MARCH, 30, 0, 0);
        assertHasDateString(textViewEndDate, newExpected);
        assertHasTimeString(textViewEndTime, end);
        assertHasDateString(textViewStartDate, expected);
        assertHasTimeString(textViewStartTime, end);
    }

    @Test
    public void testEditStartTime() {
        // initial event timing
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 18, 11, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());

        // clicking label should show date picker dialog
        textViewStartTime.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(alertDialog).isInstanceOf(TimePickerDialog.class);

        // selecting time should set time label, should not change date
        TimePickerDialog timePickerDialog = (TimePickerDialog) alertDialog;
        timePickerDialog.updateTime(12, 45);
        ((TimePickerDialog) alertDialog).onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
        long expected = createTimeMillis(2016, Calendar.MARCH, 18, 12, 45);
        assertHasDateString(textViewStartDate, start);
        assertHasTimeString(textViewStartTime, expected);

        // setting start date time > end date time should advance end date time = start date time as well
        assertHasDateString(textViewEndDate, end);
        assertHasTimeString(textViewEndTime, expected);

        // set start date time backwards should not change end date time
        textViewStartTime.performClick();
        timePickerDialog = (TimePickerDialog) ShadowAlertDialog.getLatestAlertDialog();
        timePickerDialog.updateTime(7, 45);
        timePickerDialog.onClick(timePickerDialog, DialogInterface.BUTTON_POSITIVE);
        long newExpected = createTimeMillis(2016, Calendar.MARCH, 18, 7, 45);
        assertHasDateString(textViewStartDate, start);
        assertHasTimeString(textViewStartTime, newExpected);
        assertHasDateString(textViewEndDate, end);
        assertHasTimeString(textViewEndTime, expected);
    }

    @Test
    public void testEditEndTime() {
        // initial event timing
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 18, 11, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());

        // selecting time should set time label, should not change date
        textViewEndTime.performClick();
        TimePickerDialog timePickerDialog = (TimePickerDialog) ShadowAlertDialog.getLatestAlertDialog();
        timePickerDialog.updateTime(7, 45);
        timePickerDialog.onClick(timePickerDialog, DialogInterface.BUTTON_POSITIVE);
        long expected = createTimeMillis(2016, Calendar.MARCH, 18, 7, 45);
        assertHasDateString(textViewEndDate, end);
        assertHasTimeString(textViewEndTime, expected);

        // setting end date time < start date time should retract start date time = end date time as well
        assertHasDateString(textViewStartDate, start);
        assertHasTimeString(textViewStartTime, expected);

        // set end date time forwards should not change start date time
        textViewEndTime.performClick();
        timePickerDialog = (TimePickerDialog) ShadowAlertDialog.getLatestAlertDialog();
        timePickerDialog.updateTime(10, 45);
        timePickerDialog.onClick(timePickerDialog, DialogInterface.BUTTON_POSITIVE);
        long newExpected = createTimeMillis(2016, Calendar.MARCH, 18, 10, 45);
        assertHasDateString(textViewEndDate, end);
        assertHasTimeString(textViewEndTime, newExpected);
        assertHasDateString(textViewStartDate, start);
        assertHasTimeString(textViewStartTime, expected);
    }

    @Test
    public void testSwitchAllDay() {
        // initial event timing
        long start = createTimeMillis(2016, Calendar.MARCH, 18, 10, 30),
                end = createTimeMillis(2016, Calendar.MARCH, 18, 11, 30);
        view.setEvent(new EventEditView.Event.Builder()
                .id(1L)
                .calendarId(1L)
                .title("title")
                .start(start)
                .end(end)
                .allDay(false)
                .build());
        assertFalse(switchAllDay.isChecked());

        // switching to all day should set start & end time to midnight, advance end date if equals
        switchAllDay.toggle();
        assertTrue(switchAllDay.isChecked());
        long expectedStart = createTimeMillis(2016, Calendar.MARCH, 18, 0, 0),
                expectedEnd = createTimeMillis(2016, Calendar.MARCH, 19, 0, 0);
        assertHasDateString(textViewStartDate, expectedStart);
        assertHasTimeString(textViewStartTime, expectedStart);
        assertHasDateString(textViewEndDate, expectedEnd);
        assertHasTimeString(textViewEndTime, expectedEnd);
        assertTrue(view.getEvent().isAllDay());
        assertThat(view.getEvent().getTimeZone()).isEqualTo("UTC");

        // changing date should automatically toggle off all day switch
        textViewEndDate.performClick();
        shadowOf((DatePickerDialog) ShadowDatePickerDialog.getLatestAlertDialog())
                .getOnDateSetListenerCallback()
                .onDateSet(null, 2016, Calendar.MARCH, 20);
        assertFalse(switchAllDay.isChecked());
        assertFalse(view.getEvent().isAllDay());
        assertThat(view.getEvent().getTimeZone()).isNotEqualTo("UTC");
    }

    @Test
    public void testChangeCalendar() {
        // initial state
        assertThat(textViewCalendar).isDisabled();
        assertFalse(view.getEvent().hasCalendarId());

        // swapping empty calendar source should not enable control
        view.swapCalendarSource(new TestCalendarCursor());
        assertThat(textViewCalendar).isDisabled();

        // swapping non-empty calendar source should enable control
        TestCalendarCursor cursor = new TestCalendarCursor();
        cursor.addRow(new Object[]{1L, "My calendar"});
        cursor.addRow(new Object[]{2L, "Birthdays"});
        view.swapCalendarSource(cursor);
        assertThat(textViewCalendar).isEnabled();

        // selecting calendar should update view model and label
        textViewCalendar.performClick();
        // TODO Robolectric does not shadow v7 AlertDialog yet, have to trigger change manually here
        view.changeCalendar(1);
        assertThat(textViewCalendar).hasTextString("Birthdays");
        assertThat(view.getEvent().getCalendarId()).isEqualTo(2L);
        assertTrue(view.getEvent().hasCalendarId());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertHasDateString(TextView textView, long timeMillis) {
        assertThat(textView).hasTextString(CalendarUtils.toDayString(activity, timeMillis));
    }

    private void assertHasTimeString(TextView textView, long timeMillis) {
        assertThat(textView).hasTextString(CalendarUtils.toTimeString(activity, timeMillis));
    }

    private long createTimeMillis(int year, int month, int dayOfMonth, int hour, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(year, month, dayOfMonth, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    static class TestActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EventEditView view = new EventEditView(this);
            view.setId(R.id.event_edit_view);
            view.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(view);
        }
    }

    static class TestCalendarCursor extends CalendarCursor {

        public TestCalendarCursor() {
            super(new MatrixCursor(CalendarCursor.PROJECTION));
        }

        void addRow(Object[] columnValues) {
            ((MatrixCursor) getWrappedCursor()).addRow(columnValues);
        }
    }
}
