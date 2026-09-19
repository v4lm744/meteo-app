package com.meteoapp.ui

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.ApiKeyStore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lance MainActivity comme au démarrage réel de l'application (clé API
 * configurée) et vérifie que le tableau de bord se charge sans crash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class MainActivityLaunchTest {

    @Test
    fun activityLaunchesWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key-123")
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        controller.get().recreate()
        controller.destroy()
    }

    @Test
    fun activityLaunchesWithWidgetExtras() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key-123")
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CITY_NAME, "Paris")
            putExtra(MainActivity.EXTRA_CITY_LAT, 48.85)
            putExtra(MainActivity.EXTRA_CITY_LON, 2.35)
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        controller.setup()
        controller.destroy()
    }
}
