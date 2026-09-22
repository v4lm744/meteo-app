package com.meteoapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.meteoapp.R
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.model.WeatherData
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.databinding.ActivityComparisonBinding
import com.meteoapp.databinding.ItemComparisonCityBinding
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Écran « Comparaison des villes » : météo des villes favorites côte à
 * côte sous forme de colonnes défilables horizontalement, pour choisir
 * une destination d'un seul coup d'œil. Un appui ouvre la ville dans le
 * tableau de bord.
 */
class CityComparisonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComparisonBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComparisonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.comparisonToolbar.setNavigationOnClickListener { finish() }

        val favorites = com.meteoapp.city.FavoriteCitiesStore.getFavorites(this)
        if (favorites.isEmpty()) {
            binding.comparisonEmpty.visibility = View.VISIBLE
            binding.comparisonProgress.visibility = View.GONE
            binding.columnsLayout.visibility = View.GONE
            return
        }

        val repository = com.meteoapp.data.ServiceLocator.weatherRepository(this)
        lifecycleScope.launch {
            val weathers = withContext(Dispatchers.IO) {
                favorites.map { city ->
                    async {
                        runCatching {
                            (repository.getWeather(city.lat, city.lon) as? WeatherResult.Success)?.data
                        }.getOrNull()
                    }
                }.awaitAll()
            }
            renderColumns(favorites, weathers)
            binding.comparisonProgress.visibility = View.GONE
            binding.columnsLayout.visibility = View.VISIBLE
        }
    }

    private fun renderColumns(cities: List<GeoLocation>, weathers: List<WeatherData?>) {
        binding.columnsLayout.removeAllViews()
        for ((index, city) in cities.withIndex()) {
            val itemBinding = ItemComparisonCityBinding.inflate(
                layoutInflater, binding.columnsLayout, false
            )
            itemBinding.comparisonCityName.text = city.displayName(this)

            val weather = weathers.getOrNull(index)
            if (weather != null) {
                itemBinding.comparisonTemp.text =
                    WeatherUtils.formatTemp(this, weather.current.temp)
                itemBinding.comparisonDesc.text =
                    weather.current.weather.firstOrNull()?.description
                        ?.replaceFirstChar { it.uppercase() } ?: ""
                weather.current.weather.firstOrNull()?.let { cond ->
                    WeatherIcons.bind(
                        itemBinding.comparisonIcon,
                        cond.id,
                        isDay = !cond.icon.endsWith("n")
                    )
                }
                val today = weather.daily.firstOrNull()
                itemBinding.comparisonMinMax.text = if (today != null) {
                    getString(
                        R.string.min_max_format,
                        WeatherUtils.formatTemp(this, today.tempMax),
                        WeatherUtils.formatTemp(this, today.tempMin)
                    )
                } else {
                    ""
                }
                itemBinding.comparisonState.visibility = View.GONE
            } else {
                itemBinding.comparisonTemp.text = ""
                itemBinding.comparisonDesc.text = ""
                itemBinding.comparisonMinMax.text = ""
                itemBinding.comparisonState.text = getString(R.string.comparison_unavailable)
                itemBinding.comparisonState.visibility = View.VISIBLE
            }

            itemBinding.root.setOnClickListener { openInDashboard(city) }
            binding.columnsLayout.addView(itemBinding.root)
        }
    }

    private fun openInDashboard(city: GeoLocation) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CITY_NAME, city.name)
            putExtra(MainActivity.EXTRA_CITY_LAT, city.lat)
            putExtra(MainActivity.EXTRA_CITY_LON, city.lon)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
