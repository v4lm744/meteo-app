package com.meteoapp.data

import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.api.OpenWeatherApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.meteoapp.data.api.ApiClient

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeatherRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api: OpenWeatherApi = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(ApiClient.moshi))
            .build()
            .create(OpenWeatherApi::class.java)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key")
        repository = WeatherRepository(context, api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun isApiKeyConfigured_trueWhenKeySet() {
        assertTrue(repository.isApiKeyConfigured)
    }

    @Test
    fun getWeather_returnsErrorWhenApiKeyMissing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "")
        val repo = WeatherRepository(context, buildApi())

        val result = repo.getWeather(48.85, 2.35)

        assertTrue(result is WeatherResult.Error)
        assertFalse((result as WeatherResult.Error).cityNotFound)
    }

    @Test
    fun getWeather_returnsSuccessAndMapsCurrentAndDaily() {
        server.enqueue(MockResponse().setBody(CURRENT_JSON))
        server.enqueue(MockResponse().setBody(FORECAST_JSON))

        val result = repository.getWeather(48.85, 2.35)

        assertTrue(result is WeatherResult.Success)
        val data = (result as WeatherResult.Success).data
        assertEquals(48.85, data.lat, 0.001)
        assertEquals(18.5, data.current.temp, 0.001)
        assertEquals(17.2, data.current.feelsLike, 0.001)
        assertEquals("Paris", data.timezone)
        assertEquals(24, data.hourly.size)
        assertEquals(200L, data.current.windDeg)
        assertEquals(3.6, data.current.windSpeed, 0.001)
    }

    @Test
    fun getWeather_returnsErrorOnHttp404() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

        val result = repository.getWeather(0.0, 0.0)

        assertTrue(result is WeatherResult.Error)
        assertTrue((result as WeatherResult.Error).cityNotFound)
    }

    @Test
    fun searchCity_returnsResults() {
        server.enqueue(MockResponse().setBody(GEO_JSON))

        val result = repository.searchCity("Paris")

        assertTrue(result is WeatherResult.Success)
        val cities = (result as WeatherResult.Success).data
        assertEquals(1, cities.size)
        assertEquals("Paris", cities.first().name)
        assertEquals("FR", cities.first().country)
    }

    @Test
    fun searchCity_returnsEmptyForBlankQuery() {
        val result = repository.searchCity("   ")

        assertTrue(result is WeatherResult.Success)
        assertTrue((result as WeatherResult.Success).data.isEmpty())
    }

    @Test
    fun getRegionCities_mapsResponse() {
        server.enqueue(MockResponse().setBody(FIND_JSON))

        val result = repository.getRegionCities(48.85, 2.35)

        assertTrue(result is WeatherResult.Success)
        val cities = (result as WeatherResult.Success).data
        assertEquals(1, cities.size)
        assertEquals("Paris", cities.first().name)
        assertEquals("01d", cities.first().weatherIcon)
    }

    private fun buildApi(): OpenWeatherApi = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(MoshiConverterFactory.create(ApiClient.moshi))
        .build()
        .create(OpenWeatherApi::class.java)

    private val CURRENT_JSON = """
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

    private val FORECAST_JSON = buildString {
        append("""{"cod":"200","message":0,"cnt":3,"city":{"id":1,"name":"Paris","country":"FR","timezone":3600},"list":[""")
        val baseTs = System.currentTimeMillis() / 1000L
        for (i in 0 until 24) {
            if (i > 0) append(",")
            append("""
                {"dt": ${baseTs + i * 3600L},
                 "main": {"temp": 10.0, "feels_like": 9.0, "pressure": 1000, "humidity": 70},
                 "weather": [{"id": 500, "main": "Rain", "description": "pluie légère", "icon": "10d"}],
                 "wind": {"speed": 4.0, "deg": 180},
                 "pop": 0.5}
            """.trimIndent())
        }
        append("]}")
    }

    private val GEO_JSON = """
        [
          {"name": "Paris", "lat": 48.85, "lon": 2.35, "country": "FR", "state": "Île-de-France"}
        ]
    """.trimIndent()

    private val FIND_JSON = """
        {
          "cod": "200",
          "count": 1,
          "list": [
            {
              "id": 1,
              "name": "Paris",
              "coord": {"lon": 2.35, "lat": 48.85},
              "main": {"temp": 18.5, "feels_like": 17.2, "pressure": 1015, "humidity": 60},
              "weather": [{"id": 800, "main": "Clear", "description": "ciel dégagé", "icon": "01d"}]
            }
          ]
        }
    """.trimIndent()
}
