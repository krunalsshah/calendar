package io.github.hidroh.calendar;

import android.annotation.SuppressLint;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import io.github.hidroh.calendar.test.shadows.ShadowLinearLayoutManager;
import io.github.hidroh.calendar.test.shadows.ShadowRecyclerView;
import io.github.hidroh.calendar.weather.WeatherSyncService;
import io.github.hidroh.calendar.widget.AgendaView;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerView.class, ShadowLinearLayoutManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class MainActivityWeatherTest {
    private ActivityController<TestMainActivity> controller;
    private TestMainActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestMainActivity.class);
        activity = controller.get();
    }

    @Test
    public void testEnableWeather() {
        // initial state
        assertFalse(PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, false));

        // enabling weather should prompt for permissions
        controller.create().start().postCreate(null).resume().visible();
        shadowOf(activity).clickMenuItem(R.id.action_weather);
        verify(activity.permissionRequester).requestPermissions();

        // granting permissions should persist preference
        activity.permissionCheckResult = true;
        activity.onRequestPermissionsResult(1, new String[0], new int[0]);
        assertTrue(PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, false));
    }

    @Test
    public void testEnableWeatherDenyPermissions() {
        // initial state
        assertFalse(PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, false));

        // enabling weather should prompt for permissions
        controller.create().start().postCreate(null).resume().visible();
        shadowOf(activity).clickMenuItem(R.id.action_weather);
        verify(activity.permissionRequester).requestPermissions();

        // denying permissions should not persist preference and show explanation
        activity.onRequestPermissionsResult(1, new String[0], new int[0]);
        assertFalse(PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, false));

        // can be flaky here as SnackBar may disappear due to animation
        assertThat(activity.findViewById(R.id.snackbar_action)).isVisible();

        // granting permissions through SnackBar action should persist preference
        //noinspection ConstantConditions
        activity.findViewById(R.id.snackbar_action).performClick();
        verify(activity.permissionRequester, times(2)).requestPermissions();
        activity.permissionCheckResult = true;
        activity.onRequestPermissionsResult(1, new String[0], new int[0]);
        assertTrue(PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, false));
    }

    @Test
    public void testToggleWeatherOption() {
        activity.permissionCheckResult = true;
        controller.create().start().postCreate(null).resume().visible();

        // toggling on-off-on again should not prompt for permissions, which has been granted
        shadowOf(activity).clickMenuItem(R.id.action_weather);
        shadowOf(activity).clickMenuItem(R.id.action_weather);
        verify(activity.permissionRequester, never()).requestPermissions();
    }

    @Test
    public void testWeatherEnabledButMissingPermissions() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, true)
                .apply();
        controller.create().start().postCreate(null).resume().visible();

        // can be flaky here as SnackBar may disappear due to animation
        assertThat(activity.findViewById(R.id.snackbar_action)).isVisible();
    }

    @Test
    public void testWeatherUpdate() {
        // initial state with no weather information
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(WeatherSyncService.PREF_WEATHER_ENABLED, true)
                .apply();
        activity.permissionCheckResult = true;
        controller.create().start().postCreate(null).resume().visible();
        assertThat(createBindFirstViewHolder().itemView.findViewById(R.id.weather)).isNotVisible();

        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(WeatherSyncService.PREF_WEATHER_TODAY,
                        "clear-day|86.0|clear-day|86.0|clear-day|86.0")
                .putString(WeatherSyncService.PREF_WEATHER_TOMORROW,
                        "clear-day|86.0|clear-day|86.0|clear-day|86.0")
                .apply();
        assertThat(createBindFirstViewHolder().itemView.findViewById(R.id.weather)).isVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @NonNull
    private RecyclerView.ViewHolder createBindFirstViewHolder() {
        AgendaView agendaView = (AgendaView) activity.findViewById(R.id.agenda_view);
        RecyclerView.Adapter adapter = agendaView.getAdapter();
        int firstPosition = ((LinearLayoutManager) agendaView.getLayoutManager())
                .findFirstVisibleItemPosition();
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(agendaView,
                adapter.getItemViewType(firstPosition));
        adapter.bindViewHolder(viewHolder, firstPosition);
        return viewHolder;
    }

    @SuppressLint("Registered")
    static class TestMainActivity extends MainActivity {
        boolean permissionCheckResult = false;
        final PermissionRequester permissionRequester = mock(PermissionRequester.class);

        @Override
        protected boolean checkCalendarPermissions() {
            return true;
        }

        @Override
        protected boolean checkLocationPermissions() {
            return permissionCheckResult;
        }

        @Override
        protected void requestLocationPermissions() {
            permissionRequester.requestPermissions();
        }
    }

    interface PermissionRequester {
        void requestPermissions();
    }
}
