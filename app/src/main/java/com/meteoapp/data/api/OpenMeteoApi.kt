package com.meteoapp.data.api

import com.meteoapp.data.model.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                "precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m," +
                "wind_direction_10m,wind_gusts_10m"
        const val HOURLY_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature," +
                "precipitation_probability,precipitation,weather_code,pressure_msl," +
                "cloud_cover,visibility,wind_speed_10m,wind_direction_10m,uv_index"
        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
                "uv_index_max,precipitation_probability_max"
    }
}
