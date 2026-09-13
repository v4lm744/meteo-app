package com.meteoapp.data.model

import com.meteoapp.data.api.ApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherModelsTest {

    private val moshi = ApiClient.moshi

    @Test
    fun currentWeatherResponse_parsesFromJson() {
        val json = """
            {
              "coord": {"lon": 2.35, "lat": 48.85},
              "weather": [
                {"id": 800, "main": "Clear", "description": "ciel dégagé", "icon": "01d"}
              ],
              "base": "stations",
              "main": {
                "temp": 18.5,
                "feels_like": 17.2,
                "temp_min": 15.0,
                "temp_max": 21.0,
                "pressure": 1015,
                "humidity": 60
              },
              "visibility": 10000,
              "wind": {"speed": 3.6, "deg": 200, "gust": 5.0},
              "clouds": {"all": 0},
              "dt": 1700000000,
              "sys": {"country": "FR", "sunrise": 1699980000, "sunset": 1700010000},
              "timezone": 3600,
              "name": "Paris"
            }
        """.trimIndent()

        val adapter = moshi.adapter(CurrentWeatherResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(48.85, response!!.coord!!.lat, 0.001)
        assertEquals(800L, response.weather.first().id)
        assertEquals("ciel dégagé", response.weather.first().description)
        assertEquals(18.5, response.main.temp, 0.001)
        assertEquals(17.2, response.main.feelsLike, 0.001)
        assertEquals(1015L, response.main.pressure)
        assertEquals(60L, response.main.humidity)
        assertEquals(10000L, response.visibility)
        assertEquals(3.6, response.wind.speed, 0.001)
        assertEquals(200L, response.wind.deg)
        assertEquals(5.0, response.wind.gust, 0.001)
        assertEquals("FR", response.sys?.country)
        assertEquals(1699980000L, response.sys?.sunrise)
        assertEquals("Paris", response.name)
    }

    @Test
    fun currentWeatherResponse_handlesNullablesWhenMissing() {
        val json = """
            {
              "weather": [],
              "main": {"temp": 0.0, "feels_like": 0.0, "pressure": 1000, "humidity": 50},
              "wind": {"speed": 0.0, "deg": 0},
              "dt": 0
            }
        """.trimIndent()

        val response = moshi.adapter(CurrentWeatherResponse::class.java).fromJson(json)

        assertNotNull(response)
        assertNull(response!!.coord)
        assertNull(response.sys)
        assertNull(response.name)
        assertNull(response.visibility)
        assertEquals(0, response.weather.size)
    }

    @Test
    fun geoLocation_parsesLocalNames() {
        val json = """
            {
              "name": "London",
              "local_names": {"fr": "Londres", "en": "London", "feature_name": "London"},
              "lat": 51.5,
              "lon": -0.12,
              "country": "GB",
              "state": "England"
            }
        """.trimIndent()

        val geo = moshi.adapter(GeoLocation::class.java).fromJson(json)

        assertNotNull(geo)
        assertEquals("Londres", geo!!.localNames?.fr)
        assertEquals("London", geo.localNames?.en)
        assertEquals(51.5, geo.lat, 0.001)
        assertEquals(-0.12, geo.lon, 0.001)
        assertEquals("GB", geo.country)
    }

    @Test
    fun forecastResponse_parsesListAndCity() {
        val json = """
            {
              "cod": "200",
              "message": 0,
              "cnt": 1,
              "list": [
                {
                  "dt": 1700000000,
                  "main": {"temp": 10.0, "feels_like": 9.0, "pressure": 1000, "humidity": 70},
                  "weather": [{"id": 500, "main": "Rain", "description": "pluie légère", "icon": "10d"}],
                  "wind": {"speed": 4.0, "deg": 180},
                  "pop": 0.5
                }
              ],
              "city": {"id": 1, "name": "Paris", "country": "FR", "timezone": 3600}
            }
        """.trimIndent()

        val forecast = moshi.adapter(ForecastResponse::class.java).fromJson(json)

        assertNotNull(forecast)
        assertEquals(1, forecast!!.list.size)
        assertEquals(10.0, forecast.list.first().main.temp, 0.001)
        assertEquals(0.5, forecast.list.first().pop!!, 0.001)
        assertEquals("Paris", forecast.city.name)
        assertEquals(3600L, forecast.city.timezone)
    }

    @Test
    fun dailyData_roundTripsThroughJson() {
        val day = DailyData(
            dt = 1700000000L,
            sunrise = 1699980000L,
            sunset = 1700010000L,
            tempMin = 10.0,
            tempMax = 20.0,
            morningTemp = 12.0,
            dayTemp = 18.0,
            eveningTemp = 15.0,
            nightTemp = 9.0,
            feelsLikeDay = 17.0,
            pressure = 1010L,
            humidity = 65L,
            windSpeed = 3.0,
            windDeg = 90L,
            weather = listOf(WeatherCondition(800L, "Clear", "ciel dégagé", "01d")),
            pop = 0.1,
            timezoneOffset = 3600L
        )

        val adapter = moshi.adapter(DailyData::class.java)
        val json = adapter.toJson(day)
        val parsed = adapter.fromJson(json)

        assertNotNull(parsed)
        assertEquals(day.dt, parsed!!.dt)
        assertEquals(day.tempMin, parsed.tempMin, 0.001)
        assertEquals(day.tempMax, parsed.tempMax, 0.001)
        assertEquals(day.weather.first().id, parsed.weather.first().id)
        assertEquals(0.1, parsed.pop!!, 0.001)
    }
}
