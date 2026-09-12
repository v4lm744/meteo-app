package com.meteoapp.data

import com.meteoapp.BuildConfig
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.DailyData
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cityNotFound: Boolean = false) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class WeatherRepository {

    private val api = ApiClient.api

    private val apiKey: String
        get() = BuildConfig.OPEN_WEATHER_API_KEY

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherData> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        return try {
            coroutineScope {
                val current = api.getCurrentWeather(lat = lat, lon = lon, apiKey = apiKey)
                val forecast = api.getForecast(lat = lat, lon = lon, apiKey = apiKey)

                val timezoneOffset = (forecast.city.timezone ?: current.timezone ?: 0L)
                val tz = forecast.city.country ?: ""

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
                    timezoneOffset = timezoneOffset
                )

                val hourly = forecast.list.take(24).map { item ->
                    HourlyData(
                        dt = item.dt,
                        temp = item.main.temp,
                        feelsLike = item.main.feelsLike,
                        humidity = item.main.humidity,
                        windSpeed = item.wind.speed,
                        windDeg = item.wind.deg,
                        weather = item.weather,
                        pop = item.pop,
                        timezoneOffset = timezoneOffset
                    )
                }

                val daily = buildDailyList(forecast.list, timezoneOffset)

                Result.Success(
                    WeatherData(
                        lat = lat,
                        lon = lon,
                        timezone = tz,
                        timezoneOffset = timezoneOffset,
                        current = currentData,
                        hourly = hourly,
                        daily = daily
                    )
                )
            }
        } catch (e: retrofit2.HttpException) {
            Result.Error(
                message = if (e.code() == 401 || e.code() == 403) {
                    "Clé API invalide ou non encore activée"
                } else if (e.code() == 404) {
                    "Ville introuvable"
                } else {
                    "Erreur serveur (${e.code()})"
                },
                cityNotFound = e.code() == 404
            )
        } catch (e: Exception) {
            Result.Error("Vérifiez votre connexion internet")
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
            val cal = Calendar.getInstance().apply {
                timeInMillis = (item.dt + timezoneOffset) * 1000L
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val itemHour = cal.get(Calendar.HOUR_OF_DAY)
            val diff = Math.abs(itemHour - hour)
            if (diff < bestDiff) {
                bestDiff = diff
                best = item
            }
        }
        return best
    }

    private fun dayStartKey(timestampSeconds: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            timeInMillis = timestampSeconds * 1000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 1000L
    }

    suspend fun searchCity(query: String): Result<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        return try {
            val results = api.geocode(query = query.trim(), apiKey = apiKey)
            Result.Success(results)
        } catch (e: retrofit2.HttpException) {
            Result.Error(
                message = if (e.code() == 401 || e.code() == 403) "Clé API invalide" else "Erreur lors de la recherche"
            )
        } catch (e: Exception) {
            Result.Error("Erreur lors de la recherche")
        }
    }

    suspend fun getRegionCities(lat: Double, lon: Double): Result<List<RegionCity>> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        return try {
            val response = api.findCities(lat = lat, lon = lon, count = 10, apiKey = apiKey)
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
            Result.Success(cities)
        } catch (e: retrofit2.HttpException) {
            Result.Error(
                message = if (e.code() == 401 || e.code() == 403) "Clé API invalide" else "Erreur villes proches"
            )
        } catch (e: Exception) {
            Result.Error("Erreur villes proches")
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        return try {
            val results = api.reverseGeocode(lat = lat, lon = lon, apiKey = apiKey)
            Result.Success(results)
        } catch (e: retrofit2.HttpException) {
            Result.Error(
                message = if (e.code() == 401 || e.code() == 403) "Clé API invalide" else "Erreur lors de la localisation"
            )
        } catch (e: Exception) {
            Result.Error("Erreur lors de la localisation")
        }
    }
}
