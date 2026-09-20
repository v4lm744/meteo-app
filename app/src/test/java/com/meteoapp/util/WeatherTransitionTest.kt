package com.meteoapp.util

import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Transition animée des valeurs météo : premier affichage direct,
 * puis comptage de l'ancienne vers la nouvelle température.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class WeatherTransitionTest {

    private fun format(value: Double): String = "${value.toInt()}°"

    @Test
    fun `premier affichage sans animation precedente`() {
        val view = TextView(ApplicationProvider.getApplicationContext())
        WeatherTransition.animateTemperature(view, ::format, 21.0)
        assertEquals("21°", view.text.toString())
    }

    @Test
    fun `nouvelle valeur identique affichée directement`() {
        val view = TextView(ApplicationProvider.getApplicationContext())
        WeatherTransition.animateTemperature(view, ::format, 21.0)
        WeatherTransition.animateTemperature(view, ::format, 21.0)
        assertEquals("21°", view.text.toString())
    }

    @Test
    fun `le compteur démarre depuis l'ancienne valeur`() {
        val view = TextView(ApplicationProvider.getApplicationContext())
        WeatherTransition.animateTemperature(view, ::format, 10.0)
        WeatherTransition.animateTemperature(view, ::format, 30.0)
        // L'animation est lancée ; le texte affiché reste entre 10 et 30.
        val displayed = view.text.toString().removeSuffix("°").toInt()
        assertTrue(displayed in 10..30)
    }
}
