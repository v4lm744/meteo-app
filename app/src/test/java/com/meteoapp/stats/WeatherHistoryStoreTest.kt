package com.meteoapp.stats

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.DailyData
import com.meteoapp.data.model.WeatherCondition
import com.meteoapp.data.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class WeatherHistoryStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("meteo_history_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun dayAt(daysAgo: Long, min: Double, max: Double): DailyData =
        DailyData(
            dt = WeatherHistoryStore.todayKey(0) - daysAgo * 86_400L,
            sunrise = null,
            sunset = null,
            tempMin = min,
            tempMax = max,
            morningTemp = min,
            dayTemp = (min + max) / 2,
            eveningTemp = max,
            nightTemp = min,
            feelsLikeDay = (min + max) / 2,
            pressure = 1013L,
            humidity = 60L,
            windSpeed = 2.0,
            windDeg = 200L,
            weather = listOf(WeatherCondition(id = 800L, main = "Clear", description = "ciel dégagé", icon = "01d")),
            pop = null,
            timezoneOffset = 0L
        )

    private fun weatherAt(lat: Double, lon: Double, daily: List<DailyData>): WeatherData =
        WeatherData(
            lat = lat,
            lon = lon,
            timezone = "Europe/Paris",
            timezoneOffset = 0L,
            current = CurrentData(
                dt = System.currentTimeMillis() / 1000L,
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
            hourly = emptyList(),
            daily = daily
        )

    @Test
    fun `record and read daily min max`() {
        WeatherHistoryStore.record(
            context,
            weatherAt(45.75, 4.85, listOf(dayAt(0, 5.0, 15.0)))
        )
        val records = WeatherHistoryStore.getRecords(context, 45.75, 4.85)
        assertEquals(1, records.size)
        assertEquals(5.0, records.first().tempMin, 0.001)
        assertEquals(15.0, records.first().tempMax, 0.001)
    }

    @Test
    fun `same day records merge extremes`() {
        WeatherHistoryStore.record(
            context,
            weatherAt(45.75, 4.85, listOf(dayAt(0, 8.0, 12.0)))
        )
        WeatherHistoryStore.record(
            context,
            weatherAt(45.75, 4.85, listOf(dayAt(0, 3.0, 18.0)))
        )
        val records = WeatherHistoryStore.getRecords(context, 45.75, 4.85)
        assertEquals(1, records.size)
        assertEquals(3.0, records.first().tempMin, 0.001)
        assertEquals(18.0, records.first().tempMax, 0.001)
    }

    @Test
    fun `different city uses different key`() {
        WeatherHistoryStore.record(
            context,
            weatherAt(45.75, 4.85, listOf(dayAt(0, 5.0, 15.0)))
        )
        val other = WeatherHistoryStore.getRecords(context, 48.85, 2.35)
        assertTrue(other.isEmpty())
    }

    @Test
    fun `all time records`() {
        WeatherHistoryStore.record(
            context,
            weatherAt(
                45.75, 4.85,
                listOf(dayAt(0, 10.0, 20.0), dayAt(1, 2.0, 12.0))
            )
        )
        val min = WeatherHistoryStore.allTimeMin(context, 45.75, 4.85)
        val max = WeatherHistoryStore.allTimeMax(context, 45.75, 4.85)
        assertNotNull(min)
        assertNotNull(max)
        assertEquals(2.0, min!!.tempMin, 0.001)
        assertEquals(20.0, max!!.tempMax, 0.001)
    }

    @Test
    fun `week comparison null with insufficient data`() {
        WeatherHistoryStore.record(
            context,
            weatherAt(45.75, 4.85, listOf(dayAt(0, 5.0, 15.0)))
        )
        assertNull(WeatherHistoryStore.weekComparison(context, 45.75, 4.85))
    }
}
