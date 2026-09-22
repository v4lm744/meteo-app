package com.meteoapp.data.api

import com.meteoapp.data.model.OpenMeteoAirQualityResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API qualité de l'air Open-Meteo : gratuite, sans clé, indice européen
 * (EEA) + concentrations PM2.5 / PM10 / ozone / NO2. Sert de source
 * principale pour la carte qualité de l'air, OpenWeatherMap restant le
 * repli si l'appel échoue.
 */
interface OpenMeteoAirQualityApi {

    @GET("v1/air-quality")
    suspend fun getCurrent(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoAirQualityResponse

    companion object {
        const val CURRENT_FIELDS =
            "european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide"
    }
}
