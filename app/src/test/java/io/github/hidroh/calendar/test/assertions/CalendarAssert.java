package io.github.hidroh.calendar.test.assertions;

import org.assertj.core.api.AbstractComparableAssert;
import org.assertj.core.api.Assertions;

import java.util.Calendar;

public class CalendarAssert extends AbstractComparableAssert<CalendarAssert, Calendar> {

    public static CalendarAssert assertThat(Calendar actual) {
        return new CalendarAssert(actual, CalendarAssert.class);
    }

    protected CalendarAssert(Calendar actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public CalendarAssert isInSameMonthAs(Calendar calendar) {
        isNotNull();
        int actualYear = actual.get(Calendar.YEAR),
                actualMonth = actual.get(Calendar.MONTH),
                expectedYear = calendar.get(Calendar.YEAR),
                expectedMonth = calendar.get(Calendar.MONTH);
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

    public CalendarAssert isMonthsAfter(Calendar calendar, int month) {
        Calendar expected = Calendar.getInstance();
        expected.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        expected.add(Calendar.MONTH, month);
        isInSameMonthAs(expected);
        return this;
    }

    public CalendarAssert isMonthsBefore(Calendar calendar, int month) {
        isMonthsAfter(calendar, -month);
        return this;
    }
}
