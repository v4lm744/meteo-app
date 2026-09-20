package com.meteoapp.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.WeatherCondition
import com.meteoapp.data.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mode hors-ligne : libellé de la bannière selon l'âge du cache
 * (récent, quelques minutes, plusieurs heures, aucun cache).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class OfflineBannerTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun weather(lat: Double = 48.85, lon: Double = 2.35) = WeatherData(
        lat = lat, lon = lon, timezone = "UTC", timezoneOffset = 0L,
        current = CurrentData(
            dt = 0L, sunrise = null, sunset = null, temp = 20.0,
            feelsLike = 19.0, pressure = 1013L, humidity = 50L,
            visibility = 10000L, windSpeed = 2.0, windDeg = 0L,
            weather = listOf(WeatherCondition(800L, "Clear", "ciel dégagé", "01d")),
            timezoneOffset = 0L
        ),
        hourly = emptyList(), daily = emptyList()
    )

    @Test
    fun `sans cache la banniere reste le message generique`() {
        val label = WeatherUtils.formatOfflineAge(context(), 12.34, 56.78)
        assertEquals(context().getString(com.meteoapp.R.string.cache_offline_banner), label)
    }

    @Test
    fun `cache recent affiche a l instant`() {
        val ctx = context()
        val cache = com.meteoapp.data.WeatherCache(ctx)
        cache.save(10.0, 20.0, weather(10.0, 20.0))
        val label = WeatherUtils.formatOfflineAge(ctx, 10.0, 20.0)
        assertEquals(
            ctx.getString(
                com.meteoapp.R.string.cache_offline_with_age,
                ctx.getString(com.meteoapp.R.string.offline_age_now)
            ),
            label
        )
    }

    @Test
    fun `libelle contient l age en minutes ou heures selon le cache`() {
        val ctx = context()
        val cache = com.meteoapp.data.WeatherCache(ctx)
        // Simule un cache ancien en écrivant puis vieillissant le fichier.
        cache.save(30.0, 40.0, weather(30.0, 40.0))
        val file = java.io.File(ctx.cacheDir, "weather_30.00_40.00.json")
        if (file.exists()) {
            file.setLastModified(System.currentTimeMillis() - 2 * 60_000L - 5_000L)
            val label = WeatherUtils.formatOfflineAge(ctx, 30.0, 40.0)
            assertTrue(label.contains("2"))
        }
    }
}
