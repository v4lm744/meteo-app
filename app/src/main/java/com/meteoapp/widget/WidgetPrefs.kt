package com.meteoapp.widget

import android.content.Context
import android.content.SharedPreferences
import com.meteoapp.data.model.GeoLocation

object WidgetPrefs {

    private const val PREFS_NAME = "meteo_widget_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Enregistre la ville du widget. Les coordonnées sont stockées en
     * String (précision Double complète) : le stockage Float historique
     * arrondissait à ~7 chiffres significatifs, soit une erreur pouvant
     * atteindre ~1 km et fausser la météo affichée pour des villes proches.
     */
    fun saveCity(context: Context, appWidgetId: Int, city: GeoLocation) {
        prefs(context).edit().apply {
            putString(keyName(appWidgetId), city.name)
            putString(keyLocalName(appWidgetId), city.localNames?.fr)
            putString(keyLat(appWidgetId), city.lat.toString())
            putString(keyLon(appWidgetId), city.lon.toString())
            putString(keyCountry(appWidgetId), city.country)
            putString(keyState(appWidgetId), city.state)
            apply()
        }
    }

    fun getCity(context: Context, appWidgetId: Int): GeoLocation? {
        val p = prefs(context)
        val name = p.getString(keyName(appWidgetId), null) ?: return null
        val local = p.getString(keyLocalName(appWidgetId), null)
        // Double précis si présent, sinon repli sur l'ancien Float
        // (getString lève une ClassCastException sur une valeur Float héritée,
        // d'où les runCatching pour ne jamais planter sur les anciens widgets).
        val lat = runCatching { p.getString(keyLat(appWidgetId), null) }.getOrNull()?.toDoubleOrNull()
            ?: runCatching { p.getFloat(keyLat(appWidgetId), 0f) }.getOrDefault(0f).toDouble()
        val lon = runCatching { p.getString(keyLon(appWidgetId), null) }.getOrNull()?.toDoubleOrNull()
            ?: runCatching { p.getFloat(keyLon(appWidgetId), 0f) }.getOrDefault(0f).toDouble()
        val country = p.getString(keyCountry(appWidgetId), null)
        val state = p.getString(keyState(appWidgetId), null)
        return GeoLocation(
            name = name,
            localNames = com.meteoapp.data.model.LocalNames(fr = local, en = null, featureName = null),
            lat = lat,
            lon = lon,
            country = country,
            state = state
        )
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().apply {
            remove(keyName(appWidgetId))
            remove(keyLocalName(appWidgetId))
            remove(keyLat(appWidgetId))
            remove(keyLon(appWidgetId))
            remove(keyCountry(appWidgetId))
            remove(keyState(appWidgetId))
            apply()
        }
    }

    private fun keyName(id: Int) = "name_$id"
    private fun keyLocalName(id: Int) = "local_$id"
    private fun keyLat(id: Int) = "lat_$id"
    private fun keyLon(id: Int) = "lon_$id"
    private fun keyCountry(id: Int) = "country_$id"
    private fun keyState(id: Int) = "state_$id"
}
