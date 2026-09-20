package com.meteoapp.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Notifications météo : préférences persistées et calcul du délai jusqu'au
 * prochain créneau horaire quotidien.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class NotificationPrefsTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `active et desactive la notification`() {
        val ctx = context()
        NotificationPrefs.setEnabled(ctx, true)
        assertTrue(NotificationPrefs.isEnabled(ctx))
        NotificationPrefs.setEnabled(ctx, false)
        assertFalse(NotificationPrefs.isEnabled(ctx))
    }

    @Test
    fun `heure bornee entre 0 et 23`() {
        val ctx = context()
        NotificationPrefs.setHour(ctx, 9)
        assertEquals(9, NotificationPrefs.getHour(ctx))
        NotificationPrefs.setHour(ctx, 42)
        assertEquals(23, NotificationPrefs.getHour(ctx))
    }

    @Test
    fun `delai jusqu'a demain quand l'heure est passee`() {
        // Fixe « maintenant » à 10:00 locaux.
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delay = WeatherNotificationScheduler.delayUntilNextRun(8, now.timeInMillis)
        assertEquals(22L * 3600_000L, delay)
    }

    @Test
    fun `delai jusqu'a aujourd'hui quand l'heure est a venir`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delay = WeatherNotificationScheduler.delayUntilNextRun(8, now.timeInMillis)
        assertEquals(90L * 60_000L, delay)
    }
}
