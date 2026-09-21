package com.meteoapp.widget

import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("meteo_widget_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun saveCity_persistsExactDoubleCoordinates() {
        val city = GeoLocation(
            name = "Paris",
            localNames = null,
            lat = 48.8566145,
            lon = 2.3522219,
            country = "FR",
            state = null
        )
        WidgetPrefs.saveCity(context, 42, city)
        val restored = WidgetPrefs.getCity(context, 42)
        assertEquals(48.8566145, restored?.lat ?: 0.0, 0.0)
        assertEquals(2.3522219, restored?.lon ?: 0.0, 0.0)
        assertEquals("Paris", restored?.name)
    }

    @Test
    fun legacyFloatStorage_stillReadable() {
        context.getSharedPreferences("meteo_widget_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("name_7", "Lyon")
            .putFloat("lat_7", 45.75f)
            .putFloat("lon_7", 4.85f)
            .apply()
        val restored = WidgetPrefs.getCity(context, 7)
        // Un Float hérité conserve sa précision d'origine (~1e-6), le repli
        // ne doit ni planter ni perdre davantage de précision.
        assertEquals(45.75, restored?.lat ?: 0.0, 1e-5)
        assertEquals(4.85, restored?.lon ?: 0.0, 1e-5)
    }

    @Test
    fun remove_clearsStoredCity() {
        val city = GeoLocation(
            name = "Nice",
            localNames = null,
            lat = 43.7101729,
            lon = 7.2610932,
            country = "FR",
            state = null
        )
        WidgetPrefs.saveCity(context, 9, city)
        WidgetPrefs.remove(context, 9)
        assertNull(WidgetPrefs.getCity(context, 9))
    }
}
