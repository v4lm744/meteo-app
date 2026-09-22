package com.meteoapp.data

import com.meteoapp.data.model.OpenMeteoAirQualityCurrent
import com.meteoapp.data.model.OpenMeteoAirQualityResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Conversion de l'indice européen EEA (0–100) vers l'échelle OWM (1–5) :
 * bornes officielles EEA et mapping des concentrations.
 */
class OpenMeteoAirQualityMapperTest {

    private fun aqiOf(response: OpenMeteoAirQualityResponse): Long =
        OpenMeteoAirQualityMapper.toAirPollutionResponse(response).list.first().main!!.aqi

    private fun response(aqi: Long?) = OpenMeteoAirQualityResponse(
        latitude = 48.85,
        longitude = 2.35,
        current = OpenMeteoAirQualityCurrent(
            time = "2026-09-22T14:00",
            europeanAqi = aqi,
            pm25 = 6.5,
            pm10 = 12.7,
            ozone = 51.0,
            nitrogenDioxide = 28.6
        )
    )

    @Test
    fun toAirPollutionResponse_mapsEuropeanAqiBoundaries() {
        assertEquals(1L, aqiOf(response(0)))
        assertEquals(1L, aqiOf(response(20)))
        assertEquals(2L, OpenMeteoAirQualityMapper.toAirPollutionResponse(response(40)).list.first().main!!.aqi)
        assertEquals(3L, OpenMeteoAirQualityMapper.toAirPollutionResponse(response(60)).list.first().main!!.aqi)
        assertEquals(4L, OpenMeteoAirQualityMapper.toAirPollutionResponse(response(80)).list.first().main!!.aqi)
        assertEquals(5L, OpenMeteoAirQualityMapper.toAirPollutionResponse(response(100)).list.first().main!!.aqi)
    }

    @Test
    fun toAirPollutionResponse_mapsComponents() {
        val item = OpenMeteoAirQualityMapper.toAirPollutionResponse(response(42)).list.first()
        assertEquals(3L, item.main!!.aqi)
        assertEquals(6.5, item.components!!.pm25, 0.001)
        assertEquals(12.7, item.components.pm10, 0.001)
        assertEquals(51.0, item.components.o3, 0.001)
        assertEquals(28.6, item.components.no2, 0.001)
    }

    @Test
    fun toAirPollutionResponse_nullAqi_defaultsToGood() {
        val item = OpenMeteoAirQualityMapper.toAirPollutionResponse(response(null)).list.first()
        assertEquals(1L, item.main!!.aqi)
        assertTrue(item.components!!.pm25 > 0.0)
    }
}
