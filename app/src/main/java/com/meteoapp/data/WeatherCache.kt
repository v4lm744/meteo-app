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
        const val MAX_AGE_MS = 3 * 24 * 60 * 60 * 1000L
        const val MAX_ENTRIES = 8

        /**
         * Version du format des fichiers de cache, intégrée à leur nom.
         * L'incrémenter invalide les entrées écrites par les versions
         * précédentes (purgées par [trim]). v2 : vent Open-Meteo en m/s
         * (BUG-001), les fichiers v1 contenaient des km/h.
         */
        const val CACHE_VERSION = 2
        private const val LEGACY_PREFIX = "weather_"
        const val FILE_PREFIX = "weather_v${CACHE_VERSION}_"
    }

    private val cacheDir: File = context.applicationContext.cacheDir
    private val moshi = ApiClient.moshi
    private val adapter = moshi.adapter(WeatherData::class.java)

    fun save(lat: Double, lon: Double, data: WeatherData) {
        val json = adapter.toJson(data)
        runCatching {
            File(cacheDir, fileName(lat, lon)).writeText(json)
        }
        trim()
    }

    /**
     * Éviction : les fichiers de cache des villes rarement consultées
     * s'accumulaient indéfiniment. On conserve les [MAX_ENTRIES] plus
     * récents et on supprime tout fichier plus vieux que [MAX_AGE_MS],
     * quelle que soit leur position. Les fichiers d'une version de
     * format antérieure à [CACHE_VERSION] sont supprimés d'office.
     */
    fun trim(maxEntries: Int = MAX_ENTRIES, maxAgeMs: Long = MAX_AGE_MS) {
        val now = System.currentTimeMillis()
        val all = cacheDir.listFiles { file -> file.isFile && file.name.startsWith(LEGACY_PREFIX) }
            ?: return
        val (files, outdated) = all.partition { it.name.startsWith(FILE_PREFIX) }
        outdated.forEach { it.delete() }
        val expired = files.filter { now - it.lastModified() > maxAgeMs }
        expired.forEach { it.delete() }
        val kept = files.filterNot { it in expired }
            .sortedByDescending { it.lastModified() }
        if (kept.size > maxEntries) {
            kept.drop(maxEntries).forEach { it.delete() }
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
        "$FILE_PREFIX${round(lat)}_${round(lon)}.json"

    private fun round(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value)
}
