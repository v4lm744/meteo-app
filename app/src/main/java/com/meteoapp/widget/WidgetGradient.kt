package com.meteoapp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.meteoapp.data.model.WeatherData
import com.meteoapp.util.WeatherUtils

/**
 * Génère le dégradé de fond du widget en fonction de la météo et de l'heure.
 * Gauche -> droite, adapté au jour/nuit et au type de temps.
 */
object WidgetGradient {

    private data class GradientColors(val start: Int, val end: Int)

    fun buildBackground(context: Context, weather: WeatherData, width: Int, height: Int): Bitmap {
        val w = if (width <= 0) 1 else width
        val h = if (height <= 0) 1 else height
        val colors = colorsFor(weather)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                colors.start, colors.end,
                Shader.TileMode.CLAMP
            )
        }
        val radius = 0.08f * minOf(w, h)
        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(path)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return bitmap
    }

    private fun colorsFor(weather: WeatherData): GradientColors {
        val current = weather.current
        val isDay = isDaytime(weather)
        val code = current.weather.firstOrNull()?.id ?: 800L

        // Groupes OpenWeather (par plages d'ids)
        // 2xx orage, 3x bruine, 5x pluie, 6x neige, 7x atmosphère, 800 dégagé, 80x nuages
        val group = when {
            code in 200..232 -> WeatherGroup.THUNDERSTORM
            code in 300..321 -> WeatherGroup.DRIZZLE
            code in 500..531 -> WeatherGroup.RAIN
            code in 600..622 -> WeatherGroup.SNOW
            code in 700..781 -> WeatherGroup.ATMOSPHERE
            code == 800L -> WeatherGroup.CLEAR
            code in 801..804 -> WeatherGroup.CLOUDS
            else -> WeatherGroup.CLEAR
        }

        return when (group) {
            WeatherGroup.CLEAR -> if (isDay) {
                GradientColors(0xFF2E8BC0.toInt(), 0xFFFFC857.toInt()) // bleu -> soleil
            } else {
                GradientColors(0xFF0B1D3A.toInt(), 0xFF13294B.toInt()) // nuit foncée
            }
            WeatherGroup.CLOUDS -> if (isDay) {
                GradientColors(0xFF6C7A89.toInt(), 0xFF95A5A6.toInt()) // gris bleu
            } else {
                GradientColors(0xFF2C3E50.toInt(), 0xFF34495E.toInt())
            }
            WeatherGroup.RAIN -> GradientColors(0xFF3A4A5B.toInt(), 0xFF607D8B.toInt()) // gris pluie
            WeatherGroup.DRIZZLE -> GradientColors(0xFF4A6274.toInt(), 0xFF78909C.toInt())
            WeatherGroup.THUNDERSTORM -> GradientColors(0xFF222B45.toInt(), 0xFF4B3F72.toInt()) // sombre violacé
            WeatherGroup.SNOW -> GradientColors(0xFFA8C5D6.toInt(), 0xFFE3F2FD.toInt()) // gris-bleu clair
            WeatherGroup.ATMOSPHERE -> GradientColors(0xFF8E9EAB.toInt(), 0xFFB0BEC5.toInt()) // brume
        }
    }

    private fun isDaytime(weather: WeatherData): Boolean {
        val now = weather.current.dt + weather.timezoneOffset
        val sunrise = weather.current.sunrise?.plus(weather.timezoneOffset)
        val sunset = weather.current.sunset?.plus(weather.timezoneOffset)
        if (sunrise != null && sunset != null) {
            return now >= sunrise && now < sunset
        }
        // Fallback : heure locale 7h-19h
        val hour = WeatherUtils.formatHour(weather.current.dt, weather.timezoneOffset)
        val h = hour.removeSuffix("h").toIntOrNull() ?: 12
        return h in 7..19
    }

    private enum class WeatherGroup {
        CLEAR, CLOUDS, RAIN, DRIZZLE, THUNDERSTORM, SNOW, ATMOSPHERE
    }
}
