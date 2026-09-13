package com.meteoapp.data

import android.content.Context
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.WeatherData
import java.io.File

/**
 * Cache hors ligne simple des dernières données météo récupérées.
 *
 * Stocke le [WeatherData] sérialisé en JSON dans le cache de fichiers de
 * l'application, indexé par une clé (lat, lon) arrondie, avec un timestamp.
 * Permet d'afficher une météo périmée mais pertinente en cas d'échec réseau,
 * plutôt qu'un écran d'erreur vide.
 */
class WeatherCache(context: Context) {

    private val cacheDir: File = context.applicationContext.cacheDir
    private val moshi = ApiClient.moshi
    private val adapter = moshi.adapter(WeatherData::class.java)

    fun save(lat: Double, lon: Double, data: WeatherData) {
        val payload = CachedWeather(
            savedAt = System.currentTimeMillis(),
            data = data
        )
        val json = cacheAdapter.toJson(payload) ?: return
        runCatching {
            File(cacheDir, fileName(lat, lon)).writeText(json)
        }
    }

    fun load(lat: Double, lon: Double): WeatherData? {
        val file = File(cacheDir, fileName(lat, lon))
        if (!file.exists()) return null
        return runCatching {
            cacheAdapter.fromJson(file.readText())?.data
        }.getOrNull()
    }

    fun ageMillis(lat: Double, lon: Double): Long {
        val file = File(cacheDir, fileName(lat, lon))
        if (!file.exists()) return Long.MAX_VALUE
        return runCatching {
            val cached = cacheAdapter.fromJson(file.readText())
            if (cached != null) System.currentTimeMillis() - cached.savedAt else Long.MAX_VALUE
        }.getOrDefault(Long.MAX_VALUE)
    }

    private fun fileName(lat: Double, lon: Double): String =
        "weather_${round(lat)}_${round(lon)}.json"

    private fun round(value: Double): String =
        String.format("%.2f", value)

    private val cacheAdapter by lazy {
        moshi.adapter(CachedWeather::class.java)
    }

    private data class CachedWeather(
        val savedAt: Long,
        val data: WeatherData
    )
}
