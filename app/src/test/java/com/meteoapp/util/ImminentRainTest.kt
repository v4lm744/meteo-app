package com.meteoapp.util

import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Vérifie la détection de précipitation imminente : tri des créneaux,
 * détection pluie/neige par code condition et par volume, calcul des
 * minutes restantes et arrondi humain.
 */
class ImminentRainTest {

    private fun hour(
        dt: Long,
        conditionId: Long = 800L,
        pop: Double? = null,
        rain: Double = 0.0,
        snow: Double = 0.0
    ) = HourlyData(
        dt = dt,
        temp = 15.0,
        feelsLike = 14.0,
        humidity = 60L,
        windSpeed = 3.0,
        windDeg = 180L,
        weather = listOf(
            WeatherCondition(id = conditionId, main = "", description = "", icon = "10d")
        ),
        pop = pop,
        timezoneOffset = 0L,
        rainVolume = rain,
        snowVolume = snow
    )

    @Test
    fun detect_noPrecipitation_returnsNull() {
        val hours = listOf(
            hour(dt = 1000),
            hour(dt = 2000)
        )
        assertNull(ImminentRain.detect(hours, nowEpochSeconds = 0))
    }

    @Test
    fun detect_rainWithinHours_returnsMinutes() {
        val hours = listOf(
            hour(dt = 900),
            hour(dt = 4500, conditionId = 500L, pop = 0.8),
            hour(dt = 9000)
        )
        val result = ImminentRain.detect(hours, nowEpochSeconds = 1000)
        assertNotNull(result)
        assertEquals(58L, result!!.startsInMinutes)
        assertFalse(result.isSnow)
        assertEquals(80, result.probabilityPercent)
    }

    @Test
    fun detect_byVolumeOnly_findsRain() {
        val hours = listOf(
            hour(dt = 3000, rain = 0.5)
        )
        val result = ImminentRain.detect(hours, nowEpochSeconds = 1000)
        assertNotNull(result)
    }

    @Test
    fun detect_snowCondition_flagsSnow() {
        val hours = listOf(
            hour(dt = 3600, conditionId = 601L, pop = 0.6)
        )
        val result = ImminentRain.detect(hours, nowEpochSeconds = 0)
        assertNotNull(result)
        assertTrue(result!!.isSnow)
    }

    @Test
    fun detect_ignoresPastSlots() {
        val hours = listOf(
            hour(dt = 100, conditionId = 500L),
            hour(dt = 7200)
        )
        val result = ImminentRain.detect(hours, nowEpochSeconds = 1000)
        assertNull(result)
    }

    @Test
    fun roundToHumanMinutes_snapsToQuarterHour() {
        assertEquals(0L, ImminentRain.roundToHumanMinutes(0))
        assertEquals(15L, ImminentRain.roundToHumanMinutes(10))
        assertEquals(15L, ImminentRain.roundToHumanMinutes(15))
        assertEquals(45L, ImminentRain.roundToHumanMinutes(40))
        assertEquals(45L, ImminentRain.roundToHumanMinutes(52))
    }
}
