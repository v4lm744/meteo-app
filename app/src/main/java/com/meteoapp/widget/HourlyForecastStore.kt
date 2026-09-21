package com.meteoapp.widget

import android.content.Context
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.model.HourlyData

/**
 * Stockage des prévisions horaires d'un widget collection : le
 * RemoteViewsFactory ne peut pas faire d'appels réseau, les données sont
 * donc rafraîchies en amont ([refresh]) et sérialisées dans les
 * SharedPreferences, une entrée JSON par widget.
 */
object HourlyForecastStore {

    private const val PREFS_NAME = "meteo_hourly_widget_prefs"

    data class HourSnapshot(
        val dt: Long,
        val temp: Double,
        val icon: String,
        val conditionId: Long,
        val pop: Double,
        val timezoneOffset: Long
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Met à jour les prévisions du widget depuis l'API puis notifie la liste. */
    suspend fun refresh(context: Context, appWidgetId: Int, city: com.meteoapp.data.model.GeoLocation): Boolean {
        val repository = WeatherRepository(context)
        val result = repository.getWeather(city.lat, city.lon)
        val weather = (result as? WeatherResult.Success)?.data ?: return false
        val entries = weather.hourly.take(12).map { hour ->
            val cond = hour.weather.firstOrNull()
            HourSnapshot(
                dt = hour.dt,
                temp = hour.temp,
                icon = cond?.icon ?: "01d",
                conditionId = cond?.id ?: 800L,
                pop = hour.pop ?: 0.0,
                timezoneOffset = hour.timezoneOffset
            )
        }
        save(context, appWidgetId, entries)
        return true
    }

    fun save(context: Context, appWidgetId: Int, entries: List<HourSnapshot>) {
        val json = entries.joinToString(";") { e ->
            listOf(
                e.dt.toString(),
                e.temp.toString(),
                e.icon,
                e.conditionId.toString(),
                e.pop.toString(),
                e.timezoneOffset.toString()
            ).joinToString(",")
        }
        prefs(context).edit().putString(keyFor(appWidgetId), json).apply()
    }

    fun loadSnapshot(context: Context, appWidgetId: Int): List<HourSnapshot> {
        val raw = prefs(context).getString(keyFor(appWidgetId), null) ?: return emptyList()
        return raw.split(";").mapNotNull { row ->
            val parts = row.split(",")
            if (parts.size != 6) return@mapNotNull null
            HourSnapshot(
                dt = parts[0].toLongOrNull() ?: return@mapNotNull null,
                temp = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                icon = parts[2],
                conditionId = parts[3].toLongOrNull() ?: 800L,
                pop = parts[4].toDoubleOrNull() ?: 0.0,
                timezoneOffset = parts[5].toLongOrNull() ?: 0L
            )
        }
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(keyFor(appWidgetId)).apply()
    }

    private fun keyFor(id: Int) = "hours_$id"
}
