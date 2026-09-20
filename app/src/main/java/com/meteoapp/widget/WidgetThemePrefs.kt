package com.meteoapp.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Thème choisi pour chaque widget lors de sa configuration :
 * dégradé météo (par défaut) ou fond sombre uni.
 */
object WidgetThemePrefs {

    enum class Theme { WEATHER_GRADIENT, DARK }

    private const val PREFS_NAME = "meteo_widget_theme_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun keyFor(appWidgetId: Int) = "theme_$appWidgetId"

    fun getTheme(context: Context, appWidgetId: Int): Theme =
        if (prefs(context).getString(keyFor(appWidgetId), null) == Theme.DARK.name) {
            Theme.DARK
        } else {
            Theme.WEATHER_GRADIENT
        }

    fun setTheme(context: Context, appWidgetId: Int, theme: Theme) {
        prefs(context).edit { putString(keyFor(appWidgetId), theme.name) }
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit { remove(keyFor(appWidgetId)) }
    }
}
