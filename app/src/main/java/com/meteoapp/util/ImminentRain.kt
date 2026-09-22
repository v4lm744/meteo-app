package com.meteoapp.util

import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.MinutelyPrecipitation

/**
 * Détecte une précipitation imminente dans les créneaux disponibles.
 * Source privilégiée : pas de 15 min (Open-Meteo minutely_15) quand la
 * réponse en fournit ; sinon repli sur les créneaux horaires (prévisions
 * de l'API). Le calcul reste volontairement simple et local : premier
 * créneau pluvieux à venir, exprimé en minutes depuis maintenant.
 */
object ImminentRain {

    /** Codes condition OpenWeather : orage, bruine, pluie, neige. */
    private val PRECIP_CONDITION_RANGES = listOf(
        200L..232L,
        300L..321L,
        500L..531L,
        600L..622L
    )

    /** Seuil de volume pour considérer un créneau minutely_15 pluvieux. */
    private const val MIN_MINUTELY_VOLUME_MM = 0.05

    /** Seuil de volume pour considérer un créneau comme pluvieux. */
    private const val MIN_VOLUME_MM = 0.1

    data class ImminentPrecipitation(
        val startsInMinutes: Long,
        val isSnow: Boolean,
        val probabilityPercent: Int
    )

    /**
     * Retourne la première précipitation à venir dans les créneaux de
     * 15 min si disponibles, sinon dans les créneaux horaires ; null si
     * rien n'est prévu (ou si les données sont trop anciennes).
     */
    fun detect(
        hours: List<HourlyData>,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000,
        minutely: List<MinutelyPrecipitation> = emptyList()
    ): ImminentPrecipitation? {
        detectFromMinutely(minutely, nowEpochSeconds)?.let { return it }
        val upcoming = hours
            .filter { it.dt > nowEpochSeconds }
            .sortedBy { it.dt }
            .take(MAX_SLOTS)
        for (hour in upcoming) {
            if (isPrecipitating(hour)) {
                return ImminentPrecipitation(
                    startsInMinutes = ((hour.dt - nowEpochSeconds) / 60).coerceAtLeast(0),
                    isSnow = hour.weather.any { it.id in 600..622 },
                    probabilityPercent = ((hour.pop ?: 0.0) * 100).toInt().coerceIn(0, 100)
                )
            }
        }
        return null
    }

    /** Détection sur le pas de 15 min : volume ou probabilité élevée. */
    private fun detectFromMinutely(
        minutely: List<MinutelyPrecipitation>,
        nowEpochSeconds: Long
    ): ImminentPrecipitation? {
        val upcoming = minutely
            .filter { it.dt > nowEpochSeconds }
            .sortedBy { it.dt }
        for (slot in upcoming) {
            if (slot.precipitation >= MIN_MINUTELY_VOLUME_MM) {
                return ImminentPrecipitation(
                    startsInMinutes = ((slot.dt - nowEpochSeconds) / 60).coerceAtLeast(0),
                    isSnow = slot.isSnow,
                    probabilityPercent = slot.probabilityPercent
                )
            }
        }
        return null
    }

    /** Vrai si le créneau présente une précipitation significative. */
    fun isPrecipitating(hour: HourlyData): Boolean {
        val conditionId = hour.weather.firstOrNull()?.id
        val byCondition = conditionId != null &&
            PRECIP_CONDITION_RANGES.any { conditionId in it }
        val byVolume = (hour.rainVolume + hour.snowVolume) >= MIN_VOLUME_MM
        return byCondition || byVolume
    }

    /**
     * Texte du bandeau : « Pluie attendue dans ~45 min » ou variante
     * neige, localisé, avec arrondi humain (~15, ~45, ~1 h 30…).
     */
    fun bannerText(context: android.content.Context, precipitation: ImminentPrecipitation): String {
        val approxMinutes = roundToHumanMinutes(precipitation.startsInMinutes)
        val kindRes = if (precipitation.isSnow) {
            com.meteoapp.R.string.rain_banner_snow
        } else {
            com.meteoapp.R.string.rain_banner_rain
        }
        val kind = context.getString(kindRes)
        return if (precipitation.probabilityPercent >= PROBABILITY_DISPLAY_THRESHOLD) {
            context.getString(
                com.meteoapp.R.string.rain_banner_with_probability,
                kind,
                formatMinutes(context, approxMinutes),
                precipitation.probabilityPercent
            )
        } else {
            context.getString(
                com.meteoapp.R.string.rain_banner_without_probability,
                kind,
                formatMinutes(context, approxMinutes)
            )
        }
    }

    /** Arrondit à un multiple de 15 min lisible (« ~45 min »). */
    fun roundToHumanMinutes(minutes: Long): Long =
        ((minutes + 7) / 15) * 15

    /** Formate des minutes en « 45 min » ou « 1 h 30 » localisé. */
    fun formatMinutes(context: android.content.Context, minutes: Long): String {
        if (minutes < 60) return context.getString(com.meteoapp.R.string.rain_banner_minutes, minutes)
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins == 0L) {
            context.getString(com.meteoapp.R.string.rain_banner_hours, hours)
        } else {
            context.getString(com.meteoapp.R.string.rain_banner_hours_minutes, hours, mins)
        }
    }

    private const val MAX_SLOTS = 4
    private const val PROBABILITY_DISPLAY_THRESHOLD = 10
}
