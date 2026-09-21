package com.meteoapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.meteoapp.R
import com.meteoapp.city.CitySearchController
import com.meteoapp.data.WeatherRepository
import com.meteoapp.databinding.DialogSearchBinding

/**
 * Écran de configuration du widget : la recherche de ville est déléguée à
 * [CitySearchController] (partagée avec la boîte de dialogue du tableau de
 * bord) ; l'écran ajoute uniquement le choix du thème du widget et la
 * confirmation du résultat vers AppWidgetManager.
 */
class WeatherWidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: DialogSearchBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetTheme: WidgetThemePrefs.Theme = WidgetThemePrefs.Theme.WEATHER_GRADIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        binding = DialogSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.widget_title)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        CitySearchController(
            binding = binding,
            lifecycleOwner = this,
            repository = WeatherRepository(this),
            onCitySelected = { city -> onCityChosen(city) }
        )

        binding.cityInputLayout.requestFocus()

        binding.widgetThemeLabel.visibility = View.VISIBLE
        binding.widgetThemeGroup.visibility = View.VISIBLE
        binding.widgetThemeGroup.setOnCheckedChangeListener { _, checkedId ->
            widgetTheme = if (checkedId == R.id.themeDarkRadio) {
                WidgetThemePrefs.Theme.DARK
            } else {
                WidgetThemePrefs.Theme.WEATHER_GRADIENT
            }
        }
    }

    private fun onCityChosen(city: com.meteoapp.data.model.GeoLocation) {
        WidgetPrefs.saveCity(this, appWidgetId, city)
        WidgetThemePrefs.setTheme(this, appWidgetId, widgetTheme)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()

        WeatherWidgetProvider.triggerUpdate(this)
    }
}
