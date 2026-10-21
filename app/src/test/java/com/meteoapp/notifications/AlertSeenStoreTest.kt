package com.meteoapp.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BUG-006 : l'anti-spam des alertes était indexé par type seulement ; une
 * alerte vue pour une ville masquait la même alerte pour une autre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AlertSeenStoreTest {

    private lateinit var context: Context
    private val now = 1_782_000_000_000L
    private val rain = listOf(WeatherAlertEvaluator.Alert(key = "rain", message = "pluie"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        prefs().edit().clear().commit()
    }

    private fun prefs() = context.getSharedPreferences("meteo_alert_seen", Context.MODE_PRIVATE)

    @Test
    fun sameAlertForSameCityIsSilencedWithinWindow() {
        assertEquals(rain, AlertSeenStore.filterUnseen(context, 48.85, 2.35, rain, now))
        assertTrue(AlertSeenStore.filterUnseen(context, 48.85, 2.35, rain, now + 60_000L).isEmpty())
    }

    @Test
    fun sameAlertForAnotherCityIsNotSilenced() {
        AlertSeenStore.filterUnseen(context, 48.85, 2.35, rain, now)
        assertEquals(rain, AlertSeenStore.filterUnseen(context, 45.76, 4.84, rain, now + 60_000L))
    }

    @Test
    fun alertIsNotifiedAgainAfterWindow() {
        AlertSeenStore.filterUnseen(context, 48.85, 2.35, rain, now)
        val later = now + AlertSeenStore.SEEN_WINDOW_MS
        assertEquals(rain, AlertSeenStore.filterUnseen(context, 48.85, 2.35, rain, later))
    }

    @Test
    fun expiredEntriesAndLegacyKeysArePurged() {
        prefs().edit().putLong("rain", now - AlertSeenStore.SEEN_WINDOW_MS).commit()
        AlertSeenStore.filterUnseen(context, 48.85, 2.35, emptyList(), now)
        assertFalse(prefs().contains("rain"))
    }
}
