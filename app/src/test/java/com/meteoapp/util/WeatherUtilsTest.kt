package com.meteoapp.util

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeatherUtilsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()


    @Test
    fun roundToInt_roundsToNearest() {
        assertEquals(20, WeatherUtils.roundToInt(19.6))
        assertEquals(20, WeatherUtils.roundToInt(20.4))
        assertEquals(-2, WeatherUtils.roundToInt(-2.5))
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
        // 350° est dans le secteur N (337.5° à 22.5°)
        assertEquals("N", WeatherUtils.windDirection(350L))
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
        // 0 offset : 00:00 UTC -> "00h" (format 24 h par défaut)
        assertEquals("00h", WeatherUtils.formatHour(context, 0L, 0))
    }

    @Test
    fun formatHour_12hFormatUsesAmPm() {
        UnitPrefs.setTimeFormat(context, UnitPrefs.TimeFormat.FORMAT_12H)
        // 13:00 UTC -> "1 PM"
        assertEquals(
            "1 PM",
            WeatherUtils.formatHour(context, 13L * 3600, 0)
        )
        UnitPrefs.setTimeFormat(context, UnitPrefs.TimeFormat.FORMAT_24H)
    }

    @Test
    fun formatTemp_fahrenheitConvertsCelsius() {
        UnitPrefs.setTempUnit(context, UnitPrefs.TempUnit.FAHRENHEIT)
        assertEquals("68°", WeatherUtils.formatTemp(context, 20.0))
        UnitPrefs.setTempUnit(context, UnitPrefs.TempUnit.CELSIUS)
        assertEquals("20°", WeatherUtils.formatTemp(context, 20.0))
    }

    @Test
    fun formatWindSpeed_mphConvertsMetersPerSecond() {
        UnitPrefs.setWindUnit(context, UnitPrefs.WindUnit.MPH)
        assertEquals("22 mph", WeatherUtils.formatWindSpeed(context, 10.0))
        UnitPrefs.setWindUnit(context, UnitPrefs.WindUnit.KMH)
        assertEquals("36 km/h", WeatherUtils.formatWindSpeed(context, 10.0))
    }

    @Test
    fun formatTemp_oneDecimalRoundsToTenth() {
        val original = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.FRANCE)
        try {
            UnitPrefs.setValuePrecision(context, UnitPrefs.ValuePrecision.ONE_DECIMAL)
            UnitPrefs.setTempUnit(context, UnitPrefs.TempUnit.CELSIUS)
            assertEquals("16,8°", WeatherUtils.formatTemp(context, 16.76))
            UnitPrefs.setTempUnit(context, UnitPrefs.TempUnit.FAHRENHEIT)
            assertEquals("62,2°", WeatherUtils.formatTemp(context, 16.78))
            UnitPrefs.setValuePrecision(context, UnitPrefs.ValuePrecision.WHOLE)
            UnitPrefs.setTempUnit(context, UnitPrefs.TempUnit.CELSIUS)
            assertEquals("17°", WeatherUtils.formatTemp(context, 16.76))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    @Test
    fun formatWindSpeed_oneDecimal() {
        val original = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.FRANCE)
        try {
            UnitPrefs.setValuePrecision(context, UnitPrefs.ValuePrecision.ONE_DECIMAL)
            UnitPrefs.setWindUnit(context, UnitPrefs.WindUnit.KMH)
            assertEquals("35,9 km/h", WeatherUtils.formatWindSpeed(context, 9.97))
            UnitPrefs.setValuePrecision(context, UnitPrefs.ValuePrecision.WHOLE)
            assertEquals("36 km/h", WeatherUtils.formatWindSpeed(context, 9.97))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    @Test
    fun formatPressure_inhgConvertsHpa() {
        UnitPrefs.setPressureUnit(context, UnitPrefs.PressureUnit.INHG)
        assertEquals("29.53 inHg", WeatherUtils.formatPressure(context, 1000L))
        UnitPrefs.setPressureUnit(context, UnitPrefs.PressureUnit.HPA)
        assertEquals("1000 hPa", WeatherUtils.formatPressure(context, 1000L))
    }

    @Test
    fun formatVisibility_milesWhenMphSelected() {
        UnitPrefs.setWindUnit(context, UnitPrefs.WindUnit.MPH)
        assertEquals("6 mi", WeatherUtils.formatVisibility(context, 10000L))
        UnitPrefs.setWindUnit(context, UnitPrefs.WindUnit.KMH)
        assertEquals("10 km", WeatherUtils.formatVisibility(context, 10000L))
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
