package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoAirQualityCurrent
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityCurrent(
    val time: String,
    @Json(name = "european_aqi") val europeanAqi: Long?,
    @Json(name = "pm2_5") val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double?
)
