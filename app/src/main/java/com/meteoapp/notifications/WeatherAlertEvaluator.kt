package com.meteoapp.notifications

import android.content.Context
import com.meteoapp.R
import com.meteoapp.data.model.WeatherData
import com.meteoapp.util.WeatherUtils
import java.util.Locale

/**
 * Détection locale de seuils d'alerte sur les 12 prochaines heures :
 * fortes pluies cumulées, vent violent, gel. Retourne la liste des
 * alertes déclenchées avec leur message prêt à afficher.
 */
object WeatherAlertEvaluator {

    data class Alert(val key: String, val message: String)

    private const val WINDOW_HOURS = 12

    fun evaluate(context: Context, weather: WeatherData): List<Alert> {
        val window = weather.hourly.take(WINDOW_HOURS)
        if (window.isEmpty()) return emptyList()

        val alerts = mutableListOf<Alert>()

        val rainMm = window.sumOf { it.rainVolume + it.snowVolume } * 3.0
        val rainThreshold = AlertPrefs.getRainThresholdMm(context)
        if (rainMm >= rainThreshold) {
            alerts.add(
                Alert(
                    key = "rain",
                    message = context.getString(
                        R.string.alert_heavy_rain,
                        String.format(Locale.FRANCE, "%.1f mm", rainMm)
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
}
