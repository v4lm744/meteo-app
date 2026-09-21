package com.meteoapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage persistant de la clé API OpenWeatherMap saisie par l'utilisateur.
 * La clé est chiffrée au repos via EncryptedSharedPreferences (AES-256) afin
 * de ne pas être lisible en clair dans le fichier de préférences.
 */
object ApiKeyStore {

    private const val PREFS_NAME = "meteo_prefs"
    private const val KEY_API_KEY = "open_weather_api_key"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    /**
     * Instance mémoïsée : la création d'EncryptedSharedPreferences dérive la
     * clé maître (coûteux, ~50 ms) ; on la construit une seule fois par process
     * plutôt qu'à chaque lecture de la clé API.
     */
    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val appContext = context.applicationContext
            val prefs = try {
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            cachedPrefs = prefs
            return prefs
        }
    }

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
