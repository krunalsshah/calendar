package io.github.hidroh.calendar;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.util.ActivityController;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        verify(activity.permissionRequester).requestPermissions();
        assertThat(activity.findViewById(R.id.fab)).isNotVisible();
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
    }

    @Test
    public void testGrantPermissions() {
        activity.permissionCheckResult = PackageManager.PERMISSION_GRANTED;
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.fab)).isVisible();
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
    }

    @Test
    public void testDenyPermissions() {
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.fab)).isNotVisible();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testRequestPermissions() {
        // denied
        verify(activity.permissionRequester).requestPermissions();
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.button_permission)).isVisible();

        // request via UI
        activity.findViewById(R.id.button_permission).performClick();
        verify(activity.permissionRequester, times(2)).requestPermissions();
    }

    @Test
    public void testStateRestoration() {
        // denied
        verify(activity.permissionRequester).requestPermissions();
        activity.onRequestPermissionsResult(0, new String[0], new int[0]);
        assertThat(activity.findViewById(R.id.empty)).isVisible();

        // recreating should not prompt for permission again, show empty UI instead
        activity.shadowRecreate();
        verify(activity.permissionRequester).requestPermissions();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @After
    public void tearDown() {
        controller.stop().destroy();
    }

    static class TestMainActivity extends MainActivity {
        int permissionCheckResult = PackageManager.PERMISSION_DENIED;
        final PermissionRequester permissionRequester = mock(PermissionRequester.class);

        @Override
        protected int checkPermission(@NonNull String permission) {
            return permissionCheckResult;
        }

        @Override
        protected void requestPermissions() {
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
