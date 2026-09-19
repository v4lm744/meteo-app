package com.meteoapp.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.data.ApiKeyStore
import com.meteoapp.data.model.CurrentData
import com.meteoapp.data.model.DailyData
import com.meteoapp.data.model.HourlyData
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherCondition
import com.meteoapp.data.model.WeatherData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproduit le chargement complet du tableau de bord avec des données de
 * météo réalistes : l'activité observe le WeatherViewModel et rend tout le
 * contenu (icône héro, cartes, arc solaire, minimap, listes hourly/daily).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class MainActivityRenderTest {

    private fun fullWeather(): WeatherData {
        val now = System.currentTimeMillis() / 1000L
        val clearDay = WeatherCondition(800L, "Clear", "ciel dégagé", "01d")
        val rain = WeatherCondition(501L, "Rain", "pluie modérée", "10n")
        val current = CurrentData(
            dt = now, sunrise = now - 3600L, sunset = now + 3600L,
            temp = 21.4, feelsLike = 20.1, pressure = 1013L, humidity = 55L,
            visibility = 10000L, windSpeed = 3.2, windDeg = 120L,
            weather = listOf(clearDay), timezoneOffset = 7200L
        )
        val hourly = (0 until 24).map {
            HourlyData(
                dt = now + it * 3600L, temp = 15.0 + it, feelsLike = 14.0 + it,
                humidity = 60L, windSpeed = 2.5, windDeg = 90L,
                weather = listOf(if (it % 2 == 0) clearDay else rain),
                pop = 0.2, timezoneOffset = 7200L, pressure = 1012L,
                cloudiness = 10L, visibility = 10000L
            )
        }
        val daily = (0 until 7).map {
            DailyData(
                dt = now + it * 86400L, sunrise = null, sunset = null,
                tempMin = 10.0 + it, tempMax = 20.0 + it,
                morningTemp = 11.0, dayTemp = 18.0, eveningTemp = 15.0,
                nightTemp = 9.0, feelsLikeDay = 17.0, pressure = 1013L,
                humidity = 60L, windSpeed = 3.0, windDeg = 100L,
                weather = listOf(clearDay), pop = 0.1, timezoneOffset = 7200L
            )
        }
        return WeatherData(
            lat = 48.85, lon = 2.35, timezone = "Paris", timezoneOffset = 7200L,
            current = current, hourly = hourly, daily = daily
        )
    }

    private fun pushState(activity: MainActivity, state: UiState) {
        val viewModel = ViewModelProvider(activity)[WeatherViewModel::class.java]
        val field = WeatherViewModel::class.java.getDeclaredField("_state")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val liveData = field.get(viewModel) as androidx.lifecycle.MutableLiveData<UiState>
        liveData.value = state
    }

    @Test
    fun rendersFullDashboardWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key-123")
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        val weather = fullWeather()
        val city = com.meteoapp.data.model.GeoLocation(
            name = "Paris", localNames = null, lat = 48.85, lon = 2.35,
            country = "FR", state = null
        )
        val regionCities = listOf(
            RegionCity(1L, "Boulogne", 48.83, 2.32, 20.0, "01d", "ciel dégagé"),
            RegionCity(2L, "Versailles", 48.80, 2.13, 19.0, "10n", "pluie")
        )
        pushState(
            activity,
            UiState(loading = false, weather = weather, city = city, regionCities = regionCities)
        )
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks()
        activity.recreate()
        pushState(
            activity,
            UiState(loading = false, weather = weather, city = city, regionCities = regionCities)
        )
        controller.destroy()
    }

    @Test
    fun rendersErrorStateWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        ApiKeyStore.setApiKey(context, "test-key-123")
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        pushState(activity, UiState(loading = false, error = "Erreur réseau"))
        org.robolectric.shadows.ShadowLooper.runUiThreadTasks()
        controller.destroy()
    }
}
