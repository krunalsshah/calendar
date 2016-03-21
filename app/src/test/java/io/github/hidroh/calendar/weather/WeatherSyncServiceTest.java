package io.github.hidroh.calendar.weather;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.util.ServiceController;

import java.io.IOException;

import io.github.hidroh.calendar.CalendarUtils;
import retrofit2.Call;
import retrofit2.Response;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class WeatherSyncServiceTest {
    private ServiceController<TestService> controller;
    private TestService service;

    @Before
    public void setUp() {
        controller = Robolectric.buildService(TestService.class);
        service = controller.attach().create().get();
    }

    @Test
    public void testNoLocation() {
        // initial state
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();

        // trigger service with no location
        service.location = null;
        controller.startCommand(0, 0);
        verify(service.webService, never()).forecast(anyDouble(), anyDouble(), anyLong());
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchWithException() throws IOException {
        // initial state
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();

        // trigger service that generates exception
        Call faultyCall = mock(Call.class);
        when(faultyCall.execute()).thenThrow(IOException.class);
        when(service.webService.forecast(anyDouble(), anyDouble(), anyLong()))
                .thenReturn(faultyCall);
        controller.startCommand(0, 0);

        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();
    }
    @Test
    public void testFetchForecast() throws IOException {
        // initial state
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();
        setForecastResponse(createForecast(true));

        // trigger service
        controller.startCommand(0, 0);

        // service should fetch and persist response to shared preferences
        long todaySeconds = CalendarUtils.today() / DateUtils.SECOND_IN_MILLIS,
                tomorrowSeconds = todaySeconds + DateUtils.DAY_IN_MILLIS / DateUtils.SECOND_IN_MILLIS;
        verify(service.webService).forecast(anyDouble(), anyDouble(), eq(todaySeconds));
        verify(service.webService).forecast(anyDouble(), anyDouble(), eq(tomorrowSeconds));
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNotNull();
    }

    @Test
    public void testForecastMissingInfo() throws IOException {
        // initial state
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNull();
        setForecastResponse(createForecast(false));

        // trigger service
        controller.startCommand(0, 0);

        // service should still persist any available info
        assertThat(WeatherSyncService.getSyncedWeather(service)).isNotNull();
    }

    @Test
    public void testAlarm() throws IOException {
        // initial state
        ShadowAlarmManager alarmManager = shadowOf((AlarmManager) service
                .getSystemService(Context.ALARM_SERVICE));
        assertThat(alarmManager.getScheduledAlarms()).isEmpty();

        // trigger service should schedule alarm
        setForecastResponse(createForecast(true));
        controller.startCommand(0, 0);

        assertThat(alarmManager.getScheduledAlarms()).hasSize(1);
        assertThat(alarmManager.getNextScheduledAlarm().triggerAtTime)
                .isGreaterThanOrEqualTo(CalendarUtils.today() + AlarmManager.INTERVAL_DAY);

        // trigger service again should remove scheduled alarm and schedule new one
        controller.startCommand(0, 0);
        assertThat(alarmManager.getScheduledAlarms()).hasSize(1);
    }

    @Test
    public void testAlarmBroadcastReceiver() {
        new WeatherSyncAlarmReceiver().onReceive(RuntimeEnvironment.application, null);
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedService())
                .hasComponent(RuntimeEnvironment.application, WeatherSyncService.class);
    }

    @After
    public void tearDown() {
        controller.destroy();
    }

    private void setForecastResponse(WeatherSyncService.ForecastIOService.Forecast forecast)
            throws IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(forecast));
        //noinspection unchecked
        when(service.webService.forecast(anyDouble(), anyDouble(), anyLong())).thenReturn(call);
    }

    private WeatherSyncService.ForecastIOService.Forecast createForecast(final boolean full) {
        return new WeatherSyncService.ForecastIOService.Forecast(){{
            hourly = new WeatherSyncService.ForecastIOService.Hourly(){{
                data = new WeatherSyncService.ForecastIOService.DataPoint[24];
                if (!full) {
                    data[8] = new WeatherSyncService.ForecastIOService.DataPoint() {{
                        icon = "clear-day";
                        temperature = 86.0F;
                    }};
                    data[14] = new WeatherSyncService.ForecastIOService.DataPoint(){{
                        icon = "rain";
                        temperature = 86.0F;
                    }};
                    data[20] = new WeatherSyncService.ForecastIOService.DataPoint(){{
                        icon = "clear-night";
                        temperature = 86.0F;
                    }};
                }
            }};
        }};
    }

    public static class TestService extends WeatherSyncService {
        ForecastIOService webService = mock(ForecastIOService.class);
        Location location = new Location("");

        @Override
        public void onStart(Intent intent, int startId) {
            // same logic as in internal ServiceHandler.handleMessage()
            // but runs on same thread as Service
            onHandleIntent(intent);
            stopSelf(startId);
        }

        @Nullable
        @Override
        protected Location getLocation() {
            return location;
        }

        @Override
        protected ForecastIOService getForecastService() {
            return webService;
        }
    }
}
