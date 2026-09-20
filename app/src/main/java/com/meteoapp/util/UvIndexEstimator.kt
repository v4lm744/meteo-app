package com.meteoapp.util

import android.content.Context
import com.meteoapp.R
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private fun cosDeg(deg: Double): Double = cos(Math.toRadians(deg))
private fun sinDeg(deg: Double): Double = sin(Math.toRadians(deg))

/**
 * Estimation locale de l'indice UV à partir de la position, de l'heure et
 * de la nébulosité (l'API forecast gratuite ne fournit pas l'UV) :
 * élévation solaire (formule NOAA simplifiée) → UV ciel clair, réduit par
 * la couverture nuageuse.
 */
object UvIndexEstimator {

    fun estimate(
        lat: Double,
        lon: Double,
        timestampSeconds: Long,
        timezoneOffsetSeconds: Long,
        cloudinessPercent: Long
    ): Double {
        val elevationDeg = solarElevationDeg(lat, lon, timestampSeconds, timezoneOffsetSeconds)
        if (elevationDeg <= 0) return 0.0
        val clearSkyUv = clearSkyUv(elevationDeg)
        val cloudFactor = cloudReduction(cloudinessPercent)
        return max(0.0, min(11.5, clearSkyUv * cloudFactor))
    }

    /**
     * Élévation solaire en degrés (formule NOAA simplifiée).
     */
    fun solarElevationDeg(
        lat: Double,
        lon: Double,
        timestampSeconds: Long,
        timezoneOffsetSeconds: Long
    ): Double {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("UTC")
            timeInMillis = (timestampSeconds + timezoneOffsetSeconds) * 1000L
        }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val hour = cal.get(Calendar.HOUR_OF_DAY) +
            cal.get(Calendar.MINUTE) / 60.0

        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1 + (hour - 12) / 24.0)
        val declination = 0.006918 -
            0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

        val localSolarTime = hour + lon / 15.0
        val hourAngle = Math.toRadians(15.0 * (localSolarTime - 12.0))
        val latRad = Math.toRadians(lat)

        val elevation = sin(latRad) * sin(declination) +
            cos(latRad) * cos(declination) * cos(hourAngle)
        return Math.toDegrees(asin(elevation))
    }

    private fun clearSkyUv(elevationDeg: Double): Double {
        // Approximation OMS : UV ≈ 12.5 * sin(élévation)³ pour ciel clair
        val s = sinDeg(elevationDeg)
        return 12.5 * s * s * s
    }

    private fun cloudReduction(cloudinessPercent: Long): Double {
        val clouds = max(0.0, min(100.0, cloudinessPercent.toDouble()))
        return 1.0 - 0.75 * (clouds / 100.0).pow3()
    }

    private fun Double.pow3(): Double = this * this * this

    fun label(context: Context, uv: Double): String = when {
        uv < 3 -> context.getString(R.string.uv_low)
        uv < 6 -> context.getString(R.string.uv_moderate)
        uv < 8 -> context.getString(R.string.uv_high)
        uv < 11 -> context.getString(R.string.uv_very_high)
        else -> context.getString(R.string.uv_extreme)
    }

    fun recommendation(context: Context, uv: Double): String = when {
        uv < 3 -> context.getString(R.string.uv_reco_low)
        uv < 6 -> context.getString(R.string.uv_reco_moderate)
        uv < 8 -> context.getString(R.string.uv_reco_high)
        uv < 11 -> context.getString(R.string.uv_reco_very_high)
        else -> context.getString(R.string.uv_reco_extreme)
    }
}
