package com.meteoapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.meteoapp.R
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.WeatherData
import com.meteoapp.databinding.ActivityMainBinding
import com.meteoapp.ui.adapter.DailyAdapter
import com.meteoapp.ui.adapter.HourlyAdapter
import com.meteoapp.util.WeatherUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            viewModel.loadForCurrentLocation()
        } else {
            showError(getString(R.string.permission_required))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.meteoToolbar))

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { retryLast() }

        viewModel.state.observe(this) { state -> render(state) }

        if (!viewModel.isApiKeyConfigured) {
            showError(getString(R.string.error_no_api_key))
        } else {
            requestLocationAndLoad()
        }
    }

    private var hasRequestedOnce = false
    private var triedLocation = false

    private fun requestLocationAndLoad() {
        if (hasLocationPermission()) {
            triedLocation = true
            viewModel.loadForCurrentLocation()
        } else if (!hasRequestedOnce) {
            hasRequestedOnce = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun retryLast() {
        if (viewModel.isApiKeyConfigured) {
            requestLocationAndLoad()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                SearchCityDialog { city -> viewModel.loadWeatherForCity(city) }
                    .show(supportFragmentManager, "search")
                true
            }
            R.id.action_locate -> {
                if (hasLocationPermission()) {
                    viewModel.loadForCurrentLocation()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun render(state: UiState) {
        binding.swipeRefresh.isRefreshing = state.refreshing
        binding.loadingBar.visibility = if (state.loading) View.VISIBLE else View.GONE

        if (state.error != null && state.weather == null) {
            showError(state.error)
            return
        }

        if (state.error != null && state.weather != null) {
            // Erreur de rafraîchissement : on garde l'affichage, snack simple
        }

        val weather = state.weather
        if (weather != null) {
            showContent(weather, state.city)
        }
    }

    private fun showContent(weather: WeatherData, city: GeoLocation?) {
        binding.errorLayout.visibility = View.GONE
        binding.loadingBar.visibility = View.GONE

        val displayName = city?.localNames?.fr
            ?: city?.name
            ?: ""
        binding.cityName.text = displayName
        binding.headerLayout.visibility = View.VISIBLE
        binding.detailsCard.visibility = View.VISIBLE
        binding.hourlyTitle.visibility = View.VISIBLE
        binding.dailyTitle.visibility = View.VISIBLE
        binding.hourlyRecycler.visibility = View.VISIBLE
        binding.dailyRecycler.visibility = View.VISIBLE

        val current = weather.current
        binding.temperature.text = "${WeatherUtils.roundToInt(current.temp)}°"
        binding.weatherDescription.text =
            current.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
        binding.feelsLike.text = getString(R.string.feels_like, WeatherUtils.roundToInt(current.feelsLike))

        val today = weather.daily.firstOrNull()
        if (today != null) {
            binding.minMax.text = "Max ${WeatherUtils.roundToInt(today.tempMax)}°  Min ${WeatherUtils.roundToInt(today.tempMin)}°"
        } else {
            binding.minMax.text = ""
        }

        binding.humidityValue.text = "${current.humidity} %"
        binding.windValue.text = "${WeatherUtils.kmh(current.windSpeed)} km/h ${WeatherUtils.windDirection(current.windDeg)}"
        binding.pressureValue.text = "${current.pressure} hPa"
        binding.visibilityValue.text = "${(current.visibility ?: 0L) / 1000} km"

        current.sunrise?.let { binding.sunriseValue.text = WeatherUtils.formatTime(it, weather.timezoneOffset) }
        current.sunset?.let { binding.sunsetValue.text = WeatherUtils.formatTime(it, weather.timezoneOffset) }

        // Hourly : 24 prochaines heures
        binding.hourlyRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.hourlyRecycler.adapter = HourlyAdapter(this, weather.hourly.take(24))

        // Daily : 7 jours
        binding.dailyRecycler.layoutManager = LinearLayoutManager(this)
        binding.dailyRecycler.adapter = DailyAdapter(this, weather.daily.take(7))
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.loadingBar.visibility = View.GONE
        binding.headerLayout.visibility = View.GONE
        binding.detailsCard.visibility = View.GONE
        binding.hourlyTitle.visibility = View.GONE
        binding.dailyTitle.visibility = View.GONE
        binding.hourlyRecycler.visibility = View.GONE
        binding.dailyRecycler.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }
}
