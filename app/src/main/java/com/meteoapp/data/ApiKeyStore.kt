package com.meteoapp.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Stockage persistant de la clé API OpenWeatherMap saisie par l'utilisateur.
 * La clé survit aux redémarrages de l'app et du téléphone.
 */
object ApiKeyStore {

    private const val PREFS_NAME = "meteo_prefs"
    private const val KEY_API_KEY = "open_weather_api_key"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getApiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "").orEmpty()

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().apply {
            putString(KEY_API_KEY, key.trim())
            apply()
        }
    }

    fun isConfigured(context: Context): Boolean =
        getApiKey(context).isNotBlank()
}
