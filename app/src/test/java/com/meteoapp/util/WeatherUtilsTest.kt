package com.meteoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUtilsTest {

    @Test
    fun roundToInt_roundsToNearest() {
        assertEquals(20, WeatherUtils.roundToInt(19.6))
        assertEquals(20, WeatherUtils.roundToInt(20.4))
        assertEquals(-3, WeatherUtils.roundToInt(-2.5))
        assertEquals(0, WeatherUtils.roundToInt(0.0))
    }

    @Test
    fun kmh_convertsMetersPerSecondToKmh() {
        assertEquals(36, WeatherUtils.kmh(10.0))
        assertEquals(0, WeatherUtils.kmh(0.0))
        assertEquals(4, WeatherUtils.kmh(1.0))
    }

    @Test
    fun windDirection_returnsCardinalForKnownAngles() {
        assertEquals("N", WeatherUtils.windDirection(0L))
        assertEquals("NE", WeatherUtils.windDirection(45L))
        assertEquals("E", WeatherUtils.windDirection(90L))
        assertEquals("SE", WeatherUtils.windDirection(135L))
        assertEquals("S", WeatherUtils.windDirection(180L))
        assertEquals("SO", WeatherUtils.windDirection(225L))
        assertEquals("O", WeatherUtils.windDirection(270L))
        assertEquals("NO", WeatherUtils.windDirection(315L))
    }

    @Test
    fun windDirection_handlesBoundaryAndWraparound() {
        // 22.5 -> 0 (N)
        assertEquals("N", WeatherUtils.windDirection(22L))
        // 337.5+ -> NO
        assertEquals("NO", WeatherUtils.windDirection(350L))
        assertEquals("N", WeatherUtils.windDirection(360L))
    }

    @Test
    fun iconUrl_buildsExpectedUrl() {
        assertEquals(
            "https://openweathermap.org/img/wn/01d@2x.png",
            WeatherUtils.iconUrl("01d")
        )
    }

    @Test
    fun formatHour_appendsHSuffix() {
        // 0 offset : 00:00 UTC -> "00h"
        assertEquals("00h", WeatherUtils.formatHour(0L, 0))
    }

    @Test
    fun formatDayName_returnsAbbreviatedFrenchDay() {
        // 1970-01-01 00:00:00 UTC est un jeudi -> "jeu."
        assertEquals("jeu.", WeatherUtils.formatDayName(0L, 0))
    }

    @Test
    fun formatFullDayName_returnsFullFrenchDay() {
        // 1970-01-01 00:00:00 UTC est un jeudi -> "Jeudi"
        assertEquals("Jeudi", WeatherUtils.formatFullDayName(0L, 0))
    }

    @Test
    fun isToday_trueForNowSameUtcDay() {
        val nowSeconds = System.currentTimeMillis() / 1000
        assertTrue(WeatherUtils.isToday(nowSeconds, 0))
    }

    @Test
    fun isToday_falseForFarPast() {
        // 1970-01-01 UTC n'est pas aujourd'hui
        assertFalse(WeatherUtils.isToday(0L, 0))
    }
}
