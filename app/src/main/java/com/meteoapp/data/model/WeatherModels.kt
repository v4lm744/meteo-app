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
data class Clouds(val all: Long?)

@JsonClass(generateAdapter = false)
data class Wind(
    val speed: Double,
    val deg: Long,
    val gust: Double?
)

@JsonClass(generateAdapter = false)
data class Sys(
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

@JsonClass(generateAdapter = false)
data class MainMetrics(
    val temp: Double,
    @Json(name = "feels_like") val feelsLike: Double,
    val tempMin: Double? = null,
    val tempMax: Double? = null,
    val pressure: Long,
    val humidity: Long
)

@JsonClass(generateAdapter = false)
data class Coord(
    val lon: Double,
    val lat: Double
)

@JsonClass(generateAdapter = false)
data class CityInfo(
    val id: Long? = null,
    val name: String,
    val coord: Coord? = null,
    val country: String? = null,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val timezone: Long? = null
)

@JsonClass(generateAdapter = false)
data class CurrentWeatherResponse(
    val coord: Coord? = null,
    val weather: List<WeatherCondition>,
    val base: String? = null,
    val main: MainMetrics,
    val visibility: Long? = null,
    val wind: Wind,
    val clouds: Clouds? = null,
    val dt: Long,
    val sys: Sys? = null,
    val timezone: Long? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = false)
data class ForecastItem(
    val dt: Long,
    val main: MainMetrics,
    val weather: List<WeatherCondition>,
    val clouds: Clouds? = null,
    val wind: Wind,
    val pop: Double? = null,
    @Json(name = "rain") val rain: RainVolume? = null,
    @Json(name = "snow") val snow: SnowVolume? = null,
    @Json(name = "dt_txt") val dtTxt: String? = null
)

@JsonClass(generateAdapter = false)
data class RainVolume(
    @Json(name = "1h") val oneHour: Double? = null,
    @Json(name = "3h") val threeHour: Double? = null
)

@JsonClass(generateAdapter = false)
data class SnowVolume(
    @Json(name = "1h") val oneHour: Double? = null,
    @Json(name = "3h") val threeHour: Double? = null
)

@JsonClass(generateAdapter = false)
data class ForecastResponse(
    val cod: String? = null,
    val message: Long? = null,
    val cnt: Long? = null,
    val list: List<ForecastItem>,
    val city: CityInfo
)

@JsonClass(generateAdapter = false)
data class FindCityItem(
    val id: Long,
    val name: String,
    val coord: Coord,
    val main: MainMetrics,
    val weather: List<WeatherCondition>,
    val sys: Sys? = null
)

@JsonClass(generateAdapter = false)
data class FindResponse(
    val cod: String? = null,
    val count: Long? = null,
    val list: List<FindCityItem> = emptyList()
)

@JsonClass(generateAdapter = false)
data class GeoLocation(
    val name: String,
    @Json(name = "local_names") val localNames: LocalNames? = null,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
)

@JsonClass(generateAdapter = false)
data class LocalNames(
    val fr: String? = null,
    val en: String? = null,
    @Json(name = "feature_name") val featureName: String? = null
)
