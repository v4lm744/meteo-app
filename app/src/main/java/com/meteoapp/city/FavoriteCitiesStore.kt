package com.meteoapp.city

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.GeoLocation
import com.squareup.moshi.Types

/**
 * Gestion multi-villes : liste ordonnée des villes favorites de
 * l'utilisateur, persistée en JSON dans les SharedPreferences. La ville
 * « courante » (dernier affiché) est mémorisée séparément.
 */
object FavoriteCitiesStore {

    private const val PREFS_NAME = "meteo_favorite_cities"
    private const val KEY_CITIES = "favorite_cities_json"
    private const val KEY_CURRENT = "current_city_json"

    private val listAdapter by lazy {
        ApiClient.moshi.adapter<List<GeoLocation>>(
            Types.newParameterizedType(List::class.java, GeoLocation::class.java)
        )
    }

    private val cityAdapter by lazy {
        ApiClient.moshi.adapter(GeoLocation::class.java)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): List<GeoLocation> {
        val json = prefs(context).getString(KEY_CITIES, null) ?: return emptyList()
        return runCatching { listAdapter.fromJson(json) ?: emptyList() }.getOrDefault(emptyList())
    }

    fun setFavorites(context: Context, cities: List<GeoLocation>) {
        val json = listAdapter.toJson(cities)
        prefs(context).edit { putString(KEY_CITIES, json) }
    }

    fun addFavorite(context: Context, city: GeoLocation) {
        val current = getFavorites(context)
        if (current.none { it.lat == city.lat && it.lon == city.lon }) {
            setFavorites(context, current + city)
        }
    }

    fun removeFavorite(context: Context, city: GeoLocation) {
        setFavorites(
            context,
            getFavorites(context).filterNot { it.lat == city.lat && it.lon == city.lon }
        )
    }

    fun isFavorite(context: Context, city: GeoLocation): Boolean =
        getFavorites(context).any { it.lat == city.lat && it.lon == city.lon }

    /**
     * Ville utilisée par la notification quotidienne : la dernière ville
     * consultée si elle est encore connue, sinon la première favorite.
     */
    fun currentNotificationCity(context: Context): GeoLocation? {
        val json = prefs(context).getString(KEY_CURRENT, null)
        val current = json?.let { runCatching { cityAdapter.fromJson(it) }.getOrNull() }
        return current ?: getFavorites(context).firstOrNull()
    }

    /**
     * Villes notifiées par le résumé quotidien : toutes les favorites, la
     * ville courante en tête si elle n'y figure pas déjà (l'utilisateur
     * s'intéresse de près à la dernière ville consultée).
     */
    fun dailyNotificationCities(context: Context): List<GeoLocation> {
        val favorites = getFavorites(context)
        val current = getCurrentCity(context)
        return if (current != null && favorites.none { it.lat == current.lat && it.lon == current.lon }) {
            listOf(current) + favorites
        } else {
            favorites
        }
    }

    fun setCurrentCity(context: Context, city: GeoLocation) {
        prefs(context).edit { putString(KEY_CURRENT, cityAdapter.toJson(city)) }
    }

    fun getCurrentCity(context: Context): GeoLocation? {
        val json = prefs(context).getString(KEY_CURRENT, null) ?: return null
        return runCatching { cityAdapter.fromJson(json) }.getOrNull()
    }
}
