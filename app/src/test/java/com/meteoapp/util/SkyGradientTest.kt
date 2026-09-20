package com.meteoapp.util

import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.WeatherCondition
import com.meteoapp.data.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Vérifie le dégradé de ciel dynamique : les moments clés de la journée
 * (nuit, aube, lever du soleil, plein jour, crépuscule, retour nuit)
 * produisent les couleurs attendues, avec transitions continues.
 */
class SkyGradientTest {

    private fun weather(sunrise: Long? = 25200L, sunset: Long? = 68400L): WeatherData {
        val current = CurrentData(
            dt = 0L, sunrise = sunrise, sunset = sunset,
            temp = 20.0, feelsLike = 19.0, pressure = 1013L, humidity = 50L,
            visibility = 10000L, windSpeed = 2.0, windDeg = 0L,
            weather = listOf(WeatherCondition(800L, "Clear", "ciel dégagé", "01d")),
            timezoneOffset = 0L
        )
        return WeatherData(
            lat = 48.85, lon = 2.35, timezone = "UTC", timezoneOffset = 0L,
            current = current, hourly = emptyList(), daily = emptyList()
        )
    }

    private fun stopAt(localSeconds: Long, sunrise: Long? = 25200L, sunset: Long? = 68400L): SkyGradient.Stop =
        SkyGradient.stopFor(localSeconds, sunrise, sunset)

    @Test
    fun `nuit profonde avant l'aube`() {
        val night = stopAt(2 * 3600L)
        assertEquals(0xFF0B1D3A.toInt(), night.top)
    }

    @Test
    fun `plein jour entre lever et coucher`() {
        val day = stopAt(12 * 3600L)
        assertEquals(0xFF2E8BC0.toInt(), day.top)
    }

    @Test
    fun `aube interpole entre nuit et lever`() {
        val beforeDawn = stopAt(25200L - 3600L)
        assertEquals(0xFF0B1D3A.toInt(), beforeDawn.top)
        val atSunrise = stopAt(25200L + 3600L)
        assertEquals(0xFFE8A15C.toInt(), atSunrise.top)
    }

    @Test
    fun `crepuscule interpole vers la nuit`() {
        val atDuskStart = stopAt(68400L - 5400L)
        assertEquals(0xFF2E8BC0.toInt(), atDuskStart.top)
        val afterSunset = stopAt(68400L + 3600L)
        assertEquals(0xFF0B1D3A.toInt(), afterSunset.top)
    }

    @Test
    fun `sans lever coucher on utilise 7h 19h`() {
        val day = stopAt(12 * 3600L, sunrise = null, sunset = null)
        assertEquals(0xFF2E8BC0.toInt(), day.top)
        val night = stopAt(2 * 3600L, sunrise = null, sunset = null)
        assertEquals(0xFF0B1D3A.toInt(), night.top)
    }

    @Test
    fun `la pluie assombrit le ciel du jour`() {
        val rainy = weather().let { data ->
            data.copy(
                current = data.current.copy(
                    weather = listOf(WeatherCondition(501L, "Rain", "pluie", "10d"))
                )
            )
        }
        val stop = SkyGradient.stopFor(rainy, nowMillis = 12L * 3600_000L)
        org.junit.Assert.assertNotEquals(0xFF2E8BC0.toInt(), stop.top)
        org.junit.Assert.assertTrue(
            "la teinte pluvieuse doit être plus sombre que le jour clair",
            (stop.top shr 16 and 0xFF) < (0xFF2E8BC0.toInt() shr 16 and 0xFF)
        )
    }

    @Test
    fun `transitions continues sans saut de couleur`() {
        var previous: Int? = null
        for (minute in 0 until 1440) {
            val top = stopAt(minute * 60L).top
            previous?.let { prev ->
                val dr = Math.abs((top shr 16 and 0xFF) - (prev shr 16 and 0xFF))
                val dg = Math.abs((top shr 8 and 0xFF) - (prev shr 8 and 0xFF))
                val db = Math.abs((top and 0xFF) - (prev and 0xFF))
                org.junit.Assert.assertTrue(
                    "saut de couleur à $minute min : r=$dr g=$dg b=$db",
                    dr + dg + db < 60
                )
            }
            previous = top
        }
    }
}
