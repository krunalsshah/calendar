package io.github.hidroh.calendar.test.assertions;

import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.Assertions;

import java.util.Calendar;

public class DayTimeAssert extends AbstractLongAssert<DayTimeAssert> {

    public static DayTimeAssert assertThat(long actual) {
        return new DayTimeAssert(actual, DayTimeAssert.class);
    }

    protected DayTimeAssert(long actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public DayTimeAssert isInSameMonthAs(long timeMillis) {
        isNotNull();
        Calendar actualCalendar = toCalendar(actual), expectedCalendar = toCalendar(timeMillis);
        int actualYear = actualCalendar.get(Calendar.YEAR),
                actualMonth = actualCalendar.get(Calendar.MONTH),
                expectedYear = expectedCalendar.get(Calendar.YEAR),
                expectedMonth = expectedCalendar.get(Calendar.MONTH);
        Assertions.assertThat(actualYear)
                .overridingErrorMessage("Expected <%s/%s> but was <%s/%s>",
                        expectedMonth + 1, expectedYear, actualMonth + 1, actualYear)
                .isEqualTo(expectedYear);
        Assertions.assertThat(actualMonth)
                .overridingErrorMessage("Expected <%s/%s> but was <%s/%s>",
                        expectedMonth + 1, expectedYear, actualMonth + 1, actualYear)
                .isEqualTo(expectedMonth);
        return this;
    }

    public DayTimeAssert isMonthsAfter(long timeMillis, int month) {
        Calendar expected = toCalendar(timeMillis);
        expected.add(Calendar.MONTH, month);
        isInSameMonthAs(expected.getTimeInMillis());
        return this;
    }

    public DayTimeAssert isMonthsBefore(long timeMillis, int month) {
        isMonthsAfter(timeMillis, -month);
        return this;
    }

    public DayTimeAssert isFirstDayOf(long monthMillis) {
        isInSameMonthAs(monthMillis);
        int actualDay = toCalendar(actual).get(Calendar.DAY_OF_MONTH);
        Assertions.assertThat(actualDay)
                .overridingErrorMessage("Expected day to be equal to <1> but was <%s>", actualDay)
                .isEqualTo(1);
        return this;
    }

    public DayTimeAssert isNotFirstDayOf(long monthMillis) {
        isInSameMonthAs(monthMillis);
        int actualDay = toCalendar(actual).get(Calendar.DAY_OF_MONTH);
        Assertions.assertThat(actualDay)
                .overridingErrorMessage("Expected day not be equal to <1> but was <%s>", actualDay)
                .isNotEqualTo(1);
        return this;
    }

    private Calendar toCalendar(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return calendar;
    }
}
