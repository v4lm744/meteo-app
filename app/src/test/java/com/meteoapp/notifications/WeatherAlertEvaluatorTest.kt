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

    private val now = 1782000000L

    /** Créneau horaire n° [index] à partir de [now], au pas de [stepHours]. */
    private fun hourlyItem(
        temp: Double,
        rainMm: Double,
        windMs: Double,
        index: Int = 0,
        stepHours: Int = 1
    ): HourlyData =
        HourlyData(
            dt = now + index * stepHours * 3600L,
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
            (0 until 12).map { hourlyItem(temp = 15.0, rainMm = 0.05, windMs = 3.0, index = it) }
        )
        val alerts = WeatherAlertEvaluator.evaluate(context, weather, now)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `rain alert when cumulative rainfall exceeds threshold`() {
        val weather = weatherWith(
            (0 until 12).map { hourlyItem(temp = 15.0, rainMm = 0.3, windMs = 3.0, index = it) }
        )
        val alerts = WeatherAlertEvaluator.evaluate(context, weather, now)
        assertEquals(1, alerts.size)
        assertEquals("rain", alerts.first().key)
    }

    @Test
    fun `wind alert when single gust exceeds threshold`() {
        val items = (0 until 12).map { index ->
            if (index == 5) hourlyItem(temp = 15.0, rainMm = 0.0, windMs = 20.0, index = index)
            else hourlyItem(temp = 15.0, rainMm = 0.0, windMs = 3.0, index = index)
        }
        val alerts = WeatherAlertEvaluator.evaluate(context, weatherWith(items), now)
        assertEquals(1, alerts.size)
        assertEquals("wind", alerts.first().key)
    }

    @Test
    fun `frost alert when temperature drops below threshold`() {
        val items = (0 until 12).map { index ->
            if (index == 7) hourlyItem(temp = -3.0, rainMm = 0.0, windMs = 1.0, index = index)
            else hourlyItem(temp = 5.0, rainMm = 0.0, windMs = 2.0, index = index)
        }
        val alerts = WeatherAlertEvaluator.evaluate(context, weatherWith(items), now)
        assertEquals(1, alerts.size)
        assertEquals("frost", alerts.first().key)
    }

    /**
     * BUG-002 : 12 créneaux Open-Meteo de 1 h à 0,15 mm = 1,8 mm, sous le
     * seuil de 2 mm. L'ancien calcul multipliait par 3 (5,4 mm, alerte).
     */
    @Test
    fun `hourly open-meteo rainfall is not tripled`() {
        val weather = weatherWith(
            (0 until 12).map { hourlyItem(temp = 15.0, rainMm = 0.15, windMs = 3.0, index = it) }
        )
        assertTrue(WeatherAlertEvaluator.evaluate(context, weather, now).isEmpty())
    }

    /**
     * BUG-002 : au pas de 3 h (OpenWeatherMap), seuls les 4 créneaux des
     * 12 prochaines heures comptent ; l'ancien take(12) couvrait 36 h.
     */
    @Test
    fun `three-hour steps only count the next twelve hours`() {
        val items = (0 until 12).map { index ->
            val rain = if (index < 4) 0.0 else 5.0
            hourlyItem(temp = 15.0, rainMm = rain, windMs = 3.0, index = index, stepHours = 3)
        }
        assertTrue(WeatherAlertEvaluator.evaluate(context, weatherWith(items), now).isEmpty())
    }

    @Test
    fun `three-hour rain volumes are summed without scaling`() {
        val items = (0 until 12).map { index ->
            val rain = if (index < 4) 0.6 else 0.0
            hourlyItem(temp = 15.0, rainMm = rain, windMs = 3.0, index = index, stepHours = 3)
        }
        val alerts = WeatherAlertEvaluator.evaluate(context, weatherWith(items), now)
        assertEquals(listOf("rain"), alerts.map { it.key })
    }

    @Test
    fun `window keeps the current slot and drops finished ones`() {
        val items = (-3 until 15).map { hourlyItem(temp = 15.0, rainMm = 0.0, windMs = 3.0, index = it) }
        val window = WeatherAlertEvaluator.alertWindow(items, now + 1800L)
        assertEquals(now, window.first().dt)
        assertEquals(now + 12 * 3600L, window.last().dt)
        assertEquals(13, window.size)
    }

    @Test
    fun `no alert from stale data entirely in the past`() {
        val weather = weatherWith(
            (0 until 12).map { hourlyItem(temp = -10.0, rainMm = 5.0, windMs = 30.0, index = it) }
        )
        val alerts = WeatherAlertEvaluator.evaluate(context, weather, now + 24 * 3600L)
        assertTrue(alerts.isEmpty())
    }
}
