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
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import com.meteoapp.databinding.ActivityMainBinding
import com.meteoapp.ui.adapter.DailyAdapter
import com.meteoapp.ui.adapter.HourlyAdapter
import com.meteoapp.util.WeatherUtils
import com.meteoapp.util.EntranceAnimator
import com.meteoapp.util.WeatherIcons
import androidx.core.view.iterator

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CITY_NAME = "com.meteoapp.EXTRA_CITY_NAME"
        const val EXTRA_CITY_LAT = "com.meteoapp.EXTRA_CITY_LAT"
        const val EXTRA_CITY_LON = "com.meteoapp.EXTRA_CITY_LON"
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

    private var toolbarTint: Int = 0xFFFFFFFF.toInt()
    private lateinit var cityChipsAdapter: com.meteoapp.ui.adapter.CityChipsAdapter
    private lateinit var backgroundController: com.meteoapp.ui.background.DynamicBackgroundController
    private var displayedCityKey: String? = null

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

        // Plein écran : le fond dégradé remplit l'écran sous la barre d'état,
        // fitsSystemWindows sur la racine décale le contenu (toolbar) sous la barre.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.meteoToolbar)
        setSupportActionBar(toolbar)
        backgroundController = com.meteoapp.ui.background.DynamicBackgroundController(
            binding = binding,
            window = window,
            toolbar = toolbar,
            onTintChanged = { tint -> toolbarTint = tint }
        )

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { retryLast() }

        cityChipsAdapter = com.meteoapp.ui.adapter.CityChipsAdapter(
            onCityClick = { city -> viewModel.loadWeatherForCity(city) },
            onCityLongClick = { city -> removeFavoriteCity(city) }
        )
        binding.cityChips.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.cityChips.adapter = cityChipsAdapter

        binding.regionMapView.onCitySelected = { regionCity ->
            val city = com.meteoapp.data.model.GeoLocation(
                name = regionCity.name,
                localNames = null,
                lat = regionCity.lat,
                lon = regionCity.lon,
                country = null,
                state = null
            )
            viewModel.loadWeatherForCity(city)
        }

        viewModel.state.observe(this) { state -> render(state) }

        if (!viewModel.isApiKeyConfigured) {
            showError(getString(R.string.error_no_api_key))
            openApiKeyDialog()
        } else {
            loadFromLaunchIntent(intent)
        }
    }

    /**
     * Charge la ville portée par un intent de lancement (widget, raccourci
     * dynamique, écran de comparaison), sinon la dernière ville consultée,
     * sinon la position courante.
     */
    private fun loadFromLaunchIntent(intent: android.content.Intent?) {
        val launchName = intent?.getStringExtra(EXTRA_CITY_NAME)
        val launchLat = intent?.getDoubleExtra(EXTRA_CITY_LAT, Double.NaN) ?: Double.NaN
        val launchLon = intent?.getDoubleExtra(EXTRA_CITY_LON, Double.NaN) ?: Double.NaN
        if (!launchName.isNullOrBlank() && !launchLat.isNaN() && !launchLon.isNaN()) {
            viewModel.loadWeatherForCity(
                GeoLocation(
                    name = launchName,
                    localNames = null,
                    lat = launchLat,
                    lon = launchLon,
                    country = null,
                    state = null
                )
            )
        } else {
            val savedCity = com.meteoapp.city.FavoriteCitiesStore.getCurrentCity(this)
            if (savedCity != null) {
                viewModel.loadWeatherForCity(savedCity)
            } else {
                requestLocationAndLoad()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // singleTop : un tap widget/raccourci alors que l'activité est déjà
        // affichée arrive ici sans recréer l'activité ; on charge la ville demandée.
        if (viewModel.isApiKeyConfigured) {
            loadFromLaunchIntent(intent)
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
        if (!viewModel.isApiKeyConfigured) {
            openApiKeyDialog()
            return
        }
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
    }

    private fun openApiKeyDialog() {
        ApiKeyDialog {
            if (viewModel.isApiKeyConfigured) {
                binding.errorLayout.visibility = View.GONE
                requestLocationAndLoad()
            } else {
                showError(getString(R.string.error_no_api_key))
            }
        }.show(supportFragmentManager, "api_key")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val tint = toolbarTint
        for (item in menu) {
            item.icon?.mutate()?.setTint(tint)
        }
        val current = viewModel.state.value?.city
        if (current != null) {
            menu.findItem(R.id.action_favorite)?.setIcon(
                if (com.meteoapp.city.FavoriteCitiesStore.isFavorite(this, current))
                    R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )?.icon?.mutate()?.setTint(tint)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                SearchCityDialog { city -> viewModel.loadWeatherForCity(city) }
                    .show(supportFragmentManager, "search")
                true
            }
            R.id.action_favorite -> {
                toggleFavoriteCurrentCity()
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
            R.id.action_share -> {
                shareCurrentWeather()
                true
            }
            R.id.action_compare -> {
                startActivity(android.content.Intent(this, CityComparisonActivity::class.java))
                true
            }
            R.id.action_api_key -> {
                openApiKeyDialog()
                true
            }
            R.id.action_settings -> {
                SettingsDialog { onSettingsChanged() }
                    .show(supportFragmentManager, "settings")
                true
            }
            R.id.action_tutorial -> {
                TutorialDialog().show(supportFragmentManager, "tutorial")
                true
            }
            R.id.action_about -> {
                AboutDialog().show(supportFragmentManager, "about")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rerenderCurrentState() {
        viewModel.state.value?.let { render(it) }
    }

    /**
     * Après validation des Paramètres : le mode de thème peut avoir changé,
     * l'activité est recréée pour l'appliquer partout ; sinon simple re-rendu.
     */
    private fun onSettingsChanged() {
        val currentMode = com.meteoapp.util.ThemePrefs.getMode(this)
        val appliedMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val needsRecreate = when (currentMode) {
            com.meteoapp.util.ThemePrefs.ThemeMode.DARK ->
                appliedMode != android.content.res.Configuration.UI_MODE_NIGHT_YES
            com.meteoapp.util.ThemePrefs.ThemeMode.LIGHT ->
                appliedMode != android.content.res.Configuration.UI_MODE_NIGHT_NO
            else -> false
        }
        if (needsRecreate) {
            recreate()
        } else {
            rerenderCurrentState()
        }
    }

    private fun toggleFavoriteCurrentCity() {
        val city = viewModel.state.value?.city ?: return
        val store = com.meteoapp.city.FavoriteCitiesStore
        val displayName = city.displayName(this)
        if (store.isFavorite(this, city)) {
            store.removeFavorite(this, city)
            showSnackbar(getString(R.string.favorite_removed, displayName))
        } else {
            store.addFavorite(this, city)
            showSnackbar(getString(R.string.favorite_added, displayName))
        }
        refreshCityChips()
        invalidateOptionsMenu()
    }

    private fun removeFavoriteCity(city: com.meteoapp.data.model.GeoLocation) {
        com.meteoapp.city.FavoriteCitiesStore.removeFavorite(this, city)
        showSnackbar(
            getString(
                R.string.favorite_removed,
                city.displayName(this)
            )
        )
        refreshCityChips()
        invalidateOptionsMenu()
    }

    private fun refreshCityChips() {
        val favorites = com.meteoapp.city.FavoriteCitiesStore.getFavorites(this)
        val current = viewModel.state.value?.city
        cityChipsAdapter.submitList(favorites, current)
        binding.cityChips.visibility =
            if (favorites.isEmpty()) View.GONE else View.VISIBLE
        com.meteoapp.util.ShortcutsHelper.refresh(this)
    }

    @androidx.annotation.VisibleForTesting
    fun refreshCityChipsPublic() = refreshCityChips()

    @androidx.annotation.VisibleForTesting
    fun bindingCityChipsVisibility(): Int = binding.cityChips.visibility

    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.swipeRefresh,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
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
            showContent(weather, state.city, state.regionCities, state.airQuality, state.stale)
        }
    }

    private fun showContent(
        weather: WeatherData,
        city: GeoLocation?,
        regionCities: List<RegionCity>,
        airQuality: com.meteoapp.data.model.AirPollutionItem? = null,
        stale: Boolean = false
    ) {
        binding.errorLayout.visibility = View.GONE
        binding.loadingBar.visibility = View.GONE

        if (stale) {
            binding.cacheBanner.text =
                com.meteoapp.util.WeatherUtils.formatOfflineAge(this, weather.lat, weather.lon)
            binding.cacheBanner.visibility = View.VISIBLE
        } else {
            binding.cacheBanner.visibility = View.GONE
        }

        val displayName = city?.displayName(this) ?: ""
        binding.cityName.text = displayName
        refreshCityChips()
        binding.headerLayout.visibility = View.VISIBLE
        binding.detailsCard.visibility = View.VISIBLE
        binding.regionMapTitle.visibility = View.VISIBLE
        binding.regionMapCard.visibility = View.VISIBLE
        binding.hourlyTitle.visibility = View.VISIBLE
        binding.dailyTitle.visibility = View.VISIBLE
        binding.hourlyRecycler.visibility = View.VISIBLE
        binding.dailyRecycler.visibility = View.VISIBLE

        val current = weather.current
        val cond = current.weather.firstOrNull()
        binding.swipeRefresh.setWeatherCondition(
            cond?.id ?: 800L,
            cond?.icon?.endsWith("n") == false
        )
        com.meteoapp.util.WeatherTransition.animateTemperature(
            binding.temperature,
            { value -> WeatherUtils.formatTemp(this, value) },
            current.temp
        )
        current.weather.firstOrNull()?.let { cond ->
            WeatherIcons.bind(
                binding.heroIcon,
                cond.id,
                isDay = !cond.icon.endsWith("n")
            )
        }
        binding.weatherDescription.text =
            current.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
        binding.feelsLike.text = getString(R.string.feels_like_value, WeatherUtils.formatTemp(this, current.feelsLike))

        val today = weather.daily.firstOrNull()
        if (today != null) {
            binding.minMax.text = getString(
                R.string.min_max_format,
                WeatherUtils.formatTemp(this, today.tempMax),
                WeatherUtils.formatTemp(this, today.tempMin)
            )
        } else {
            binding.minMax.text = ""
        }

        binding.humidityValue.text = getString(R.string.format_percent, current.humidity)
        binding.windValue.text = getString(
            R.string.format_wind,
            WeatherUtils.formatWindSpeed(this, current.windSpeed),
            WeatherUtils.windDirection(current.windDeg)
        )
        binding.pressureValue.text = WeatherUtils.formatPressure(this, current.pressure)
        binding.visibilityValue.text = WeatherUtils.formatVisibility(this, current.visibility)

        current.sunrise?.let { binding.sunriseValue.text = WeatherUtils.formatTime(this, it, weather.timezoneOffset) }
        current.sunset?.let { binding.sunsetValue.text = WeatherUtils.formatTime(this, it, weather.timezoneOffset) }

        val sunrise = current.sunrise
        val sunset = current.sunset
        if (sunrise != null && sunset != null) {
            binding.arcSunriseValue.text = WeatherUtils.formatTime(this, sunrise, weather.timezoneOffset)
            binding.arcSunsetValue.text = WeatherUtils.formatTime(this, sunset, weather.timezoneOffset)
            binding.sunArcView.setSunTimes(sunrise, sunset, current.dt)
            binding.sunArcCard.visibility = View.VISIBLE
        } else {
            binding.sunArcCard.visibility = View.GONE
        }

        showAirQuality(weather, airQuality)
        showHistory(weather)

        // Hourly : 48 prochaines heures (pas de 3 h)
        val isCityChange = displayedCityKey != cityKeyFor(city ?: viewModel.state.value?.city)
        if (isCityChange) {
            binding.hourlyRecycler.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.hourlyRecycler.adapter = HourlyAdapter(this, weather.hourly) { hour ->
                openHourDetail(hour)
            }

            // Daily : 7 jours
            binding.dailyRecycler.layoutManager = LinearLayoutManager(this)
            binding.dailyRecycler.adapter = DailyAdapter(this, weather.daily.take(7), current.temp) { day ->
                openDayDetail(day)
            }
        } else {
            (binding.hourlyRecycler.adapter as? HourlyAdapter)?.submitList(weather.hourly)
            (binding.dailyRecycler.adapter as? DailyAdapter)?.submitItems(weather.daily.take(7), current.temp)
        }

        // Minimap région : villes proches avec météo
        if (regionCities.isNotEmpty()) {
            binding.regionMapView.showCities(regionCities, weather.lat, weather.lon)
        } else {
            binding.regionMapView.clear()
        }

        applyDynamicBackground(weather)
        if (isCityChange) {
            displayedCityKey = cityKeyFor(city ?: viewModel.state.value?.city)
            EntranceAnimator.cascade(
                binding.headerLayout,
                binding.detailsCard,
                binding.airQualityCard.root,
                binding.sunArcCard,
                binding.historyCard.root,
                binding.regionMapTitle,
                binding.regionMapCard,
                binding.hourlyTitle,
                binding.hourlyRecycler,
                binding.dailyTitle,
                binding.dailyRecycler
            )
        }
    }

    private fun cityKeyFor(city: GeoLocation?): String? =
        city?.let { "%.2f_%.2f".format(java.util.Locale.US, it.lat, it.lon) }

    private fun showAirQuality(weather: WeatherData, airQuality: com.meteoapp.data.model.AirPollutionItem?) {
        val aqi = airQuality?.main?.aqi
        if (aqi != null) {
            binding.airQualityCard.airQualityValue.text = com.meteoapp.util.AirQualityUtils.label(this, aqi)
            binding.airQualityCard.airQualityPm.text = airQuality.components?.let {
                getString(
                    R.string.pollutant_unit_ugm3,
                    com.meteoapp.util.AirQualityUtils.formatPm25(it.pm25)
                )
            } ?: ""
            binding.airQualityCard.airQualityAdvice.text = com.meteoapp.util.AirQualityUtils.recommendation(this, aqi)
        } else {
            binding.airQualityCard.airQualityValue.text = getString(R.string.comparison_unavailable)
            binding.airQualityCard.airQualityPm.text = ""
            binding.airQualityCard.airQualityAdvice.text = ""
        }

        val uv = com.meteoapp.util.UvIndexEstimator.estimate(
            weather.lat,
            weather.lon,
            weather.current.dt,
            weather.timezoneOffset,
            weather.current.cloudiness
        )
        binding.airQualityCard.uvIndexValue.text = String.format(java.util.Locale.getDefault(), "%.1f", uv)
        binding.airQualityCard.uvIndexLabel.text = com.meteoapp.util.UvIndexEstimator.label(this, uv)
        binding.airQualityCard.uvAdvice.text = com.meteoapp.util.UvIndexEstimator.recommendation(this, uv)
        binding.airQualityCard.root.visibility = View.VISIBLE
    }

    private fun showHistory(weather: WeatherData) {
        val store = com.meteoapp.stats.WeatherHistoryStore
        val records = store.last7Days(this, weather.lat, weather.lon)
        if (records.size < 2) {
            binding.historyCard.historyChart.submit(emptyList())
            binding.historyCard.historyRecords.text = getString(R.string.history_empty)
            binding.historyCard.historyWeekCompare.visibility = View.GONE
            binding.historyCard.root.visibility = View.VISIBLE
            return
        }
        binding.historyCard.historyChart.submit(records)

        val recordsText = buildString {
            store.allTimeMin(this@MainActivity, weather.lat, weather.lon)?.let {
                append(
                    getString(
                        R.string.history_record_min,
                        WeatherUtils.formatTemp(this@MainActivity, it.tempMin),
                        WeatherUtils.formatDate(it.dayKey, 0)
                    )
                )
                append('\n')
            }
            store.allTimeMax(this@MainActivity, weather.lat, weather.lon)?.let {
                append(
                    getString(
                        R.string.history_record_max,
                        WeatherUtils.formatTemp(this@MainActivity, it.tempMax),
                        WeatherUtils.formatDate(it.dayKey, 0)
                    )
                )
            }
        }
        binding.historyCard.historyRecords.text = recordsText.trim()

        store.weekComparison(this, weather.lat, weather.lon)?.let { (current, previous) ->
            val delta = String.format(java.util.Locale.getDefault(), "%.1f°C", kotlin.math.abs(current - previous))
            binding.historyCard.historyWeekCompare.text = getString(
                if (current >= previous) R.string.history_week_compare_warmer
                else R.string.history_week_compare_cooler,
                delta
            )
            binding.historyCard.historyWeekCompare.visibility = View.VISIBLE
        } ?: run {
            binding.historyCard.historyWeekCompare.visibility = View.GONE
        }
        binding.historyCard.root.visibility = View.VISIBLE
    }

    private fun shareCurrentWeather() {
        val state = viewModel.state.value ?: return
        val weather = state.weather ?: return
        val cityLabel = state.city?.displayName(this) ?: weather.timezone
        val cond = weather.current.weather.firstOrNull()?.description ?: ""
        val shareText = getString(
            R.string.share_text,
            WeatherUtils.formatTemp(this, weather.current.temp),
            cityLabel,
            cond.replaceFirstChar { it.uppercase() },
            WeatherUtils.formatTemp(this, weather.current.feelsLike),
            WeatherUtils.formatTemp(this, weather.daily.firstOrNull()?.tempMax ?: weather.current.temp),
            WeatherUtils.formatTemp(this, weather.daily.firstOrNull()?.tempMin ?: weather.current.temp),
            getString(R.string.app_full_name)
        )
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        startActivity(
            android.content.Intent.createChooser(sendIntent, getString(R.string.share_via))
        )
    }

    private fun applyDynamicBackground(weather: WeatherData) {
        val stop = com.meteoapp.util.SkyGradient.stopFor(weather)
        backgroundController.apply(stop.top, stop.bottom)
        binding.swipeRefresh.setColorSchemeColors(stop.top)
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.loadingBar.visibility = View.GONE
        backgroundController.reset()
        binding.root.setBackgroundResource(R.drawable.bg_sky_gradient)
        com.meteoapp.util.SystemBars.setStatusBarColorCompat(window, ContextCompat.getColor(this, R.color.md_blue_deep))
        binding.swipeRefresh.setWeatherCondition(800L, isDay = true)
        backgroundController.tintToolbar(ContextCompat.getColor(this, R.color.md_blue_deep))
        binding.headerLayout.visibility = View.GONE
        binding.cacheBanner.visibility = View.GONE
        binding.detailsCard.visibility = View.GONE
        binding.airQualityCard.root.visibility = View.GONE
        binding.sunArcCard.visibility = View.GONE
        binding.historyCard.root.visibility = View.GONE
        binding.regionMapTitle.visibility = View.GONE
        binding.regionMapCard.visibility = View.GONE
        binding.hourlyTitle.visibility = View.GONE
        binding.dailyTitle.visibility = View.GONE
        binding.hourlyRecycler.visibility = View.GONE
        binding.dailyRecycler.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    private fun openDayDetail(day: com.meteoapp.data.model.DailyData) {
        val json = com.meteoapp.data.api.ApiClient.moshi
            .adapter(com.meteoapp.data.model.DailyData::class.java)
            .toJson(day)
        val intent = android.content.Intent(this, DayDetailActivity::class.java).apply {
            putExtra(DayDetailActivity.EXTRA_DAILY_JSON, json)
        }
        startActivity(intent)
    }

    private fun openHourDetail(hour: com.meteoapp.data.model.HourlyData) {
        val json = com.meteoapp.data.api.ApiClient.moshi
            .adapter(com.meteoapp.data.model.HourlyData::class.java)
            .toJson(hour)
        val intent = android.content.Intent(this, HourDetailActivity::class.java).apply {
            putExtra(HourDetailActivity.EXTRA_HOURLY_JSON, json)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        binding.regionMapView.onResume()
        viewModel.state.value?.weather?.let { applyDynamicBackground(it) }
    }

    override fun onPause() {
        binding.regionMapView.onPause()
        super.onPause()
    }
}
