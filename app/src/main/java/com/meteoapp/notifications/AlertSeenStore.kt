package com.meteoapp.notifications

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

/**
 * Anti-spam des alertes météo : une même alerte (type + ville) n'est
 * notifiée qu'une fois par fenêtre de [SEEN_WINDOW_MS]. La clé inclut la
 * ville : une alerte pluie vue pour Paris ne masque plus celle de Lyon
 * (BUG-006).
 */
object AlertSeenStore {

    private const val PREFS_SEEN = "meteo_alert_seen"
    const val SEEN_WINDOW_MS = 12 * 60 * 60 * 1000L

    /**
     * Alertes pas encore notifiées pour cette ville dans la fenêtre ; elles
     * sont marquées vues à [nowMs]. Les entrées expirées (y compris les
     * anciennes clés sans ville) sont purgées au passage.
     */
    fun filterUnseen(
        context: Context,
        lat: Double,
        lon: Double,
        alerts: List<WeatherAlertEvaluator.Alert>,
        nowMs: Long = System.currentTimeMillis()
    ): List<WeatherAlertEvaluator.Alert> {
        val prefs = context.getSharedPreferences(PREFS_SEEN, Context.MODE_PRIVATE)
        val expired = prefs.all.filter { (_, seenAt) ->
            seenAt !is Long || nowMs - seenAt >= SEEN_WINDOW_MS
        }.keys
        val fresh = alerts.filter { alert ->
            val key = key(lat, lon, alert)
            key in expired || !prefs.contains(key)
        }
        prefs.edit {
            expired.forEach { remove(it) }
            fresh.forEach { putLong(key(lat, lon, it), nowMs) }
        }
        return fresh
    }

    private fun key(lat: Double, lon: Double, alert: WeatherAlertEvaluator.Alert): String =
        String.format(Locale.US, "%.2f,%.2f:%s", lat, lon, alert.key)
}
