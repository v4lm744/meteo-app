package com.meteoapp.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Préférences des alertes météo (activées ou non + seuils pluie/vent/gel).
 * Seuils exprimés en mm cumulés / 12 h, km/h et °C.
 */
object AlertPrefs {

    private const val PREFS_NAME = "meteo_alert_prefs"
    private const val KEY_ENABLED = "alerts_enabled"
    private const val KEY_RAIN_MM = "alert_rain_mm"
    private const val KEY_WIND_KMH = "alert_wind_kmh"
    private const val KEY_FROST_TEMP = "alert_frost_temp"

    const val DEFAULT_RAIN_MM = 2.0
    const val DEFAULT_WIND_KMH = 60.0
    const val DEFAULT_FROST_TEMP = 0.0

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun getRainThresholdMm(context: Context): Double =
        prefs(context).getString(KEY_RAIN_MM, null)?.toDoubleOrNull()
            ?: DEFAULT_RAIN_MM

    fun setRainThresholdMm(context: Context, mm: Double) {
        prefs(context).edit { putString(KEY_RAIN_MM, mm.toString()) }
    }

    fun getWindThresholdKmh(context: Context): Double =
        prefs(context).getString(KEY_WIND_KMH, null)?.toDoubleOrNull()
            ?: DEFAULT_WIND_KMH

    fun setWindThresholdKmh(context: Context, kmh: Double) {
        prefs(context).edit { putString(KEY_WIND_KMH, kmh.toString()) }
    }

    fun getFrostThresholdCelsius(context: Context): Double =
        prefs(context).getString(KEY_FROST_TEMP, null)?.toDoubleOrNull()
            ?: DEFAULT_FROST_TEMP

    fun setFrostThresholdCelsius(context: Context, celsius: Double) {
        prefs(context).edit { putString(KEY_FROST_TEMP, celsius.toString()) }
    }
}
