package com.meteoapp.stats

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.WeatherData
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import java.util.Calendar
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class DailyRecord(
    val dayKey: Long,
    val tempMin: Double,
    val tempMax: Double
)

/**
 * Historique local des températures quotidiennes par ville : alimenté à
 * chaque chargement de météo réussi, conservé 60 jours, consulté pour le
 * graphe des 7 derniers jours, les records absolus et la comparaison avec
 * la semaine précédente.
 */
object WeatherHistoryStore {

    private const val PREFS_NAME = "meteo_history_prefs"
    private const val MAX_AGE_DAYS = 60L

    private val listAdapter by lazy {
        ApiClient.moshi.adapter<List<DailyRecord>>(
            Types.newParameterizedType(List::class.java, DailyRecord::class.java)
        )
    }

    private fun keyFor(lat: Double, lon: Double): String =
        String.format(java.util.Locale.US, "history_%.2f_%.2f", lat, lon)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecords(context: Context, lat: Double, lon: Double): List<DailyRecord> {
        val json = prefs(context).getString(keyFor(lat, lon), null) ?: return emptyList()
        return runCatching { listAdapter.fromJson(json) ?: emptyList() }
            .getOrDefault(emptyList())
            .sortedBy { it.dayKey }
    }

    fun record(context: Context, weather: WeatherData) {
        val fresh = weather.daily
            .filter { it.tempMin > -90.0 && it.tempMax < 60.0 }
            .map { DailyRecord(dayKey(it.dt, weather.timezoneOffset), it.tempMin, it.tempMax) }
        if (fresh.isEmpty()) return

        val existing = getRecords(context, weather.lat, weather.lon).toMutableList()
        for (record in fresh) {
            val index = existing.indexOfFirst { it.dayKey == record.dayKey }
            if (index >= 0) {
                existing[index] = DailyRecord(
                    dayKey = record.dayKey,
                    tempMin = minOf(existing[index].tempMin, record.tempMin),
                    tempMax = maxOf(existing[index].tempMax, record.tempMax)
                )
            } else {
                existing.add(record)
            }
        }
        val cutoff = todayKey(weather.timezoneOffset) - MAX_AGE_DAYS * 86_400L
        val kept = existing.filter { it.dayKey >= cutoff }.sortedBy { it.dayKey }
        prefs(context).edit {
            putString(keyFor(weather.lat, weather.lon), listAdapter.toJson(kept))
        }
    }

    fun last7Days(context: Context, lat: Double, lon: Double): List<DailyRecord> =
        getRecords(context, lat, lon).takeLast(7)

    fun allTimeMin(context: Context, lat: Double, lon: Double): DailyRecord? =
        getRecords(context, lat, lon).minByOrNull { it.tempMin }

    fun allTimeMax(context: Context, lat: Double, lon: Double): DailyRecord? =
        getRecords(context, lat, lon).maxByOrNull { it.tempMax }

    /**
     * Comparaison semaine courante vs semaine précédente : températures
     * moyennes de chaque période (moyenne des min/max quotidiens).
     * Retourne (semaine courante, semaine précédente) ou null si données
     * insuffisantes.
     */
    fun weekComparison(context: Context, lat: Double, lon: Double): Pair<Double, Double>? {
        val records = getRecords(context, lat, lon)
        if (records.isEmpty()) return null
        val today = todayKey(0)
        fun averageOf(fromDaysAgo: Long, toDaysAgo: Long): Double? {
            val window = records.filter {
                it.dayKey >= today - fromDaysAgo * 86_400L && it.dayKey < today - toDaysAgo * 86_400L
            }
            if (window.isEmpty()) return null
            return window.map { (it.tempMin + it.tempMax) / 2.0 }.average()
        }
        val current = averageOf(7, 0) ?: return null
        val previous = averageOf(14, 7) ?: return null
        return Pair(current, previous)
    }

    fun todayKey(timezoneOffsetSeconds: Long): Long =
        dayKey(System.currentTimeMillis() / 1000L, timezoneOffsetSeconds)

    fun dayKey(timestampSeconds: Long, timezoneOffsetSeconds: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / 1000L
    }
}
