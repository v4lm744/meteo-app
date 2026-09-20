package com.meteoapp.util

import com.meteoapp.data.model.WeatherData

/**
 * Dégradé de ciel dynamique selon l'heure réelle de la ville affichée :
 * nuit profonde, aube orangée, plein jour bleu puis crépuscule violet,
 * avec interpolation continue entre les étapes. Les couleurs restent
 * teintées par la météo (pluie, neige, orage...) pour garder la cohérence
 * visuelle du tableau de bord.
 */
object SkyGradient {

    data class Stop(val top: Int, val bottom: Int)

    private val NIGHT = Stop(0xFF0B1D3A.toInt(), 0xFF15263F.toInt())
    private val DAWN = Stop(0xFF8A5A44.toInt(), 0xFF3E4E66.toInt())
    private val SUNRISE = Stop(0xFFE8A15C.toInt(), 0xFF6D7FA0.toInt())
    private val DAY = Stop(0xFF2E8BC0.toInt(), 0xFF1B3A5B.toInt())
    private val DUSK = Stop(0xFF4A3B6E.toInt(), 0xFF2C3654.toInt())

    /**
     * Dégradé pour l'heure donnée (secondes locales = utc + fuseau).
     * [sunriseLocal] et [sunsetLocal] en secondes locales du même repère ;
     * si absents, on retombe sur des créneaux fixes 7h/19h.
     */
    fun stopFor(
        localSeconds: Long,
        sunriseLocal: Long?,
        sunsetLocal: Long?
    ): Stop {
        val dayStart = sunriseLocal ?: (7 * 3600L)
        val dayEnd = sunsetLocal ?: (19 * 3600L)

        val secOfDay = localSeconds.mod(86400L)
        val start = dayStart.mod(86400L)
        val end = dayEnd.mod(86400L)

        return when {
            secOfDay < start - 3600L -> NIGHT
            secOfDay < start -> blend(NIGHT, DAWN, (secOfDay - (start - 3600L)) / 3600f)
            secOfDay < start + 3600L -> blend(DAWN, SUNRISE, (secOfDay - start) / 3600f)
            secOfDay < start + 5400L -> blend(SUNRISE, DAY, (secOfDay - (start + 3600L)) / 1800f)
            secOfDay < end - 5400L -> DAY
            secOfDay < end -> blend(DAY, DUSK, (secOfDay - (end - 5400L)) / 5400f)
            secOfDay < end + 3600L -> blend(DUSK, NIGHT, (secOfDay - end) / 3600f)
            else -> NIGHT
        }
    }

    /**
     * Dégradé tenant compte de la météo : par temps couvert/pluvieux on
     * assombrit les teintes du jour pour rester lisible et réaliste.
     */
    fun stopFor(weather: WeatherData, nowMillis: Long = System.currentTimeMillis()): Stop {
        val localNow = (nowMillis / 1000L + weather.timezoneOffset)
        val sunriseLocal = weather.current.sunrise?.let { it + weather.timezoneOffset }
        val sunsetLocal = weather.current.sunset?.let { it + weather.timezoneOffset }
        val stop = stopFor(
            localNow,
            sunriseLocal,
            sunsetLocal
        )
        val code = weather.current.weather.firstOrNull()?.id ?: 800L
        return when {
            code in 200..232 -> blend(stop, Stop(0xFF1A2233.toInt(), 0xFF232E42.toInt()), 0.55f)
            code in 300..321 -> blend(stop, Stop(0xFF3A4A5B.toInt(), 0xFF2E3C4C.toInt()), 0.45f)
            code in 500..531 -> blend(stop, Stop(0xFF2A3A50.toInt(), 0xFF25344A.toInt()), 0.5f)
            code in 600..622 -> blend(stop, Stop(0xFF5E7A94.toInt(), 0xFF4E6880.toInt()), 0.4f)
            code in 700..781 -> blend(stop, Stop(0xFF6E7E8C.toInt(), 0xFF5E6E7C.toInt()), 0.4f)
            else -> stop
        }
    }

    private fun blend(from: Stop, to: Stop, fraction: Float): Stop {
        val f = fraction.coerceIn(0f, 1f)
        return Stop(
            top = blendColor(from.top, to.top, f),
            bottom = blendColor(from.bottom, to.bottom, f)
        )
    }

    private fun blendColor(from: Int, to: Int, fraction: Float): Int {
        val inv = 1f - fraction
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return ((a * inv) + (b * fraction)).toInt().coerceIn(0, 255)
        }
        return 0xFF shl 24 or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
