package com.meteoapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meteoapp.city.FavoriteCitiesStore
import com.meteoapp.data.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation du store multi-villes sur un vrai appareil :
 * persistance réelle des SharedPreferences + Moshi sur l'appareil.
 */
@RunWith(AndroidJUnit4::class)
class FavoriteCitiesStoreInstrumentedTest {

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun favorisPersistEntreDeuxLectures() {
        val ctx = context()
        FavoriteCitiesStore.setFavorites(ctx, emptyList())
        val paris = GeoLocation("Paris", null, 48.85, 2.35, "FR", null)
        FavoriteCitiesStore.addFavorite(ctx, paris)
        FavoriteCitiesStore.addFavorite(ctx, paris)
        assertEquals(1, FavoriteCitiesStore.getFavorites(ctx).size)
        assertTrue(FavoriteCitiesStore.isFavorite(ctx, paris))
        FavoriteCitiesStore.removeFavorite(ctx, paris)
        assertTrue(FavoriteCitiesStore.getFavorites(ctx).isEmpty())
    }

    @Test
    fun villeCouranteUtiliseePourLaNotification() {
        val ctx = context()
        val lyon = GeoLocation("Lyon", null, 45.75, 4.85, "FR", null)
        FavoriteCitiesStore.setCurrentCity(ctx, lyon)
        assertEquals("Lyon", FavoriteCitiesStore.currentNotificationCity(ctx)?.name)
    }
}
