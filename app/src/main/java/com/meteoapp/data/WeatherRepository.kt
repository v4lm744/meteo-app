package com.meteoapp.data

import com.meteoapp.R
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.api.OpenMeteoApi
import com.meteoapp.data.api.OpenWeatherApi
import com.meteoapp.data.model.AirPollutionResponse
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.DailyData
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

sealed class WeatherResult<out T> {
    /**
     * [stale] signale des données servies du cache suite à un échec
     * réseau (bannière hors ligne) ; un cache frais servi sans appel
     * réseau reste `stale = false`.
     */
    data class Success<T>(
        val data: T,
        val fromCache: Boolean = false,
        val stale: Boolean = false
    ) : WeatherResult<T>()
    data class Error(val message: String, val cityNotFound: Boolean = false) : WeatherResult<Nothing>()
    object Loading : WeatherResult<Nothing>()
}

class WeatherRepository(
    context: android.content.Context,
    api: OpenWeatherApi = ApiClient.api,
    openMeteoApi: OpenMeteoApi = ApiClient.openMeteoApi,
    private val lang: String = context.resources.configuration.locales[0].language
) {

    companion object {
        const val FRESH_CACHE_MAX_AGE_MS = 10 * 60 * 1000L
        private const val RATE_LIMIT_BACKOFF_MS = 15_000L
    }

    private val appContext = context.applicationContext
    private val api = api
    private val openMeteoApi = openMeteoApi
    private val cache = WeatherCache(appContext)

    private val apiKey: String
        get() = ApiKeyStore.getApiKey(appContext)

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun getWeather(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean = false
    ): WeatherResult<WeatherData> = fetchWeather(lat, lon, forceRefresh, allowRateLimitRetry = true)

    private suspend fun fetchWeather(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean,
        allowRateLimitRetry: Boolean
    ): WeatherResult<WeatherData> {
        if (!forceRefresh) {
            val fresh = withContext(Dispatchers.IO) { cache.loadIfFresh(lat, lon) }
            if (fresh != null) {
                return WeatherResult.Success(fresh, fromCache = true, stale = false)
            }
        }
        val openMeteoResult = fetchWeatherFromOpenMeteo(lat, lon)
        if (openMeteoResult is WeatherResult.Success) {
            return openMeteoResult
        }
        if (!isApiKeyConfigured) {
            return WeatherResult.Error(appContext.getString(R.string.error_api_key_not_configured))
        }
        return try {
            coroutineScope {
                val currentDeferred = async { api.getCurrentWeather(lat = lat, lon = lon, apiKey = apiKey, lang = lang) }
                val forecastDeferred = async { api.getForecast(lat = lat, lon = lon, apiKey = apiKey, lang = lang) }
                val current = currentDeferred.await()
                val forecast = forecastDeferred.await()

                val timezoneOffset = (forecast.city.timezone ?: current.timezone ?: 0L)
                val tz = forecast.city.name ?: current.name ?: ""

                val currentData = CurrentData(
                    dt = current.dt,
                    sunrise = current.sys?.sunrise,
                    sunset = current.sys?.sunset,
                    temp = current.main.temp,
                    feelsLike = current.main.feelsLike,
                    pressure = current.main.pressure,
                    humidity = current.main.humidity,
                    visibility = current.visibility,
                    windSpeed = current.wind.speed,
                    windDeg = current.wind.deg,
                    weather = current.weather,
                    cloudiness = current.clouds?.all ?: 0L,
                    timezoneOffset = timezoneOffset
                )

                val hourly = forecast.list.take(16).map { item ->
                    HourlyData(
                        dt = item.dt,
                        temp = item.main.temp,
                        feelsLike = item.main.feelsLike,
                        humidity = item.main.humidity,
                        windSpeed = item.wind.speed,
                        windDeg = item.wind.deg,
                        weather = item.weather,
                        pop = item.pop,
                        timezoneOffset = timezoneOffset,
                        pressure = item.main.pressure,
                        cloudiness = item.clouds?.all ?: 0L,
                        visibility = null,
                        rainVolume = item.rain?.threeHour ?: item.rain?.oneHour ?: 0.0,
                        snowVolume = item.snow?.threeHour ?: item.snow?.oneHour ?: 0.0
                    )
                }

                val daily = buildDailyList(forecast.list, timezoneOffset)

                val weatherData = WeatherData(
                    lat = lat,
                    lon = lon,
                    timezone = tz,
                    timezoneOffset = timezoneOffset,
                    current = currentData,
                    hourly = hourly,
                    daily = daily
                )
                withContext(Dispatchers.IO) { cache.save(lat, lon, weatherData) }
                WeatherResult.Success(weatherData)
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                val cached = withContext(Dispatchers.IO) { cache.load(lat, lon) }
                if (cached != null) {
                    return WeatherResult.Success(cached, fromCache = true, stale = true)
                }
                if (allowRateLimitRetry) {
                    kotlinx.coroutines.delay(RATE_LIMIT_BACKOFF_MS)
                    return fetchWeather(lat, lon, forceRefresh = true, allowRateLimitRetry = false)
                }
            }
            WeatherResult.Error(
                message = if (e.code() == 401 || e.code() == 403) {
                    appContext.getString(R.string.error_api_key_invalid)
                } else if (e.code() == 404) {
                    appContext.getString(R.string.error_city_not_found_short)
                } else {
                    appContext.getString(R.string.error_server, e.code())
                },
                cityNotFound = e.code() == 404
            )
        } catch (e: Exception) {
            val cached = withContext(Dispatchers.IO) { cache.load(lat, lon) }
            if (cached != null) {
                WeatherResult.Success(cached, fromCache = true, stale = true)
            } else {
                WeatherResult.Error(appContext.getString(R.string.error_network))
            }
        }
    }

    /**
     * Source principale des prévisions : Open-Meteo (gratuit, sans clé) —
     * pas horaire réel de 1 h, 7 jours réels avec lever/coucher par jour
     * et indice UV fourni par l'API. En cas d'échec, l'appelant retombe
     * sur OpenWeatherMap.
     */
    private suspend fun fetchWeatherFromOpenMeteo(
        lat: Double,
        lon: Double
    ): WeatherResult<WeatherData> {
        return try {
            val response = openMeteoApi.getForecast(lat = lat, lon = lon)
            val weatherData = OpenMeteoMapper.toWeatherData(response, lang)
            withContext(Dispatchers.IO) { cache.save(lat, lon, weatherData) }
            WeatherResult.Success(weatherData)
        } catch (e: Exception) {
            WeatherResult.Error(appContext.getString(R.string.error_network))
        }
    }

    private fun buildDailyList(
        items: List<com.meteoapp.data.model.ForecastItem>,
        timezoneOffset: Long
    ): List<DailyData> {
        val byDay = LinkedHashMap<Long, MutableList<com.meteoapp.data.model.ForecastItem>>()
        for (item in items) {
            val dayKey = dayStartKey(item.dt + timezoneOffset)
            byDay.getOrPut(dayKey) { mutableListOf() }.add(item)
        }

        val todayKey = dayStartKey(System.currentTimeMillis() / 1000 + timezoneOffset)
        val sorted = byDay.entries
            .filter { it.key >= todayKey }
            .sortedBy { it.key }
            .take(7)

        return sorted.map { (dayKey, dayItems) ->
            val tempMin = dayItems.minOf { it.main.temp }
            val tempMax = dayItems.maxOf { it.main.temp }
            val pop = dayItems.maxOf { it.pop ?: 0.0 }
            val pressure = dayItems.first().main.pressure
            val humidity = dayItems.first().main.humidity
            val wind = dayItems.first().wind

            val morning = dayItems.closestToHour(9, timezoneOffset)?.main?.temp ?: dayItems.first().main.temp
            val day = dayItems.closestToHour(15, timezoneOffset)?.main?.temp ?: dayItems.first().main.temp
            val evening = dayItems.closestToHour(21, timezoneOffset)?.main?.temp ?: dayItems.first().main.temp
            val night = dayItems.closestToHour(3, timezoneOffset)?.main?.temp ?: dayItems.first().main.temp
            val feelsDay = dayItems.closestToHour(15, timezoneOffset)?.main?.feelsLike ?: dayItems.first().main.feelsLike

            val weather = dayItems.closestToHour(12, timezoneOffset)?.weather
                ?: dayItems.first().weather

            DailyData(
                dt = dayKey - timezoneOffset,
                sunrise = null,
                sunset = null,
                tempMin = tempMin,
                tempMax = tempMax,
                morningTemp = morning,
                dayTemp = day,
                eveningTemp = evening,
                nightTemp = night,
                feelsLikeDay = feelsDay,
                pressure = pressure,
                humidity = humidity,
                windSpeed = wind.speed,
                windDeg = wind.deg,
                weather = weather,
                pop = if (pop > 0) pop else null,
                timezoneOffset = timezoneOffset
            )
        }
    }

    private fun List<com.meteoapp.data.model.ForecastItem>.closestToHour(
        hour: Int,
        timezoneOffset: Long
    ): com.meteoapp.data.model.ForecastItem? {
        var best: com.meteoapp.data.model.ForecastItem? = null
        var bestDiff = Int.MAX_VALUE
        for (item in this) {
            val itemHour = hourOfDayUtc(item.dt + timezoneOffset)
            val diff = Math.abs(itemHour - hour)
            if (diff < bestDiff) {
                bestDiff = diff
                best = item
            }
        }
        return best
    }

    private fun dayStartKey(timestampSeconds: Long): Long {
        return Instant.ofEpochSecond(timestampSeconds)
            .atZone(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.DAYS)
            .toEpochSecond()
    }

    private fun hourOfDayUtc(timestampSeconds: Long): Int =
        Instant.ofEpochSecond(timestampSeconds).atZone(ZoneOffset.UTC).hour

    suspend fun getAirQuality(lat: Double, lon: Double): WeatherResult<AirPollutionResponse> {
        if (!isApiKeyConfigured) {
            return WeatherResult.Error(appContext.getString(R.string.error_api_key_not_configured))
        }
        return try {
            WeatherResult.Success(api.getAirPollution(lat = lat, lon = lon, apiKey = apiKey))
        } catch (e: Exception) {
            WeatherResult.Error(appContext.getString(R.string.error_air_quality))
        }
    }

    suspend fun searchCity(query: String): WeatherResult<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return WeatherResult.Error(appContext.getString(R.string.error_api_key_not_configured))
        }
        if (query.isBlank()) {
            return WeatherResult.Success(emptyList())
        }
        return try {
            val results = api.geocode(query = query.trim(), apiKey = apiKey)
            WeatherResult.Success(results)
        } catch (e: retrofit2.HttpException) {
            WeatherResult.Error(
                message = if (e.code() == 401 || e.code() == 403) {
                    appContext.getString(R.string.error_api_key_invalid_short)
                } else {
                    appContext.getString(R.string.error_search)
                }
            )
        } catch (e: Exception) {
            WeatherResult.Error(appContext.getString(R.string.error_search))
        }
    }

    suspend fun getRegionCities(lat: Double, lon: Double): WeatherResult<List<RegionCity>> {
        if (!isApiKeyConfigured) {
            return WeatherResult.Error(appContext.getString(R.string.error_api_key_not_configured))
        }
        return try {
            val response = api.findCities(lat = lat, lon = lon, count = 10, apiKey = apiKey, lang = lang)
            val cities = response.list.map { item ->
                RegionCity(
                    id = item.id,
                    name = item.name,
                    lat = item.coord.lat,
                    lon = item.coord.lon,
                    temp = item.main.temp,
                    weatherIcon = item.weather.firstOrNull()?.icon ?: "",
                    weatherDescription = item.weather.firstOrNull()?.description ?: ""
                )
            }
            WeatherResult.Success(cities)
        } catch (e: retrofit2.HttpException) {
            WeatherResult.Error(
                message = if (e.code() == 401 || e.code() == 403) {
                    appContext.getString(R.string.error_api_key_invalid_short)
                } else {
                    appContext.getString(R.string.error_region_cities)
                }
            )
        } catch (e: Exception) {
            WeatherResult.Error(appContext.getString(R.string.error_region_cities))
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): WeatherResult<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return WeatherResult.Error(appContext.getString(R.string.error_api_key_not_configured))
        }
        return try {
            val results = api.reverseGeocode(lat = lat, lon = lon, apiKey = apiKey)
            WeatherResult.Success(results)
        } catch (e: retrofit2.HttpException) {
            WeatherResult.Error(
                message = if (e.code() == 401 || e.code() == 403) {
                    appContext.getString(R.string.error_api_key_invalid_short)
                } else {
                    appContext.getString(R.string.error_reverse_geocode)
                }
            )
        } catch (e: Exception) {
            WeatherResult.Error(appContext.getString(R.string.error_reverse_geocode))
        }
    }
}
