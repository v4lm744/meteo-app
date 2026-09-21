package com.meteoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Vérifie le calcul de phase lunaire : bornes du cycle, fractions
 * éclairées aux moments clés et correspondance des âges avec les phases.
 */
class MoonPhaseTest {

    private val referenceEpoch = 947182440L

    @Test
    fun referenceEpoch_isNewMoon() {
        val age = MoonPhase.ageDays(referenceEpoch)
        assertEquals(0.0, age, 0.5)
        assertEquals(MoonPhase.Phase.NEW_MOON, MoonPhase.phase(referenceEpoch))
        assertEquals(0.0, MoonPhase.illuminatedFraction(referenceEpoch), 0.05)
    }

    @Test
    fun halfCycle_isFullMoon() {
        val epoch = referenceEpoch + 1276961L
        val age = MoonPhase.ageDays(epoch)
        assertEquals(14.765, age, 0.1)
        assertEquals(MoonPhase.Phase.FULL_MOON, MoonPhase.phase(epoch))
        assertEquals(1.0, MoonPhase.illuminatedFraction(epoch), 0.05)
    }

    @Test
    fun quarterCycle_isFirstQuarter() {
        val epoch = referenceEpoch + 638481L
        assertEquals(MoonPhase.Phase.FIRST_QUARTER, MoonPhase.phase(epoch))
        assertEquals(0.5, MoonPhase.illuminatedFraction(epoch), 0.05)
    }

    @Test
    fun age_alwaysWithinCycleBounds() {
        val samples = longArrayOf(
            referenceEpoch,
            referenceEpoch + 100000000L,
            referenceEpoch - 100000000L
        )
        for (epoch in samples) {
            val age = MoonPhase.ageDays(epoch)
            assert(age in 0.0..29.530588853)
        }
    }
}
