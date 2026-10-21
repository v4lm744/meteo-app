package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @param:Json(name = "utc_offset_seconds") val utcOffsetSeconds: Long = 0L,
    val current: OpenMeteoCurrent,
    val hourly: OpenMeteoHourly,
    val daily: OpenMeteoDaily,
    @param:Json(name = "minutely_15") val minutely15: OpenMeteoMinutely15? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    val time: String,
    @param:Json(name = "temperature_2m") val temperature2m: Double,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: Long,
    @param:Json(name = "apparent_temperature") val apparentTemperature: Double,
    @param:Json(name = "is_day") val isDay: Long,
    val precipitation: Double,
    @param:Json(name = "weather_code") val weatherCode: Long,
    @param:Json(name = "cloud_cover") val cloudCover: Long,
    @param:Json(name = "pressure_msl") val pressureMsl: Double,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @param:Json(name = "wind_direction_10m") val windDirection10m: Long,
    @param:Json(name = "wind_gusts_10m") val windGusts10m: Double?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    val time: List<String>,
    @param:Json(name = "temperature_2m") val temperature2m: List<Double>,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Long>,
    @param:Json(name = "apparent_temperature") val apparentTemperature: List<Double>,
    @param:Json(name = "precipitation_probability") val precipitationProbability: List<Long?>,
    val precipitation: List<Double>,
    @param:Json(name = "weather_code") val weatherCode: List<Long>,
    @param:Json(name = "pressure_msl") val pressureMsl: List<Double>,
    @param:Json(name = "cloud_cover") val cloudCover: List<Long>,
    val visibility: List<Long?>,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: List<Double>,
    @param:Json(name = "wind_direction_10m") val windDirection10m: List<Long>,
    @param:Json(name = "uv_index") val uvIndex: List<Double>
)

@JsonClass(generateAdapter = true)
data class OpenMeteoMinutely15(
    val time: List<String>,
    val precipitation: List<Double>,
    @param:Json(name = "precipitation_probability") val precipitationProbability: List<Long?>,
    @param:Json(name = "weather_code") val weatherCode: List<Long>
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    val time: List<String>,
    @param:Json(name = "weather_code") val weatherCode: List<Long>,
    @param:Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @param:Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @param:Json(name = "uv_index_max") val uvIndexMax: List<Double>,
    @param:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Long?>
)
