package com.meteoapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meteoapp.util.ThemePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation du thème Material You sur appareil réel :
 * support des couleurs dynamiques selon l'API réelle de l'appareil.
 */
@RunWith(AndroidJUnit4::class)
class ThemePrefsInstrumentedTest {

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun modeSombreAppliqueAuDelegate() {
        val ctx = context()
        ThemePrefs.setMode(ctx, ThemePrefs.ThemeMode.DARK)
        ThemePrefs.applyMode(ctx)
        assertEquals(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode()
        )
        ThemePrefs.setMode(ctx, ThemePrefs.ThemeMode.FOLLOW_SYSTEM)
        ThemePrefs.applyMode(ctx)
    }

    @Test
    fun supportDynamiqueCoherentAvecLApi() {
        val ctx = context()
        val supported = ThemePrefs.isDynamicColorsSupported(ctx)
        assertEquals(android.os.Build.VERSION.SDK_INT >= 31, supported)
        ThemePrefs.setDynamicColorsEnabled(ctx, true)
        assertTrue(ThemePrefs.isDynamicColorsEnabled(ctx))
    }
}
