package io.github.hidroh.calendar;

import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.util.ActivityController;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ConstantConditions")
@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityPermissionTest {
    private ActivityController<TestMainActivity> controller;
    private TestMainActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestMainActivity.class);
        activity = controller.create().start().postCreate(null).get();
    }

    @Test
    public void testInitialState() {
        verify(activity.permissionRequester, never()).requestPermissions();
        assertThat(activity.findViewById(R.id.fab)).isNotVisible();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @Test
    public void testGrantPermissions() {
        activity.findViewById(R.id.button_permission).performClick();
        verify(activity.permissionRequester).requestPermissions();
        activity.permissionCheckResult = true;
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.fab)).isVisible();
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
    }

    @Test
    public void testDenyPermissions() {
        activity.findViewById(R.id.button_permission).performClick();
        verify(activity.permissionRequester).requestPermissions();
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.fab)).isNotVisible();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @Test
    public void testStateRestoration() {
        // initial state
        verify(activity.permissionRequester, never()).requestPermissions();
        assertThat(activity.findViewById(R.id.empty)).isVisible();

        activity.shadowRecreate();
        verify(activity.permissionRequester, never()).requestPermissions();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @After
    public void tearDown() {
        controller.stop().destroy();
    }

    static class TestMainActivity extends MainActivity {
        boolean permissionCheckResult = false;
        final PermissionRequester permissionRequester = mock(PermissionRequester.class);

        @Override
        protected boolean checkCalendarPermissions() {
            return permissionCheckResult;
        }

        @Override
        protected void requestCalendarPermissions() {
            permissionRequester.requestPermissions();
        }

        void shadowRecreate() {
            Bundle outState = new Bundle();
            onSaveInstanceState(outState);
            onPause();
            onStop();
            onDestroy();
            onCreate(outState);
            onStart();
            onRestoreInstanceState(outState);
            onPostCreate(outState); // Robolectric misses this
            onResume();
        }
    }

    interface PermissionRequester {
        void requestPermissions();
    }
}
