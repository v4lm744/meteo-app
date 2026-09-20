package com.meteoapp.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Préférences de thème : persistance du mode (système/clair/sombre),
 * activation des couleurs dynamiques Material You.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ThemePrefsTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `mode par defaut suit le systeme`() {
        val ctx = context()
        ctx.getSharedPreferences("meteo_theme_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertEquals(ThemePrefs.ThemeMode.FOLLOW_SYSTEM, ThemePrefs.getMode(ctx))
    }

    @Test
    fun `mode clair et sombre persists`() {
        val ctx = context()
        ThemePrefs.setMode(ctx, ThemePrefs.ThemeMode.DARK)
        assertEquals(ThemePrefs.ThemeMode.DARK, ThemePrefs.getMode(ctx))
        ThemePrefs.setMode(ctx, ThemePrefs.ThemeMode.LIGHT)
        assertEquals(ThemePrefs.ThemeMode.LIGHT, ThemePrefs.getMode(ctx))
    }

    @Test
    fun `couleurs dynamiques persistees`() {
        val ctx = context()
        ThemePrefs.setDynamicColorsEnabled(ctx, true)
        assertTrue(ThemePrefs.isDynamicColorsEnabled(ctx))
        ThemePrefs.setDynamicColorsEnabled(ctx, false)
        assertEquals(false, ThemePrefs.isDynamicColorsEnabled(ctx))
    }

    @Test
    fun `applyMode configure le night mode du delegate`() {
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
}
