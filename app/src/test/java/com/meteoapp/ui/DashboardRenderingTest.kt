package com.meteoapp.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.graphics.Canvas
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.meteoapp.util.WeatherIcons
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproduit le chargement du tableau de bord : inflation du layout principal
 * et rendu de tous les composants (icônes animées, arc solaire, barres de
 * température, RecyclerView hourly/daily).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class DashboardRenderingTest {

    private fun themedContext(): Context {
        val base: Context = ApplicationProvider.getApplicationContext()
        return ContextThemeWrapper(base, com.meteoapp.R.style.Theme_MeteoApp)
    }

    @Test
    fun mainLayoutInflatesAndAllWeatherIconsLoad() {
        val context = themedContext()
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(com.meteoapp.R.layout.activity_main, null, false)
        root.measure(1080, 2280)
        root.layout(0, 0, 1080, 2280)
        root.draw(Canvas())
    }

    @Test
    fun allWeatherIconDrawablesLoadAndDraw() {
        val context: Context = themedContext()
        val imageView = ImageView(context)
        imageView.measure(192, 192)
        imageView.layout(0, 0, 192, 192)
        val codes = listOf(800L, 801L, 802L, 500L, 600L, 200L, 701L, 999L)
        for (code in codes) {
            for (isDay in listOf(true, false)) {
                WeatherIcons.bind(imageView, code, isDay)
                imageView.draw(Canvas())
            }
        }
    }

    @Test
    fun itemDailyAndHourlyInflateAndDraw() {
        val context = themedContext()
        val inflater = LayoutInflater.from(context)
        val daily = inflater.inflate(com.meteoapp.R.layout.item_daily, null, false)
        daily.measure(1080, 200)
        daily.layout(0, 0, 1080, 200)
        daily.draw(Canvas())
        val hourly = inflater.inflate(com.meteoapp.R.layout.item_hourly, null, false)
        hourly.measure(300, 400)
        hourly.layout(0, 0, 300, 400)
        hourly.draw(Canvas())
    }

    @Test
    fun weatherIconsForAllKnownConditionsResolve() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val known = listOf(
            200L, 232L, 300L, 321L, 500L, 501L, 502L, 531L,
            600L, 622L, 701L, 781L, 800L, 801L, 802L, 804L
        )
        for (code in known) {
            val name = WeatherIcons.nameFor(code, isDay = true)
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            check(id != 0) { "Icône manquante pour le code $code : $name" }
        }
    }

    @Test
    fun unknownConditionFallsBackToValidIcon() {
        val context: Context = themedContext()
        val id = WeatherIcons.forCondition(context, 999L, isDay = false)
        check(id != 0) { "Aucune icône de repli pour une condition inconnue" }
    }

    /**
     * Non-régression du crash de la v1.6 : animer un <path> avec translateY
     * lève IllegalArgumentException (« Property: translateY is not supported
     * for FullPath ») au start() de l'AnimatedVectorDrawable, ce qui faisait
     * planter l'app au chargement du tableau de bord. Toutes les icônes de
     * toutes les conditions doivent démarrer sans exception.
     */
    @Test
    fun allWeatherIconsStartAnimationWithoutCrash() {
        val context: Context = themedContext()
        val imageView = ImageView(context)
        imageView.measure(96, 96)
        imageView.layout(0, 0, 96, 96)
        val allCodes = listOf(
            200L, 232L, 300L, 321L, 500L, 501L, 502L, 531L,
            600L, 622L, 701L, 781L, 800L, 801L, 802L, 804L, 999L
        )
        for (code in allCodes) {
            for (isDay in listOf(true, false)) {
                WeatherIcons.bind(imageView, code, isDay)
                imageView.draw(Canvas())
            }
        }
    }
}
