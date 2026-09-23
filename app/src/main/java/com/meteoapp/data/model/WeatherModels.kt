package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherCondition(
    val id: Long,
    val main: String,
    val description: String,
    val icon: String
)

@JsonClass(generateAdapter = true)
data class Clouds(val all: Long?)

@JsonClass(generateAdapter = true)
data class Wind(
    val speed: Double,
    val deg: Long,
    val gust: Double?
)

@JsonClass(generateAdapter = true)
data class Sys(
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

@JsonClass(generateAdapter = true)
data class MainMetrics(
    val temp: Double,
    @param:Json(name = "feels_like") val feelsLike: Double,
    val tempMin: Double? = null,
    val tempMax: Double? = null,
    val pressure: Long,
    val humidity: Long
)

@JsonClass(generateAdapter = true)
data class Coord(
    val lon: Double,
    val lat: Double
)

@JsonClass(generateAdapter = true)
data class CityInfo(
    val id: Long? = null,
    val name: String,
    val coord: Coord? = null,
    val country: String? = null,
    val sunrise: Long? = null,
    val sunset: Long? = null,
    val timezone: Long? = null
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class ForecastItem(
    val dt: Long,
    val main: MainMetrics,
    val weather: List<WeatherCondition>,
    val clouds: Clouds? = null,
    val wind: Wind,
    val pop: Double? = null,
    @param:Json(name = "rain") val rain: RainVolume? = null,
    @param:Json(name = "snow") val snow: SnowVolume? = null,
    @param:Json(name = "dt_txt") val dtTxt: String? = null
)

@JsonClass(generateAdapter = true)
data class RainVolume(
    @param:Json(name = "1h") val oneHour: Double? = null,
    @param:Json(name = "3h") val threeHour: Double? = null
)

@JsonClass(generateAdapter = true)
data class SnowVolume(
    @param:Json(name = "1h") val oneHour: Double? = null,
    @param:Json(name = "3h") val threeHour: Double? = null
)

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val cod: String? = null,
    val message: Long? = null,
    val cnt: Long? = null,
    val list: List<ForecastItem>,
    val city: CityInfo
)

@JsonClass(generateAdapter = true)
data class FindCityItem(
    val id: Long,
    val name: String,
    val coord: Coord,
    val main: MainMetrics,
    val weather: List<WeatherCondition>,
    val sys: Sys? = null
)

@JsonClass(generateAdapter = true)
data class FindResponse(
    val cod: String? = null,
    val count: Long? = null,
    val list: List<FindCityItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeoLocation(
    val name: String,
    @param:Json(name = "local_names") val localNames: LocalNames? = null,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
) {
    /**
     * Nom d'affichage de la ville dans la langue de l'interface : nom local
     * correspondant à la locale si l'API l'a fourni, sinon nom international.
     */
    fun displayName(context: android.content.Context): String {
        val lang = context.resources.configuration.locales[0].language
        return when (lang) {
            "fr" -> localNames?.fr ?: name
            "en" -> localNames?.en ?: name
            else -> localNames?.featureName ?: name
        }
    }
}

@JsonClass(generateAdapter = true)
data class LocalNames(
    val fr: String? = null,
    val en: String? = null,
    @param:Json(name = "feature_name") val featureName: String? = null
)
