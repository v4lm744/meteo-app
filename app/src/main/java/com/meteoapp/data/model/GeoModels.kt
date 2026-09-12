package com.meteoapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class GeoLocation(
    val name: String,
    @Json(name = "local_names") val localNames: LocalNames?,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)

@JsonClass(generateAdapter = false)
data class LocalNames(
    val fr: String?,
    val en: String?,
    @Json(name = "feature_name") val featureName: String?
)
