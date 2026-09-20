package com.meteoapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentData(
    val dt: Long,
    val sunrise: Long?,
    val sunset: Long?,
    val temp: Double,
    val feelsLike: Double,
    val pressure: Long,
    val humidity: Long,
    val visibility: Long?,
    val windSpeed: Double,
    val windDeg: Long,
    val weather: List<WeatherCondition>,
    val cloudiness: Long = 0L,
    val timezoneOffset: Long
)

@JsonClass(generateAdapter = true)
data class HourlyData(
    val dt: Long,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Long,
    val windSpeed: Double,
    val windDeg: Long,
    val weather: List<WeatherCondition>,
    val pop: Double?,
    val timezoneOffset: Long,
    val pressure: Long = 0L,
    val cloudiness: Long = 0L,
    val visibility: Long? = null,
    val rainVolume: Double = 0.0,
    val snowVolume: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DailyData(
    val dt: Long,
    val sunrise: Long?,
    val sunset: Long?,
    val tempMin: Double,
    val tempMax: Double,
    val morningTemp: Double,
    val dayTemp: Double,
    val eveningTemp: Double,
    val nightTemp: Double,
    val feelsLikeDay: Double,
    val pressure: Long,
    val humidity: Long,
    val windSpeed: Double,
    val windDeg: Long,
    val weather: List<WeatherCondition>,
    val pop: Double?,
    val timezoneOffset: Long
)

@JsonClass(generateAdapter = true)
data class WeatherData(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val timezoneOffset: Long,
    val current: CurrentData,
    val hourly: List<HourlyData>,
    val daily: List<DailyData>
)

@JsonClass(generateAdapter = true)
data class RegionCity(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    val temp: Double,
    val weatherIcon: String,
    val weatherDescription: String
)
