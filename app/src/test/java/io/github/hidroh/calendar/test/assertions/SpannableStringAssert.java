package io.github.hidroh.calendar.test.assertions;

import android.text.SpannableString;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;

public class SpannableStringAssert
        extends AbstractCharSequenceAssert<SpannableStringAssert, SpannableString> {

    public static SpannableStringAssert assertThat(SpannableString actual) {
        return new SpannableStringAssert(actual, SpannableStringAssert.class);
    }

    protected SpannableStringAssert(SpannableString actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public SpannableStringAssert hasSpan(Class<?> type) {
        Object[] span = actual.getSpans(0, actual.length(), type);
        Assertions.assertThat(span)
                .overridingErrorMessage("Expect to have <%s> span but did not have", type.getName())
                .isNotEmpty();
        return this;
    }

    public SpannableStringAssert doesNotHaveSpan(Class<?> type) {
        Object[] span = actual.getSpans(0, actual.length(), type);
        Assertions.assertThat(span)
                .overridingErrorMessage("Expect not to have <%s> span but had", type.getName())
                .isEmpty();
        return this;
    }
}
