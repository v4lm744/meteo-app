package com.meteoapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WeatherUtils {

    private val dayNames = arrayOf(
        "dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."
    )

    fun formatHour(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val sdf = SimpleDateFormat("HH", Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return "${sdf.format(Date(millis))}h"
    }

    fun formatDayName(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        }
        return dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun isToday(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): Boolean {
        val target = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val now = System.currentTimeMillis() + timezoneOffsetSeconds * 1000L
        val cal1 = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = target
        }
        val cal2 = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = now
        }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun formatTime(timestampSeconds: Long, timezoneOffsetSeconds: Long): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val sdf = SimpleDateFormat("HH:mm", Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun roundToInt(value: Double): Int = Math.round(value).toInt()

    fun iconUrl(icon: String): String = "https://openweathermap.org/img/wn/$icon@2x.png"

    fun windDirection(deg: Long): String {
        val directions = arrayOf(
            "N", "NE", "E", "SE", "S", "SO", "O", "NO"
        )
        val index = ((deg.toDouble() + 22.5) / 45.0).toInt() % 8
        return directions[if (index < 0) index + 8 else index]
    }

    fun kmh(speed: Double): Int = Math.round(speed * 3.6).toInt()
}
