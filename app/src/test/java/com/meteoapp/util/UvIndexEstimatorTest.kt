package com.meteoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estimation locale de l'indice UV : formule claire + réduction par la
 * nébulosité. Les timestamps correspondent au 21 juin 2026 à Paris :
 * 02:00 UTC (nuit, UV nul) et 12:00 UTC (midi solaire proche, UV élevé).
 */
class UvIndexEstimatorTest {

    private val lat = 48.85
    private val lon = 2.35

    @Test
    fun `night returns zero uv`() {
        val uv = UvIndexEstimator.estimate(
            lat = lat, lon = lon,
            timestampSeconds = 1_782_007_200L,
            timezoneOffsetSeconds = 0L,
            cloudinessPercent = 0L
        )
        assertEquals(0.0, uv, 0.01)
    }

    @Test
    fun `noon summer returns significant uv`() {
        val uv = UvIndexEstimator.estimate(
            lat = lat, lon = lon,
            timestampSeconds = 1_782_043_200L,
            timezoneOffsetSeconds = 0L,
            cloudinessPercent = 0L
        )
        assertTrue("UV attendu > 3, obtenu $uv", uv > 3.0)
        assertTrue("UV attendu <= 11.5, obtenu $uv", uv <= 11.5)
    }

    @Test
    fun `clouds reduce uv`() {
        val clear = UvIndexEstimator.estimate(
            lat = lat, lon = lon,
            timestampSeconds = 1_782_043_200L,
            timezoneOffsetSeconds = 0L,
            cloudinessPercent = 0L
        )
        val overcast = UvIndexEstimator.estimate(
            lat = lat, lon = lon,
            timestampSeconds = 1_782_043_200L,
            timezoneOffsetSeconds = 0L,
            cloudinessPercent = 100L
        )
        assertTrue(overcast < clear)
    }
}
