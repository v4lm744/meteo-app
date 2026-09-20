package com.meteoapp.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirPollutionResponse(
    val list: List<AirPollutionItem>
)

@JsonClass(generateAdapter = true)
data class AirPollutionItem(
    val main: AirPollutionMain?,
    val components: AirPollutionComponents?
)

@JsonClass(generateAdapter = true)
data class AirPollutionMain(
    val aqi: Long
)

@JsonClass(generateAdapter = true)
data class AirPollutionComponents(
    val pm25: Double = 0.0,
    val pm10: Double = 0.0,
    val o3: Double = 0.0,
    val no2: Double = 0.0
)
