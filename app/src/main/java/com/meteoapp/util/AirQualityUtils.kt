package com.meteoapp.util

import android.content.Context
import com.meteoapp.R

/**
 * Utilitaires qualité de l'air : libellé OWM AQI (1–5), couleur associée
 * et recommandation santé associée à chaque niveau.
 */
object AirQualityUtils {

    fun label(context: Context, aqi: Long): String = when (aqi) {
        1L -> context.getString(R.string.aqi_good)
        2L -> context.getString(R.string.aqi_fair)
        3L -> context.getString(R.string.aqi_moderate)
        4L -> context.getString(R.string.aqi_poor)
        else -> context.getString(R.string.aqi_very_poor)
    }

    fun recommendation(context: Context, aqi: Long): String = when (aqi) {
        1L -> context.getString(R.string.aqi_reco_good)
        2L -> context.getString(R.string.aqi_reco_fair)
        3L -> context.getString(R.string.aqi_reco_moderate)
        4L -> context.getString(R.string.aqi_reco_poor)
        else -> context.getString(R.string.aqi_reco_very_poor)
    }

    fun color(aqi: Long): Int = when (aqi) {
        1L -> 0xFF00E676.toInt()
        2L -> 0xFFAEEA00.toInt()
        3L -> 0xFFFFD600.toInt()
        4L -> 0xFFFF6D00.toInt()
        else -> 0xFFD50000.toInt()
    }

    fun formatPm25(value: Double): String =
        String.format(java.util.Locale.getDefault(), "%.1f", value)
}
