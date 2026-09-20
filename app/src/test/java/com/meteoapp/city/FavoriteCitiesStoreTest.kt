package com.meteoapp.city

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gestion multi-villes : ajout/retrait de favoris, persistance JSON et
 * ville courante utilisée par les notifications.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class FavoriteCitiesStoreTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun paris() = GeoLocation("Paris", null, 48.85, 2.35, "FR", null)
    private fun lyon() = GeoLocation("Lyon", null, 45.75, 4.85, "FR", null)

    @Test
    fun `ajoute et retire des favoris sans doublon`() {
        val ctx = context()
        FavoriteCitiesStore.setFavorites(ctx, emptyList())
        FavoriteCitiesStore.addFavorite(ctx, paris())
        FavoriteCitiesStore.addFavorite(ctx, paris())
        assertEquals(1, FavoriteCitiesStore.getFavorites(ctx).size)
        assertTrue(FavoriteCitiesStore.isFavorite(ctx, paris()))
        FavoriteCitiesStore.removeFavorite(ctx, paris())
        assertTrue(FavoriteCitiesStore.getFavorites(ctx).isEmpty())
        assertFalse(FavoriteCitiesStore.isFavorite(ctx, paris()))
    }

    @Test
    fun `ville courante persiste et sert de fallback notification`() {
        val ctx = context()
        FavoriteCitiesStore.setFavorites(ctx, listOf(lyon()))
        FavoriteCitiesStore.setCurrentCity(ctx, paris())
        assertEquals("Paris", FavoriteCitiesStore.getCurrentCity(ctx)?.name)
        assertEquals("Paris", FavoriteCitiesStore.currentNotificationCity(ctx)?.name)

        FavoriteCitiesStore.setCurrentCity(ctx, lyon())
        FavoriteCitiesStore.removeFavorite(ctx, lyon())
        // Plus de favoris ni de ville courante valide pour la notif ?
        // La ville courante reste utilisable même hors favoris.
        assertEquals("Lyon", FavoriteCitiesStore.currentNotificationCity(ctx)?.name)
    }

    @Test
    fun `fallback sur premiere favorite sans ville courante`() {
        val ctx = context()
        FavoriteCitiesStore.setFavorites(ctx, listOf(paris(), lyon()))
        // Réinitialise la ville courante : pas de setCurrentCity.
        ctx.getSharedPreferences("meteo_favorite_cities", Context.MODE_PRIVATE)
            .edit().remove("current_city_json").commit()
        val city = FavoriteCitiesStore.currentNotificationCity(ctx)
        assertEquals("Paris", city?.name)
    }

    @Test
    fun `sans favoris ni courante, aucune ville de notification`() {
        val ctx = context()
        FavoriteCitiesStore.setFavorites(ctx, emptyList())
        ctx.getSharedPreferences("meteo_favorite_cities", Context.MODE_PRIVATE)
            .edit().remove("current_city_json").commit()
        assertNull(FavoriteCitiesStore.currentNotificationCity(ctx))
    }
}
