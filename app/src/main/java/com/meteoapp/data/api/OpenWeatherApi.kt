package com.meteoapp.data.api

import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.OneCallResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {

    @GET("data/2.5/onecall")
    suspend fun getOneCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("exclude") exclude: String = "minutely,alerts",
        @Query("appid") apiKey: String
    ): OneCallResponse

    @GET("geo/1.0/direct")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<GeoLocation>

    @GET("geo/1.0/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoLocation>
}
