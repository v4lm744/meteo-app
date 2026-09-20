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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AlertPrefsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("meteo_alert_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `disabled by default`() {
        assertFalse(AlertPrefs.isEnabled(context))
    }

    @Test
    fun `enable and disable`() {
        AlertPrefs.setEnabled(context, true)
        assertTrue(AlertPrefs.isEnabled(context))
        AlertPrefs.setEnabled(context, false)
        assertFalse(AlertPrefs.isEnabled(context))
    }

    @Test
    fun `default thresholds and custom values`() {
        assertEquals(AlertPrefs.DEFAULT_RAIN_MM, AlertPrefs.getRainThresholdMm(context), 0.001)
        assertEquals(AlertPrefs.DEFAULT_WIND_KMH, AlertPrefs.getWindThresholdKmh(context), 0.001)
        assertEquals(AlertPrefs.DEFAULT_FROST_TEMP, AlertPrefs.getFrostThresholdCelsius(context), 0.001)

        AlertPrefs.setRainThresholdMm(context, 5.5)
        AlertPrefs.setWindThresholdKmh(context, 90.0)
        AlertPrefs.setFrostThresholdCelsius(context, -2.5)

        assertEquals(5.5, AlertPrefs.getRainThresholdMm(context), 0.001)
        assertEquals(90.0, AlertPrefs.getWindThresholdKmh(context), 0.001)
        assertEquals(-2.5, AlertPrefs.getFrostThresholdCelsius(context), 0.001)
    }
}
