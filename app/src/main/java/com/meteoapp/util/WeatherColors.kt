package com.meteoapp.util

import com.meteoapp.data.model.WeatherData

/**
 * Couleurs dynamiques dérivées de la météo et de l'heure (jour/nuit).
 * Utilisées pour le dégradé de l'en-tête de l'app et du widget.
 */
object WeatherColors {

    data class Gradient(val top: Int, val bottom: Int)

    fun isDaytime(weather: WeatherData): Boolean {
        val now = weather.current.dt + weather.timezoneOffset
        val sunrise = weather.current.sunrise?.plus(weather.timezoneOffset)
        val sunset = weather.current.sunset?.plus(weather.timezoneOffset)
        if (sunrise != null && sunset != null) {
            return now >= sunrise && now < sunset
        }
        val hour = WeatherUtils.formatHour(weather.current.dt, weather.timezoneOffset)
            .removeSuffix("h").toIntOrNull() ?: 12
        return hour in 7..19
    }

    /**
     * Couleur "haut" (partie supérieure) selon la météo et l'heure.
     */
    fun topColor(weather: WeatherData): Int {
        val isDay = isDaytime(weather)
        val code = weather.current.weather.firstOrNull()?.id ?: 800L
        return when {
            code in 200..232 -> 0xFF222B45.toInt()      // orage
            code in 300..321 -> 0xFF4A6274.toInt()     // bruine
            code in 500..531 -> 0xFF3A4A5B.toInt()     // pluie
            code in 600..622 -> 0xFF7E9CB3.toInt()      // neige
            code in 700..781 -> 0xFF8E9EAB.toInt()      // brume
            code == 800L && isDay -> 0xFF2E8BC0.toInt() // soleil jour
            code == 800L -> 0xFF0B1D3A.toInt()          // nuit
            code in 801..804 && isDay -> 0xFF6C7A89.toInt() // nuages jour
            code in 801..804 -> 0xFF2C3E50.toInt()      // nuages nuit
            else -> 0xFF2364AA.toInt()
        }
    }

    /**
     * Couleur "bas" (vers laquelle le haut se fond en descendant).
     * Ici le bleu profond de l'app.
     */
    val appBaseColor: Int = 0xFF1B3A5B.toInt()

    fun gradient(weather: WeatherData): Gradient = Gradient(
        top = topColor(weather),
        bottom = appBaseColor
    )

    /**
     * Couleur "haut" pour un jour de prévision (DailyData), basée sur la météo
     * et l'heure (lever/coucher du soleil de ce jour). Utilisée par l'écran de
     * détail d'un jour, qui n'a pas de WeatherData complet.
     */
    fun topColor(day: com.meteoapp.data.model.DailyData): Int {
        val isDay = run {
            val now = day.dt + day.timezoneOffset
            val sunrise = day.sunrise?.plus(day.timezoneOffset)
            val sunset = day.sunset?.plus(day.timezoneOffset)
            if (sunrise != null && sunset != null) {
                now >= sunrise && now < sunset
            } else {
                val hour = WeatherUtils.formatHour(day.dt, day.timezoneOffset)
                    .removeSuffix("h").toIntOrNull() ?: 12
                hour in 7..19
            }
        }
        val code = day.weather.firstOrNull()?.id ?: 800L
        return colorForCode(code, isDay)
    }

    fun gradient(day: com.meteoapp.data.model.DailyData): Gradient = Gradient(
        top = topColor(day),
        bottom = appBaseColor
    )

    /**
     * Couleur "haut" pour une heure de pr\u00e9vision (HourlyData), bas\u00e9e sur la
     * m\u00e9t\u00e9o et l'heure (jour/nuit selon l'heure locale de la pr\u00e9vision).
     */
    fun topColor(hour: com.meteoapp.data.model.HourlyData): Int {
        val hourOfDay = WeatherUtils.formatHour(hour.dt, hour.timezoneOffset)
            .removeSuffix("h").toIntOrNull() ?: 12
        val isDay = hourOfDay in 7..19
        val code = hour.weather.firstOrNull()?.id ?: 800L
        return colorForCode(code, isDay)
    }

    fun gradient(hour: com.meteoapp.data.model.HourlyData): Gradient = Gradient(
        top = topColor(hour),
        bottom = appBaseColor
    )

    /**
     * Couleur de contraste (blanc ou noir) selon la luminance de [bgColor],
     * afin que les icônes/contrôles restent lisibles sur n'importe quel fond.
     */
    fun contrastColor(bgColor: Int): Int {
        val r = (bgColor shr 16) and 0xFF
        val g = (bgColor shr 8) and 0xFF
        val b = bgColor and 0xFF
        // luminance perçue (rec. 709)
        val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
        return if (luminance > 0.6) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
    }

    private fun colorForCode(code: Long, isDay: Boolean): Int {
        return when {
            code in 200..232 -> 0xFF222B45.toInt()      // orage
            code in 300..321 -> 0xFF4A6274.toInt()     // bruine
            code in 500..531 -> 0xFF3A4A5B.toInt()     // pluie
            code in 600..622 -> 0xFF7E9CB3.toInt()      // neige
            code in 700..781 -> 0xFF8E9EAB.toInt()      // brume
            code == 800L && isDay -> 0xFF2E8BC0.toInt() // soleil jour
            code == 800L -> 0xFF0B1D3A.toInt()          // nuit
            code in 801..804 && isDay -> 0xFF6C7A89.toInt() // nuages jour
            code in 801..804 -> 0xFF2C3E50.toInt()      // nuages nuit
            else -> 0xFF2364AA.toInt()
        }
    }
}
