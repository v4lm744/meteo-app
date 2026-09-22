package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Long = 0L,
    val current: OpenMeteoCurrent,
    val hourly: OpenMeteoHourly,
    val daily: OpenMeteoDaily
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Long,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "is_day") val isDay: Long,
    val precipitation: Double,
    @Json(name = "weather_code") val weatherCode: Long,
    @Json(name = "cloud_cover") val cloudCover: Long,
    @Json(name = "pressure_msl") val pressureMsl: Double,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Long,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperature2m: List<Double>,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Long>,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Long?>,
    val precipitation: List<Double>,
    @Json(name = "weather_code") val weatherCode: List<Long>,
    @Json(name = "pressure_msl") val pressureMsl: List<Double>,
    @Json(name = "cloud_cover") val cloudCover: List<Long>,
    val visibility: List<Long?>,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double>,
    @Json(name = "wind_direction_10m") val windDirection10m: List<Long>,
    @Json(name = "uv_index") val uvIndex: List<Double>
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Long>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Long?>
)
