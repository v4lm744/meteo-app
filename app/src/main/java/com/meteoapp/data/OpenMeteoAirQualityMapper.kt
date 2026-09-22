package com.meteoapp.data

import com.meteoapp.data.model.AirPollutionComponents
import com.meteoapp.data.model.AirPollutionItem
import com.meteoapp.data.model.AirPollutionMain
import com.meteoapp.data.model.AirPollutionResponse
import com.meteoapp.data.model.OpenMeteoAirQualityResponse

/**
 * Conversion d'une réponse qualité de l'air Open-Meteo (indice européen
 * EEA, 0\u2013100) vers le modèle OpenWeatherMap (AQI 1\u20135) afin de conserver
 * la carte et les libellés existants sans double système visuel.
 *
 * Seuils EEA officiels : 0\u201320 bon, 20\u201340 correct, 40\u201360 moyen,
 * 60\u201380 mauvais, 80\u2013\u2265100 très mauvais.
 */
object OpenMeteoAirQualityMapper {

    fun toAirPollutionResponse(response: OpenMeteoAirQualityResponse): AirPollutionResponse {
        val current = response.current
        return AirPollutionResponse(
            list = listOf(
                AirPollutionItem(
                    main = AirPollutionMain(aqi = toOwmAqi(current.europeanAqi)),
                    components = AirPollutionComponents(
                        pm25 = current.pm25 ?: 0.0,
                        pm10 = current.pm10 ?: 0.0,
                        o3 = current.ozone ?: 0.0,
                        no2 = current.nitrogenDioxide ?: 0.0
                    )
                )
            )
        )
    }

    /** Indice européen 0\u2013100 \u2192 échelle OWM 1\u20135 (bornes EEA). */
    private fun toOwmAqi(europeanAqi: Long?): Long = when {
        europeanAqi == null -> 1L
        europeanAqi <= 20 -> 1L
        europeanAqi <= 40 -> 2L
        europeanAqi <= 60 -> 3L
        europeanAqi <= 80 -> 4L
        else -> 5L
    }
}
