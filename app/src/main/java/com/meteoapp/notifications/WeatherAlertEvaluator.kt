package com.meteoapp.notifications

import android.content.Context
import com.meteoapp.R
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.WeatherData
import com.meteoapp.util.WeatherUtils

/**
 * Détection locale de seuils d'alerte sur les 12 prochaines heures :
 * fortes pluies cumulées, vent violent, gel. Retourne la liste des
 * alertes déclenchées avec leur message prêt à afficher.
 *
 * La fenêtre est temporelle (et non un nombre de créneaux) : le pas
 * horaire vaut 1 h avec Open-Meteo et 3 h avec OpenWeatherMap (BUG-002).
 */
object WeatherAlertEvaluator {

    data class Alert(val key: String, val message: String)

    private const val WINDOW_SECONDS = 12 * 3600L
    private const val DEFAULT_STEP_SECONDS = 3600L

    fun evaluate(
        context: Context,
        weather: WeatherData,
        nowSeconds: Long = System.currentTimeMillis() / 1000
    ): List<Alert> {
        val window = alertWindow(weather.hourly, nowSeconds)
        if (window.isEmpty()) return emptyList()

        val alerts = mutableListOf<Alert>()

        // rainVolume/snowVolume sont déjà des cumuls sur le pas du créneau
        // (1 h ou 3 h) : la somme donne directement le cumul de la fenêtre.
        val rainMm = window.sumOf { it.rainVolume + it.snowVolume }
        val rainThreshold = AlertPrefs.getRainThresholdMm(context)
        if (rainMm >= rainThreshold) {
            alerts.add(
                Alert(
                    key = "rain",
                    message = context.getString(
                        R.string.alert_heavy_rain,
                        String.format(java.util.Locale.getDefault(), "%.1f mm", rainMm)
                    )
                )
            )
        }

        val windKmh = window.maxOf { it.windSpeed * 3.6 }
        val windThreshold = AlertPrefs.getWindThresholdKmh(context)
        if (windKmh >= windThreshold) {
            alerts.add(
                Alert(
                    key = "wind",
                    message = context.getString(
                        R.string.alert_strong_wind,
                        WeatherUtils.formatWindSpeed(context, window.maxOf { it.windSpeed })
                    )
                )
            )
        }

        val frostThreshold = AlertPrefs.getFrostThresholdCelsius(context)
        val minTemp = window.minOf { it.temp }
        if (minTemp <= frostThreshold) {
            alerts.add(
                Alert(
                    key = "frost",
                    message = context.getString(
                        R.string.alert_frost,
                        WeatherUtils.formatTemp(context, minTemp)
                    )
                )
            )
        }

        return alerts
    }

    /**
     * Créneaux qui recoupent `[now, now + 12 h[` : un créneau commencé
     * avant `now` est retenu tant qu'il n'est pas terminé (dt + pas > now).
     * Des données en cache entièrement passées donnent une fenêtre vide.
     */
    internal fun alertWindow(hourly: List<HourlyData>, nowSeconds: Long): List<HourlyData> {
        val step = hourly.zipWithNext { a, b -> b.dt - a.dt }.firstOrNull { it > 0 }
            ?: DEFAULT_STEP_SECONDS
        val end = nowSeconds + WINDOW_SECONDS
        return hourly.filter { it.dt + step > nowSeconds && it.dt < end }
    }
}
