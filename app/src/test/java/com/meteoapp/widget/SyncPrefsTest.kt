package com.meteoapp.widget

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
@Config(sdk = [33])
class SyncPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("meteo_sync_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun defaultInterval_is30Minutes() {
        assertEquals(30, SyncPrefs.getIntervalMinutes(context))
        assertTrue(SyncPrefs.isAutoSyncEnabled(context))
    }

    @Test
    fun setInterval_persistsValue() {
        SyncPrefs.setIntervalMinutes(context, 60)
        assertEquals(60, SyncPrefs.getIntervalMinutes(context))
    }

    @Test
    fun disabledValue_disablesAutoSync() {
        SyncPrefs.setIntervalMinutes(context, SyncPrefs.SYNC_DISABLED)
        assertEquals(SyncPrefs.SYNC_DISABLED, SyncPrefs.getIntervalMinutes(context))
        assertFalse(SyncPrefs.isAutoSyncEnabled(context))
    }

    @Test
    fun intervalOptions_areAllAtLeast15Minutes() {
        for (m in SyncPrefs.INTERVAL_OPTIONS_MINUTES) {
            assertTrue("option $m min doit respecter le minimum WorkManager (15)", m >= 15)
        }
    }
}
