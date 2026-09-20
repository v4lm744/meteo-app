package com.meteoapp.util

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * Préférences d'apparence : mode de thème (suivre le système, clair forcé,
 * sombre forcé) et activation des couleurs dynamiques Material You
 * (Android 12+, tirées du fond d'écran).
 */
object ThemePrefs {

    private const val PREFS_NAME = "meteo_theme_prefs"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_DYNAMIC = "dynamic_colors"

    /** Mode de thème applicatif. */
    enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMode(context: Context): ThemeMode {
        val name = prefs(context).getString(KEY_MODE, null)
        return when (name) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.FOLLOW_SYSTEM
        }
    }

    fun setMode(context: Context, mode: ThemeMode) {
        prefs(context).edit { putString(KEY_MODE, mode.name) }
    }

    /** Applique le mode persisté au delegate global (à appeler au démarrage). */
    fun applyMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (getMode(context)) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    /** Couleurs dynamiques Material You activées (défaut : activées si dispo). */
    fun isDynamicColorsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(
            KEY_DYNAMIC,
            isDynamicColorsSupported(context)
        )

    fun setDynamicColorsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_DYNAMIC, enabled) }
    }

    /** Material You n'est disponible qu'à partir d'Android 12 (API 31). */
    fun isDynamicColorsSupported(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT >= 31 && hasSupportedUiMode(context)

    private fun hasSupportedUiMode(context: Context): Boolean =
        runCatching {
            val manager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
            manager?.currentModeType != android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        }.getOrDefault(true)
}
