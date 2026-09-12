package com.meteoapp.data.api

import com.meteoapp.data.model.CurrentWeatherResponse
import com.meteoapp.data.model.FindResponse
import com.meteoapp.data.model.ForecastResponse
import com.meteoapp.data.model.GeoLocation
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("appid") apiKey: String
    ): CurrentWeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("appid") apiKey: String
    ): ForecastResponse

    @GET("data/2.5/find")
    suspend fun findCities(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("cnt") count: Int = 10,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "fr",
        @Query("appid") apiKey: String
    ): FindResponse

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
