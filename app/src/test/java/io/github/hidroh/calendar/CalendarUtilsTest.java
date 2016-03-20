package io.github.hidroh.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
public class CalendarUtilsTest {
    @Test
    public void testIsNotTime() {
        assertTrue(CalendarUtils.isNotTime(CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.isNotTime(System.currentTimeMillis()));
    }

    @Test
    public void testToday() {
        long actual = CalendarUtils.today(), now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(actual);
        assertThat(calendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(0);
        assertThat(calendar.get(Calendar.MINUTE)).isEqualTo(0);
        assertThat(calendar.get(Calendar.SECOND)).isEqualTo(0);
        assertThat(calendar.get(Calendar.MILLISECOND)).isEqualTo(0);
    }

    @Test
    public void testToDayString() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.MARCH, 20);
        String actual = CalendarUtils.toDayString(RuntimeEnvironment.application,
                calendar.getTimeInMillis());
        assertThat(actual)
                .contains("Sunday")
                .contains("March")
                .contains("20");
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testToMonthString() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.MARCH, 20);
        String actual = CalendarUtils.toMonthString(RuntimeEnvironment.application,
                calendar.getTimeInMillis());
        assertThat(actual)
                .doesNotContain("Sunday")
                .contains("March")
                .contains("20");
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testToTimeString() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2016, Calendar.MARCH, 20, 8, 30);
        String actual = CalendarUtils.toTimeString(RuntimeEnvironment.application,
                calendar.getTimeInMillis());
        assertThat(actual).contains("8:30 AM");
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testSameMonth() {
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        Calendar march30 = Calendar.getInstance();
        march30.set(2016, Calendar.MARCH, 30);
        Calendar april20 = Calendar.getInstance();
        april20.set(2016, Calendar.APRIL, 20);
        assertFalse(CalendarUtils.sameMonth(CalendarUtils.NO_TIME_MILLIS, CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.sameMonth(march20.getTimeInMillis(), CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.sameMonth(CalendarUtils.NO_TIME_MILLIS, march20.getTimeInMillis()));
        assertTrue(CalendarUtils.sameMonth(march20.getTimeInMillis(), march20.getTimeInMillis()));
        assertTrue(CalendarUtils.sameMonth(march20.getTimeInMillis(), march30.getTimeInMillis()));
        assertFalse(CalendarUtils.sameMonth(march20.getTimeInMillis(), april20.getTimeInMillis()));
    }

    @Test
    public void testMonthBefore() {
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        Calendar march30 = Calendar.getInstance();
        march30.set(2016, Calendar.MARCH, 30);
        Calendar may20 = Calendar.getInstance();
        may20.set(2016, Calendar.MAY, 20);
        assertFalse(CalendarUtils.monthBefore(CalendarUtils.NO_TIME_MILLIS, CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.monthBefore(march20.getTimeInMillis(), CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.monthBefore(CalendarUtils.NO_TIME_MILLIS, march20.getTimeInMillis()));
        assertFalse(CalendarUtils.monthBefore(march20.getTimeInMillis(), march20.getTimeInMillis()));
        assertFalse(CalendarUtils.monthBefore(march20.getTimeInMillis(), march30.getTimeInMillis()));
        assertTrue(CalendarUtils.monthBefore(march20.getTimeInMillis(), may20.getTimeInMillis()));
        assertFalse(CalendarUtils.monthBefore(may20.getTimeInMillis(), march20.getTimeInMillis()));
    }

    @Test
    public void testMonthAfter() {
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        Calendar march30 = Calendar.getInstance();
        march30.set(2016, Calendar.MARCH, 30);
        Calendar may20 = Calendar.getInstance();
        may20.set(2016, Calendar.MAY, 20);
        assertFalse(CalendarUtils.monthAfter(CalendarUtils.NO_TIME_MILLIS, CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.monthAfter(march20.getTimeInMillis(), CalendarUtils.NO_TIME_MILLIS));
        assertFalse(CalendarUtils.monthAfter(CalendarUtils.NO_TIME_MILLIS, march20.getTimeInMillis()));
        assertFalse(CalendarUtils.monthAfter(march20.getTimeInMillis(), march20.getTimeInMillis()));
        assertFalse(CalendarUtils.monthAfter(march20.getTimeInMillis(), march30.getTimeInMillis()));
        assertFalse(CalendarUtils.monthAfter(march20.getTimeInMillis(), may20.getTimeInMillis()));
        assertTrue(CalendarUtils.monthAfter(may20.getTimeInMillis(), march20.getTimeInMillis()));
    }

    @Test
    public void testDayOfMonth() {
        assertThat(CalendarUtils.dayOfMonth(CalendarUtils.NO_TIME_MILLIS)).isEqualTo(-1);
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        assertThat(CalendarUtils.dayOfMonth(march20.getTimeInMillis())).isEqualTo(20);
    }

    @Test
    public void testAddMonth() {
        assertThat(CalendarUtils.addMonths(CalendarUtils.NO_TIME_MILLIS, 1))
                .isEqualTo(CalendarUtils.NO_TIME_MILLIS);
        Calendar december1 = Calendar.getInstance();
        december1.set(2016, Calendar.DECEMBER, 1);
        Calendar january1 = Calendar.getInstance();
        january1.set(2017, Calendar.JANUARY, 1);
        long actual = CalendarUtils.addMonths(december1.getTimeInMillis(), 1);
        Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actual);
        assertThat(actualCalendar.get(Calendar.YEAR)).isEqualTo(2017);
        assertThat(actualCalendar.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
        assertThat(actualCalendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(1);
    }

    @Test
    public void testMonthFirstDay() {
        assertThat(CalendarUtils.monthFirstDay(CalendarUtils.NO_TIME_MILLIS))
                .isEqualTo(CalendarUtils.NO_TIME_MILLIS);
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        Calendar expected = Calendar.getInstance();
        expected.set(2016, Calendar.MARCH, 1);
        expected.set(Calendar.HOUR_OF_DAY, 0);
        expected.set(Calendar.MINUTE, 0);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertThat(CalendarUtils.monthFirstDay(march20.getTimeInMillis()))
                .isEqualTo(expected.getTimeInMillis());
    }

    @Test
    public void testMonthLastDay() {
        assertThat(CalendarUtils.monthLastDay(CalendarUtils.NO_TIME_MILLIS))
                .isEqualTo(CalendarUtils.NO_TIME_MILLIS);
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        Calendar expected = Calendar.getInstance();
        expected.set(2016, Calendar.MARCH, 31);
        expected.set(Calendar.HOUR_OF_DAY, 0);
        expected.set(Calendar.MINUTE, 0);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertThat(CalendarUtils.monthLastDay(march20.getTimeInMillis()))
                .isEqualTo(expected.getTimeInMillis());
    }

    @Test
    public void testMonthSize() {
        assertThat(CalendarUtils.monthSize(CalendarUtils.NO_TIME_MILLIS))
                .isEqualTo(0);
        Calendar march20 = Calendar.getInstance();
        march20.set(2016, Calendar.MARCH, 20);
        assertThat(CalendarUtils.monthSize(march20.getTimeInMillis()))
                .isEqualTo(31);
    }

    @Test
    public void testMonthFirstDayOffset() {
        assertThat(CalendarUtils.monthFirstDayOffset(CalendarUtils.NO_TIME_MILLIS))
                .isEqualTo(0);
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        Calendar march = Calendar.getInstance();
        march.set(2016, Calendar.MARCH, 1);
        assertThat(CalendarUtils.monthFirstDayOffset(march.getTimeInMillis()))
                .isEqualTo(2); // Tues - Sun
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testConvertTimeZone() {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Singapore")); // after UTC
        long local = System.currentTimeMillis();
        long utc = CalendarUtils.toUtcTimeZone(local);
        assertThat(local).isLessThan(utc);
        assertThat(local).isEqualTo(CalendarUtils.toLocalTimeZone(utc));
        TimeZone.setDefault(defaultTimeZone);
    }
}
