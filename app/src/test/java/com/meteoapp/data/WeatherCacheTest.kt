package com.meteoapp.data

import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.CurrentData
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
@Config(sdk = [33])
class WeatherCacheTest {

    private lateinit var cache: WeatherCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        cache = WeatherCache(context)
    }

    @Test
    fun load_returnsNullWhenNothingSaved() {
        assertNull(cache.load(48.85, 2.35))
    }

    @Test
    fun ageMillis_returnsMaxWhenNothingSaved() {
        assertEquals(Long.MAX_VALUE, cache.ageMillis(48.85, 2.35))
    }

    @Test
    fun saveThenLoad_roundTripsData() {
        val data = sampleWeather(48.85, 2.35, 18.5)

        cache.save(48.85, 2.35, data)

        val loaded = cache.load(48.85, 2.35)
        assertNotNull(loaded)
        assertEquals(48.85, loaded!!.lat, 0.001)
        assertEquals(2.35, loaded.lon, 0.001)
        assertEquals(18.5, loaded.current.temp, 0.001)
        assertEquals("Paris", loaded.timezone)
    }

    @Test
    fun ageMillis_isSmallAfterSave() {
        cache.save(48.85, 2.35, sampleWeather(48.85, 2.35, 18.5))

        val age = cache.ageMillis(48.85, 2.35)
        assertTrue("age should be small after a fresh save", age < 5000L)
    }

    @Test
    fun save_isKeyedByCoordinates() {
        cache.save(48.85, 2.35, sampleWeather(48.85, 2.35, 18.5))
        cache.save(40.0, -3.7, sampleWeather(40.0, -3.7, 25.0))

        val paris = cache.load(48.85, 2.35)
        val madrid = cache.load(40.0, -3.7)

        assertNotNull(paris)
        assertNotNull(madrid)
        assertEquals(18.5, paris!!.current.temp, 0.001)
        assertEquals(25.0, madrid!!.current.temp, 0.001)
    }

    @Test
    fun load_returnsNullForDifferentKey() {
        cache.save(48.85, 2.35, sampleWeather(48.85, 2.35, 18.5))

        assertNull(cache.load(40.0, -3.7))
    }

    private fun sampleWeather(lat: Double, lon: Double, temp: Double): WeatherData {
        return WeatherData(
            lat = lat,
            lon = lon,
            timezone = "Paris",
            timezoneOffset = 3600,
            current = CurrentData(
                dt = 1700000000,
                sunrise = 1699980000,
                sunset = 1700010000,
                temp = temp,
                feelsLike = temp - 1.3,
                pressure = 1015,
                humidity = 60,
                visibility = 10000,
                windSpeed = 3.6,
                windDeg = 200,
                weather = listOf(
                    WeatherCondition(
                        id = 800,
                        main = "Clear",
                        description = "ciel degage",
                        icon = "01d"
                    )
                ),
                timezoneOffset = 3600
            ),
            hourly = emptyList(),
            daily = emptyList()
        )
    }
}
