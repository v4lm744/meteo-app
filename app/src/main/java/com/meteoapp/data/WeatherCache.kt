package com.meteoapp.data

import android.content.Context
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.WeatherData
import java.io.File

/**
 * Cache hors ligne simple des dernières données météo récupérées.
 *
 * Stocke le [WeatherData] sérialisé en JSON dans le cache de fichiers de
 * l'application, indexé par une clé (lat, lon) arrondie. L'horodatage du
 * cache repose sur la date de modification du fichier, évitant un wrapper
 * supplémentaire à sérialiser.
 *
 * Permet d'afficher une météo périmée mais pertinente en cas d'échec réseau,
 * plutôt qu'un écran d'erreur vide.
 */
class WeatherCache(context: Context) {

    companion object {
        const val FRESH_MAX_AGE_MS = 10 * 60 * 1000L
    }

    private val cacheDir: File = context.applicationContext.cacheDir
    private val moshi = ApiClient.moshi
    private val adapter = moshi.adapter(WeatherData::class.java)

    fun save(lat: Double, lon: Double, data: WeatherData) {
        val json = adapter.toJson(data)
        runCatching {
            File(cacheDir, fileName(lat, lon)).writeText(json)
        }
    }

    fun load(lat: Double, lon: Double): WeatherData? {
        val file = File(cacheDir, fileName(lat, lon))
        if (!file.exists()) return null
        return runCatching { adapter.fromJson(file.readText()) }.getOrNull()
    }

    /**
     * Données du cache si elles sont plus récentes que [FRESH_MAX_AGE_MS],
     * sinon `null` : permet un affichage immédiat sans requête réseau.
     */
    fun loadIfFresh(
        lat: Double,
        lon: Double,
        maxAgeMs: Long = FRESH_MAX_AGE_MS
    ): WeatherData? {
        if (ageMillis(lat, lon) > maxAgeMs) return null
        return load(lat, lon)
    }

    fun ageMillis(lat: Double, lon: Double): Long {
        val file = File(cacheDir, fileName(lat, lon))
        if (!file.exists()) return Long.MAX_VALUE
        val lastModified = file.lastModified()
        if (lastModified <= 0L) return Long.MAX_VALUE
        return System.currentTimeMillis() - lastModified
    }

    private fun fileName(lat: Double, lon: Double): String =
        "weather_${round(lat)}_${round(lon)}.json"

    private fun round(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value)
}
