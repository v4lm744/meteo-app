package com.meteoapp

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meteoapp.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'instrumentation du lancement : l'application se lance sur un
 * appareil/émulateur réel et MainActivity s'affiche sans crash.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLaunchInstrumentedTest {

    @Test
    fun packageNameEstMeteoApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.meteoapp", context.packageName)
    }

    @Test
    fun mainActivitySeLanceSansCrash() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assert(!activity.isFinishing)
            }
        }
    }
}
