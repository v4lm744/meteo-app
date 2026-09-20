package com.meteoapp.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.WeatherCondition
import com.meteoapp.data.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class WeatherAlertEvaluatorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("meteo_alert_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        AlertPrefs.setEnabled(context, true)
    }

    private fun hourlyItem(temp: Double, rainMm: Double, windMs: Double): HourlyData =
        HourlyData(
            dt = 1782000000L,
            temp = temp,
            feelsLike = temp,
            humidity = 60L,
            windSpeed = windMs,
            windDeg = 200L,
            weather = listOf(WeatherCondition(id = 500L, main = "Rain", description = "pluie", icon = "10d")),
            pop = 0.5,
            timezoneOffset = 0L,
            pressure = 1013L,
            cloudiness = 50L,
            visibility = null,
            rainVolume = rainMm,
            snowVolume = 0.0
        )

    private fun weatherWith(items: List<HourlyData>): WeatherData =
        WeatherData(
            lat = 45.75,
            lon = 4.85,
            timezone = "Europe/Paris",
            timezoneOffset = 0L,
            current = CurrentData(
                dt = 1782000000L,
                sunrise = null,
                sunset = null,
                temp = 15.0,
                feelsLike = 14.0,
                pressure = 1013L,
                humidity = 60L,
                visibility = null,
                windSpeed = 2.0,
                windDeg = 200L,
                weather = emptyList(),
                timezoneOffset = 0L
            ),
            hourly = items,
            daily = emptyList()
        )

    @Test
    fun `no alert below thresholds`() {
        val weather = weatherWith(
            (1..12).map { hourlyItem(temp = 15.0, rainMm = 0.05, windMs = 3.0) }
        )
        val alerts = WeatherAlertEvaluator.evaluate(context, weather)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `rain alert when cumulative rainfall exceeds threshold`() {
        val weather = weatherWith(
            (1..12).map { hourlyItem(temp = 15.0, rainMm = 0.3, windMs = 3.0) }
        )
        val alerts = WeatherAlertEvaluator.evaluate(context, weather)
        assertEquals(1, alerts.size)
        assertEquals("rain", alerts.first().key)
    }

    @Test
    fun `wind alert when single gust exceeds threshold`() {
        val items = (1..12).map { index ->
            if (index == 5) hourlyItem(temp = 15.0, rainMm = 0.0, windMs = 20.0)
            else hourlyItem(temp = 15.0, rainMm = 0.0, windMs = 3.0)
        }
        val alerts = WeatherAlertEvaluator.evaluate(context, weatherWith(items))
        assertEquals(1, alerts.size)
        assertEquals("wind", alerts.first().key)
    }

    @Test
    fun `frost alert when temperature drops below threshold`() {
        val items = (1..12).map { index ->
            if (index == 7) hourlyItem(temp = -3.0, rainMm = 0.0, windMs = 1.0)
            else hourlyItem(temp = 5.0, rainMm = 0.0, windMs = 2.0)
        }
        val alerts = WeatherAlertEvaluator.evaluate(context, weatherWith(items))
        assertEquals(1, alerts.size)
        assertEquals("frost", alerts.first().key)
    }
}
