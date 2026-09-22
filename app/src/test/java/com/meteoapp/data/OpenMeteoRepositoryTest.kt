package com.meteoapp.data

import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.api.OpenMeteoApi
import com.meteoapp.data.api.OpenWeatherApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OpenMeteoRepositoryTest {

    private lateinit var openMeteoServer: MockWebServer
    private lateinit var openWeatherServer: MockWebServer

    @Before
    fun setUp() {
        openMeteoServer = MockWebServer()
        openMeteoServer.start()
        openWeatherServer = MockWebServer()
        openWeatherServer.start()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "")
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    @After
    fun tearDown() {
        openMeteoServer.shutdown()
        openWeatherServer.shutdown()
    }

    private fun repository(): WeatherRepository {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val openMeteo: OpenMeteoApi = Retrofit.Builder()
            .baseUrl(openMeteoServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(ApiClient.moshi))
            .build()
            .create(OpenMeteoApi::class.java)
        val openWeather: OpenWeatherApi = Retrofit.Builder()
            .baseUrl(openWeatherServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(ApiClient.moshi))
            .build()
            .create(OpenWeatherApi::class.java)
        return WeatherRepository(
            context,
            api = openWeather,
            openMeteoApi = openMeteo,
            lang = "fr"
        )
    }

    @Test
    fun getWeather_succeedsWithoutApiKeyViaOpenMeteo() {
        openMeteoServer.enqueue(MockResponse().setBody(OPEN_METEO_JSON))
        val result = runBlocking { repository().getWeather(48.85, 2.35) }
        assertTrue(result is WeatherResult.Success)
        val data = (result as WeatherResult.Success).data
        assertEquals("Europe/Paris", data.timezone)
        assertEquals(7200L, data.timezoneOffset)
        assertEquals(21.4, data.current.temp, 0.001)
        assertEquals(48, data.current.humidity)
        assertNotNull(data.current.sunrise)
        assertNotNull(data.current.sunset)
        assertTrue(data.daily.size == 2)
        assertEquals(4.8, data.daily.first().uvIndexMax!!, 0.001)
        assertTrue(data.hourly.size == 4)
        assertEquals(1.9, data.hourly.first().uvIndex!!, 0.001)
        assertTrue(data.minutely.size == 4)
        assertEquals(0.2, data.minutely.first().precipitation, 0.001)
        assertEquals(60, data.minutely.first().probabilityPercent)
        assertEquals(0, openWeatherServer.requestCount)
    }

    @Test
    fun getWeather_fallsBackToOpenWeatherWhenOpenMeteoFails() {
        openMeteoServer.enqueue(MockResponse().setResponseCode(500))
        openWeatherServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.requestUrl?.encodedPath?.contains("forecast") == true) {
                    MockResponse().setBody(OWM_FORECAST_JSON)
                } else {
                    MockResponse().setBody(OWM_CURRENT_JSON)
                }
        }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key")
        val repo = repository()
        val result = runBlocking { repo.getWeather(48.85, 2.35) }
        assertTrue(result is WeatherResult.Success)
        val data = (result as WeatherResult.Success).data
        assertEquals(18.5, data.current.temp, 0.001)
    }

    @Test
    fun getWeather_errorWhenBothSourcesFailWithoutCache() {
        openMeteoServer.enqueue(MockResponse().setResponseCode(500))
        val result = runBlocking { repository().getWeather(0.0, 0.0) }
        assertTrue(result is WeatherResult.Error)
        assertFalse((result as WeatherResult.Error).cityNotFound)
    }

    /**
     * Réponse Open-Meteo à heures relatives à l'instant du test : le
     * mapper exclut les créneaux passés, une date fixe casserait le test
     * dès le lendemain de sa rédaction.
     */
    private val OPEN_METEO_JSON: String by lazy {
        val offset = 7200L
        val nowLocal = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(offset)
        val base = nowLocal.plusHours(1).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
        val fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val times = (0 until 4).joinToString(",") { "\"" + base.plusHours(it.toLong()).format(fmt) + "\"" }
        val minutelyTimes = (0 until 4).joinToString(",") { "\"" + base.plusMinutes(it.toLong() * 15).format(fmt) + "\"" }
        val today = base.toLocalDate()
        """
        {
          "latitude": 48.85, "longitude": 2.35,
          "timezone": "Europe/Paris", "utc_offset_seconds": $offset,
          "current": {
            "time": "${nowLocal.format(fmt)}",
            "temperature_2m": 21.4, "relative_humidity_2m": 48,
            "apparent_temperature": 20.9, "is_day": 1,
            "precipitation": 0.0, "weather_code": 1,
            "cloud_cover": 11, "pressure_msl": 1016.8,
            "wind_speed_10m": 9.2, "wind_direction_10m": 220, "wind_gusts_10m": 16.6
          },
          "hourly": {
            "time": [$times],
            "temperature_2m": [21.4, 21.9, 22.3, 22.1],
            "relative_humidity_2m": [48, 47, 45, 46],
            "apparent_temperature": [20.9, 21.4, 21.8, 21.6],
            "precipitation_probability": [0, 10, 5, 0],
            "precipitation": [0.0, 0.0, 0.0, 0.0],
            "weather_code": [1, 2, 2, 1],
            "pressure_msl": [1016.8, 1016.6, 1016.4, 1016.2],
            "cloud_cover": [11, 24, 30, 20],
            "visibility": [24140, 24140, 24140, 24140],
            "wind_speed_10m": [9.2, 9.5, 8.8, 8.1],
            "wind_direction_10m": [220, 225, 230, 215],
            "uv_index": [1.9, 1.6, 1.2, 0.7]
          },
          "minutely_15": {
            "time": [$minutelyTimes],
            "precipitation": [0.2, 0.0, 0.0, 0.0],
            "precipitation_probability": [60, 10, 5, 0],
            "weather_code": [61, 1, 1, 1]
          },
          "daily": {
            "time": ["$today", "${today.plusDays(1)}"],
            "weather_code": [1, 2],
            "temperature_2m_max": [24.2, 23.5],
            "temperature_2m_min": [12.7, 13.1],
            "sunrise": ["${today}T07:36", "${today.plusDays(1)}T07:37"],
            "sunset": ["${today}T19:48", "${today.plusDays(1)}T19:46"],
            "uv_index_max": [4.8, 4.6],
            "precipitation_probability_max": [10, 20]
          }
        }
        """.trimIndent()
    }

    private val OWM_FORECAST_JSON = """
        {
          "cod": "200", "cnt": 1,
          "list": [
            {
              "dt": 1700000000,
              "main": {"temp": 18.0, "feels_like": 17.0, "pressure": 1015, "humidity": 60},
              "weather": [{"id": 800, "main": "Clear", "description": "ciel dégagé", "icon": "01d"}],
              "clouds": {"all": 0},
              "wind": {"speed": 3.0, "deg": 200},
              "pop": 0.1
            }
          ],
          "city": {"name": "Paris", "timezone": 3600}
        }
    """.trimIndent()

    private val OWM_CURRENT_JSON = """
        {
          "coord": {"lon": 2.35, "lat": 48.85},
          "weather": [{"id": 800, "main": "Clear", "description": "ciel dégagé", "icon": "01d"}],
          "main": {"temp": 18.5, "feels_like": 17.2, "pressure": 1015, "humidity": 60},
          "visibility": 10000,
          "wind": {"speed": 3.6, "deg": 200},
          "dt": 1700000000,
          "sys": {"country": "FR", "sunrise": 1699980000, "sunset": 1700010000},
          "timezone": 3600,
          "name": "Paris"
        }
    """.trimIndent()
}
