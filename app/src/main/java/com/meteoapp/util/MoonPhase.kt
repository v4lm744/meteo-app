package com.meteoapp.util

import java.time.Instant

/**
 * Phase lunaire approximative calculée localement à partir du cycle synodique
 * (29,530588 jours). Précision ~1 jour, suffisante pour un affichage
 * descriptif. La référence est la nouvelle lune du 6 janvier 2000 (synodic
 * epoch classique).
 */
object MoonPhase {

    private const val SYNODIC_DAYS = 29.530588853
    private const val REFERENCE_NEW_MOON_EPOCH = 947182440L

    enum class Phase {
        NEW_MOON,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL_MOON,
        WANING_GIBBOUS,
        LAST_QUARTER,
        WANING_CRESCENT
    }

    /** Âge de la lune en jours (0 = nouvelle lune, ~14,8 = pleine lune). */
    fun ageDays(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Double {
        val daysSince = (nowEpochSeconds - REFERENCE_NEW_MOON_EPOCH) / 86400.0
        return ((daysSince % SYNODIC_DAYS) + SYNODIC_DAYS) % SYNODIC_DAYS
    }

    /** Fraction éclairée du disque (0 = nouvelle lune, 1 = pleine lune). */
    fun illuminatedFraction(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Double {
        val age = ageDays(nowEpochSeconds)
        return 0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * age / SYNODIC_DAYS))
    }

    fun phase(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Phase {
        val age = ageDays(nowEpochSeconds)
        return when {
            age < 1.0 || age > SYNODIC_DAYS - 1.0 -> Phase.NEW_MOON
            age < SYNODIC_DAYS * 0.25 - 1.0 -> Phase.WAXING_CRESCENT
            age < SYNODIC_DAYS * 0.25 + 1.0 -> Phase.FIRST_QUARTER
            age < SYNODIC_DAYS * 0.5 - 1.0 -> Phase.WAXING_GIBBOUS
            age < SYNODIC_DAYS * 0.5 + 1.0 -> Phase.FULL_MOON
            age < SYNODIC_DAYS * 0.75 - 1.0 -> Phase.WANING_GIBBOUS
            age < SYNODIC_DAYS * 0.75 + 1.0 -> Phase.LAST_QUARTER
            else -> Phase.WANING_CRESCENT
        }
    }
}
