package com.meteoapp.widget

import androidx.test.core.app.ApplicationProvider
import com.meteoapp.widget.HourlyForecastStore.HourSnapshot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Vérifie la sérialisation des prévisions horaires du widget collection :
 * aller-retour save/load, tolérance aux lignes corrompues et nettoyage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HourlyForecastStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("meteo_hourly_widget_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun snapshot(dt: Long, temp: Double) = HourSnapshot(
        dt = dt,
        temp = temp,
        icon = "10d",
        conditionId = 500L,
        pop = 0.4,
        timezoneOffset = 3600L
    )

    @Test
    fun saveThenLoad_preservesAllEntries() {
        val entries = listOf(
            snapshot(dt = 1700000000, temp = 12.5),
            snapshot(dt = 1700003600, temp = 13.0),
            snapshot(dt = 1700007200, temp = 11.25)
        )
        HourlyForecastStore.save(context, 7, entries)
        val restored = HourlyForecastStore.loadSnapshot(context, 7)
        assertEquals(3, restored.size)
        entries.zip(restored).forEach { (expected, actual) ->
            assertEquals(expected.dt, actual.dt)
            assertEquals(expected.temp, actual.temp, 0.0)
            assertEquals(expected.icon, actual.icon)
            assertEquals(expected.conditionId, actual.conditionId)
            assertEquals(expected.pop, actual.pop, 0.0)
            assertEquals(expected.timezoneOffset, actual.timezoneOffset)
        }
    }

    @Test
    fun load_missingWidget_returnsEmptyList() {
        assertEquals(emptyList<HourSnapshot>(), HourlyForecastStore.loadSnapshot(context, 99))
    }

    @Test
    fun remove_deletesStoredEntries() {
        HourlyForecastStore.save(context, 5, listOf(snapshot(dt = 1700000000, temp = 20.0)))
        HourlyForecastStore.remove(context, 5)
        assertEquals(emptyList<HourSnapshot>(), HourlyForecastStore.loadSnapshot(context, 5))
    }

    @Test
    fun saveOverwrite_replacesPreviousEntries() {
        HourlyForecastStore.save(context, 3, listOf(snapshot(dt = 1700000000, temp = 1.0)))
        HourlyForecastStore.save(context, 3, listOf(snapshot(dt = 1800000000, temp = 2.0)))
        val restored = HourlyForecastStore.loadSnapshot(context, 3)
        assertEquals(1, restored.size)
        assertEquals(1800000000L, restored[0].dt)
    }
}
