package com.meteoapp.ui

import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.city.FavoriteCitiesStore
import com.meteoapp.data.ApiKeyStore
import com.meteoapp.data.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gestion multi-villes dans le tableau de bord : les puces de favoris
 * apparaissent dès qu'une ville est favorite et l'étoile du menu bascule.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class CityChipsTest {

    @Test
    fun `chips invisibles sans favoris puis visibles apres ajout`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key-123")
        FavoriteCitiesStore.setFavorites(context, emptyList())
        FavoriteCitiesStore.setCurrentCity(
            context,
            GeoLocation("Paris", null, 48.85, 2.35, "FR", null)
        )

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks()

        // Sans favori, la rangée est masquée.
        assertEquals(View.GONE, activity.bindingCityChipsVisibility())

        FavoriteCitiesStore.addFavorite(
            context,
            GeoLocation("Lyon", null, 45.75, 4.85, "FR", null)
        )
        activity.refreshCityChipsPublic()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks()
        assertTrue(activity.bindingCityChipsVisibility() == View.VISIBLE)
        controller.destroy()
    }
}
