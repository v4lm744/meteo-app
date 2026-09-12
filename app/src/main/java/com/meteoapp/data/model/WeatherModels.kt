package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class WeatherCondition(
    val id: Long,
    val main: String,
    val description: String,
    val icon: String
)

@JsonClass(generateAdapter = false)
data class CurrentWeather(
    val dt: Long,
    val sunrise: Long?,
    val sunset: Long?,
    val temp: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    val pressure: Long,
    val humidity: Long,
    @Json(name = "dew_point") val dewPoint: Double?,
    val uvi: Double?,
    val clouds: Long?,
    val visibility: Long?,
    @Json(name = "wind_speed") val windSpeed: Double,
    @Json(name = "wind_deg") val windDeg: Long,
    @Json(name = "wind_gust") val windGust: Double?,
    val weather: List<WeatherCondition>
)

@JsonClass(generateAdapter = false)
data class HourlyForecast(
    val dt: Long,
    val temp: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    val humidity: Long,
    @Json(name = "wind_speed") val windSpeed: Double,
    @Json(name = "wind_deg") val windDeg: Long,
    val weather: List<WeatherCondition>,
    val pop: Double?,
    @Json(name = "rain") val rain: RainVolume?,
    @Json(name = "snow") val snow: SnowVolume?
)

@JsonClass(generateAdapter = false)
data class RainVolume(
    @Json(name = "1h") val oneHour: Double?
)

@JsonClass(generateAdapter = false)
data class SnowVolume(
    @Json(name = "1h") val oneHour: Double?
)

@JsonClass(generateAdapter = false)
data class DailyTemp(
    val morn: Double,
    val day: Double,
    val eve: Double,
    val night: Double,
    val min: Double,
    val max: Double
)

@JsonClass(generateAdapter = false)
data class DailyFeelsLike(
    val morn: Double,
    val day: Double,
    val eve: Double,
    val night: Double
)

@JsonClass(generateAdapter = false)
data class DailyForecast(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: DailyTemp,
    @Json(name = "feels_like") val feelsLike: DailyFeelsLike,
    val pressure: Long,
    val humidity: Long,
    @Json(name = "wind_speed") val windSpeed: Double,
    @Json(name = "wind_deg") val windDeg: Long,
    val weather: List<WeatherCondition>,
    val clouds: Long,
    val pop: Double?,
    val rain: Double?,
    val uvi: Double?
)

@JsonClass(generateAdapter = false)
data class OneCallResponse(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    @Json(name = "timezone_offset") val timezoneOffset: Long,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
)
