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
import com.meteoapp.util.WeatherColors
import androidx.core.graphics.createBitmap

/**
 * Génère le dégradé de fond du widget en fonction de la météo et de l'heure.
 * Gauche -> droite, adapté au jour/nuit et au type de temps.
 */
object WidgetGradient {

    fun buildBackground(context: Context, weather: WeatherData, width: Int, height: Int): Bitmap {
        val w = if (width <= 0) 1 else width
        val h = if (height <= 0) 1 else height

        val isDay = WeatherColors.isDaytime(weather)
        val code = weather.current.weather.firstOrNull()?.id ?: 800L
        val start = WeatherColors.topColor(weather)
        val end = widgetEndColor(code, isDay)

        val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                start, end,
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

    fun buildDarkBackground(context: Context, width: Int, height: Int): Bitmap {
        val w = if (width <= 0) 1 else width
        val h = if (height <= 0) 1 else height

        val bitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                0xFF1B1B1F.toInt(), 0xFF2D2D33.toInt(),
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

    private fun widgetEndColor(code: Long, isDay: Boolean): Int = when {
        code == 800L && isDay -> 0xFFFFC857.toInt() // soleil -> jaune
        code == 800L -> 0xFF13294B.toInt()         // nuit
        code in 801..804 && isDay -> 0xFF95A5A6.toInt() // nuages jour
        code in 801..804 -> 0xFF34495E.toInt()      // nuages nuit
        code in 600..622 -> 0xFFE3F2FD.toInt()      // neige clair
        code in 500..531 -> 0xFF607D8B.toInt()      // pluie
        code in 300..321 -> 0xFF78909C.toInt()      // bruine
        code in 200..232 -> 0xFF4B3F72.toInt()      // orage
        code in 700..781 -> 0xFFB0BEC5.toInt()      // brume
        else -> 0xFF3DA9FC.toInt()
    }
}
