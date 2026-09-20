package com.meteoapp.util

import android.content.Context
import com.meteoapp.R
import com.meteoapp.util.UnitPrefs.PressureUnit
import com.meteoapp.util.UnitPrefs.TempUnit
import com.meteoapp.util.UnitPrefs.TimeFormat
import com.meteoapp.util.UnitPrefs.WindUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object WeatherUtils {

    private val dayNames = arrayOf(
        "dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."
    )

    fun formatHour(context: Context, timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val pattern = when (UnitPrefs.getTimeFormat(context)) {
            TimeFormat.FORMAT_24H -> "HH"
            TimeFormat.FORMAT_12H -> "h a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return if (UnitPrefs.getTimeFormat(context) == TimeFormat.FORMAT_24H) {
            "${sdf.format(Date(millis))}h"
        } else {
            sdf.format(Date(millis))
        }
    }

    fun formatDayName(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        }
        return dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun formatFullDayName(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val names = arrayOf(
            "Dimanche", "Lundi", "Mardi", "Mercredi",
            "Jeudi", "Vendredi", "Samedi"
        )
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        }
        return names[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun formatDate(timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val sdf = SimpleDateFormat("d MMM", Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
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

    fun formatTime(context: Context, timestampSeconds: Long, timezoneOffsetSeconds: Long): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val pattern = when (UnitPrefs.getTimeFormat(context)) {
            TimeFormat.FORMAT_24H -> "HH:mm"
            TimeFormat.FORMAT_12H -> "h:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun formatFullDateTime(context: Context, timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val millis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        val pattern = when (UnitPrefs.getTimeFormat(context)) {
            TimeFormat.FORMAT_24H -> "EEEE HH:mm"
            TimeFormat.FORMAT_12H -> "EEEE h:mm a"
        }
        val sdf = SimpleDateFormat(pattern, Locale.FRANCE)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun formatFullDayNameTime(context: Context, timestampSeconds: Long, timezoneOffsetSeconds: Long = 0): String {
        val dayName = formatFullDayName(timestampSeconds, timezoneOffsetSeconds)
        val hour = formatTime(context, timestampSeconds, timezoneOffsetSeconds)
        return "$dayName $hour"
    }

    fun roundToInt(value: Double): Int = Math.round(value).toInt()

    /**
     * Formate une valeur numérique selon la précision choisie dans les
     * Paramètres : entier (« 17 ») ou une décimale (« 16,8 ») avec le
     * séparateur décimal de la locale.
     */
    fun formatValue(context: Context, value: Double): String =
        when (UnitPrefs.getValuePrecision(context)) {
            UnitPrefs.ValuePrecision.WHOLE ->
                Math.round(value).toString()
            UnitPrefs.ValuePrecision.ONE_DECIMAL ->
                String.format(
                    java.util.Locale.getDefault(),
                    "%.1f",
                    value
                )
        }

    /**
     * Convertit et formate une température (toujours fournie en °C par l'API)
     * selon l'unité choisie dans les Paramètres.
     */
    fun formatTemp(context: Context, celsius: Double): String {
        val value = when (UnitPrefs.getTempUnit(context)) {
            TempUnit.CELSIUS -> formatValue(context, celsius)
            TempUnit.FAHRENHEIT -> formatValue(context, celsius * 9.0 / 5.0 + 32.0)
        }
        return "$value°"
    }

    /**
     * Libellé de la bannière hors ligne : âge des données du cache pour la
     * ville affichée (« à l'instant », « il y a 12 minutes », « il y a 3 heures »).
     */
    fun formatOfflineAge(context: Context, lat: Double, lon: Double): String {
        val cache = com.meteoapp.data.WeatherCache(context)
        val age = cache.ageMillis(lat, lon)
        if (age == Long.MAX_VALUE || age < 0) {
            return context.getString(R.string.cache_offline_banner)
        }
        val ageLabel = when {
            age < 60_000L -> context.getString(R.string.offline_age_now)
            age < 3_600_000L -> {
                val minutes = (age / 60_000L).toInt()
                context.resources.getQuantityString(R.plurals.offline_age_minutes, minutes, minutes)
            }
            else -> {
                val hours = (age / 3_600_000L).toInt()
                context.resources.getQuantityString(R.plurals.offline_age_hours, hours, hours)
            }
        }
        return context.getString(R.string.cache_offline_with_age, ageLabel)
    }

    /** Formate une vitesse du vent selon l'unité choisie dans les Paramètres. */
    fun formatWindSpeed(context: Context, speedMs: Double): String =
        when (UnitPrefs.getWindUnit(context)) {
            WindUnit.KMH -> context.getString(
                R.string.kmh_format,
                formatValue(context, speedMs * 3.6)
            )
            WindUnit.MPH -> context.getString(
                R.string.mph_format,
                formatValue(context, speedMs * 2.23694)
            )
        }

    /** Formate une pression (fournée en hPa par l'API) selon l'unité choisie. */
    fun formatPressure(context: Context, hPa: Long): String =
        when (UnitPrefs.getPressureUnit(context)) {
            PressureUnit.HPA -> context.getString(R.string.hpa, hPa)
            PressureUnit.INHG -> context.getString(
                R.string.inhg,
                String.format(Locale.US, "%.2f", hPa / 33.8639)
            )
        }

    /** Formate une visibilité (fournée en mètres par l'API) selon l'unité de vent. */
    fun formatVisibility(context: Context, visibilityMeters: Long?): String {
        if (visibilityMeters == null) {
            return context.getString(R.string.visibility_unavailable)
        }
        return when (UnitPrefs.getWindUnit(context)) {
            WindUnit.KMH -> context.getString(R.string.km, (visibilityMeters / 1000).toInt())
            WindUnit.MPH -> context.getString(R.string.miles, kmToMiles(visibilityMeters / 1000.0).toInt())
        }
    }

    fun iconUrl(icon: String): String = "https://openweathermap.org/img/wn/$icon@2x.png"

    fun windDirection(deg: Long): String {
        val directions = arrayOf(
            "N", "NE", "E", "SE", "S", "SO", "O", "NO"
        )
        val index = ((deg.toDouble() + 22.5) / 45.0).toInt() % 8
        return directions[if (index < 0) index + 8 else index]
    }

    fun kmh(speed: Double): Int = Math.round(speed * 3.6).toInt()

    fun mph(speed: Double): Int = Math.round(speed * 2.23694).toInt()

    fun kmToMiles(km: Double): Double = km * 0.621371
}
