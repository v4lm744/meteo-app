package com.meteoapp.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Préférences des notifications météo : activation, heure de remise
 * quotidienne. Stockées dans les SharedPreferences de l'application.
 */
object NotificationPrefs {

    private const val PREFS_NAME = "meteo_notification_prefs"
    private const val KEY_ENABLED = "weather_notification_enabled"
    private const val KEY_HOUR = "weather_notification_hour"

    const val DEFAULT_HOUR = 8

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun getHour(context: Context): Int =
        prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR)

    fun setHour(context: Context, hour: Int) {
        prefs(context).edit { putInt(KEY_HOUR, hour.coerceIn(0, 23)) }
    }
}
