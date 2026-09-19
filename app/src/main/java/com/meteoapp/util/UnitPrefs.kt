package com.meteoapp.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Unités d'affichage choisies dans le menu « Paramètres ».
 *
 * Les choix sont persistés dans les préférences de l'application et utilisés
 * par [WeatherUtils] pour formater températures, vent, pression, visibilité
 * et heure. Les données restent toujours récupérées en métrique depuis
 * l'API ; seules les conversions d'affichage changent.
 */
object UnitPrefs {

    private const val PREFS_NAME = "meteo_unit_prefs"

    private const val KEY_TEMP_UNIT = "temp_unit"
    private const val KEY_WIND_UNIT = "wind_unit"
    private const val KEY_PRESSURE_UNIT = "pressure_unit"
    private const val KEY_TIME_FORMAT = "time_format"
    private const val KEY_VALUE_PRECISION = "value_precision"

    /** Unité de température. */
    enum class TempUnit { CELSIUS, FAHRENHEIT }

    /** Unité de vitesse du vent. */
    enum class WindUnit { KMH, MPH }

    /** Unité de pression. */
    enum class PressureUnit { HPA, INHG }

    /** Format de l'heure. */
    enum class TimeFormat { FORMAT_24H, FORMAT_12H }

    /** Précision des valeurs numériques (température, vent). */
    enum class ValuePrecision { WHOLE, ONE_DECIMAL }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTempUnit(context: Context): TempUnit =
        enumValueOfSafe(
            prefs(context).getString(KEY_TEMP_UNIT, null),
            TempUnit.CELSIUS
        )

    fun setTempUnit(context: Context, unit: TempUnit) {
        prefs(context).edit().putString(KEY_TEMP_UNIT, unit.name).apply()
    }

    fun getWindUnit(context: Context): WindUnit =
        enumValueOfSafe(
            prefs(context).getString(KEY_WIND_UNIT, null),
            WindUnit.KMH
        )

    fun setWindUnit(context: Context, unit: WindUnit) {
        prefs(context).edit().putString(KEY_WIND_UNIT, unit.name).apply()
    }

    fun getPressureUnit(context: Context): PressureUnit =
        enumValueOfSafe(
            prefs(context).getString(KEY_PRESSURE_UNIT, null),
            PressureUnit.HPA
        )

    fun setPressureUnit(context: Context, unit: PressureUnit) {
        prefs(context).edit().putString(KEY_PRESSURE_UNIT, unit.name).apply()
    }

    fun getTimeFormat(context: Context): TimeFormat =
        enumValueOfSafe(
            prefs(context).getString(KEY_TIME_FORMAT, null),
            TimeFormat.FORMAT_24H
        )

    fun setTimeFormat(context: Context, format: TimeFormat) {
        prefs(context).edit().putString(KEY_TIME_FORMAT, format.name).apply()
    }

    fun getValuePrecision(context: Context): ValuePrecision =
        enumValueOfSafe(
            prefs(context).getString(KEY_VALUE_PRECISION, null),
            ValuePrecision.WHOLE
        )

    fun setValuePrecision(context: Context, precision: ValuePrecision) {
        prefs(context).edit().putString(KEY_VALUE_PRECISION, precision.name).apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOfSafe(value: String?, default: T): T =
        try {
            if (value == null) default else enumValueOf<T>(value)
        } catch (_: IllegalArgumentException) {
            default
        }
}
