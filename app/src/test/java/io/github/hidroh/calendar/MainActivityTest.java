package io.github.hidroh.calendar;

import android.widget.CheckedTextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.util.ActivityController;

import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private CheckedTextView toggle;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(MainActivity.class);
        controller.create().start().resume().visible();
        activity = controller.get();
        toggle = (CheckedTextView) activity.findViewById(R.id.toolbar_toggle);
    }

    @Test
    public void testToolbarToggle() {
        // initial state
        assertToggleOff();

        // toggle on
        toggle.performClick();
        assertToggleOn();

        // toggle off
        toggle.performClick();
        assertToggleOff();
    }

    @Test
    public void testStateRestoration() {
        // initial state
        assertToggleOff();

        // recreate
        shadowOf(activity).recreate();
        assertToggleOff();

        // toggle on
        toggle = (CheckedTextView) activity.findViewById(R.id.toolbar_toggle);
        toggle.performClick();
        assertToggleOn();

        // recreate
        shadowOf(activity).recreate();
        assertToggleOn();
    }

    @Test
    public void testOptionsItemBack() {
        assertThat(activity).isNotFinishing();
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertToggleOn() {
        assertThat(toggle).isChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isVisible();
    }

    private void assertToggleOff() {
        assertThat(toggle).isNotChecked();
        assertThat(activity.findViewById(R.id.calendar_view)).isNotVisible();
    }
}
