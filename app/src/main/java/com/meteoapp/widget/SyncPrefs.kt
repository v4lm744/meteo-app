package com.meteoapp.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Stockage persistant de l'intervalle de synchronisation automatique des widgets.
 *
 * L'intervalle choisi (en minutes) est conservé dans les préférences de l'application
 * et survit aux redémarrages. Une valeur de [SYNC_DISABLED] désactive la synchro
 * automatique : seuls le rafraîchissement manuel et la mise à jour système du widget
 * (updatePeriodMillis) restent actifs.
 */
object SyncPrefs {

    private const val PREFS_NAME = "meteo_sync_prefs"
    private const val KEY_INTERVAL_MINUTES = "sync_interval_minutes"

    /** Valeur spéciale indiquant que la synchro automatique est désactivée. */
    const val SYNC_DISABLED = 0

    /**
     * Intervals proposés dans le menu Paramètres, en minutes.
     * L'ordre suit l'ordre d'affichage du dialogue de réglages.
     */
    val INTERVAL_OPTIONS_MINUTES: List<Int> = listOf(15, 30, 60, 180)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Intervalle de synchro en minutes ([SYNC_DISABLED] si désactivé).
     * Par défaut 30 minutes.
     */
    fun getIntervalMinutes(context: Context): Int =
        prefs(context).getInt(KEY_INTERVAL_MINUTES, 30)

    fun setIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit { putInt(KEY_INTERVAL_MINUTES, minutes) }
    }

    fun isAutoSyncEnabled(context: Context): Boolean =
        getIntervalMinutes(context) != SYNC_DISABLED
}
