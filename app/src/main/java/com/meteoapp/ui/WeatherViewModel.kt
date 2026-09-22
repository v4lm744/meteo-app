package com.meteoapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.meteoapp.R
import com.meteoapp.data.ServiceLocator
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.model.AirPollutionItem
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import com.meteoapp.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val weather: WeatherData? = null,
    val city: GeoLocation? = null,
    val regionCities: List<RegionCity> = emptyList(),
    val airQuality: AirPollutionItem? = null,
    val error: String? = null,
    val fromCache: Boolean = false,
    val stale: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.weatherRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /**
     * Vue LiveData pour l'activité : conserve le comportement lifecycle-aware
     * existant (observe) tout en rendant le flux interne atomique.
     */
    val stateLiveData: LiveData<UiState> = _state.asLiveData()

    private var weatherJob: Job? = null

    val isApiKeyConfigured: Boolean
        get() = repository.isApiKeyConfigured

    fun loadWeatherForCity(city: GeoLocation) {
        weatherJob?.cancel()
        com.meteoapp.city.FavoriteCitiesStore.setCurrentCity(getApplication(), city)
        weatherJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, city = city) }
            when (val result = repository.getWeather(city.lat, city.lon)) {
                is WeatherResult.Success -> {
                    recordHistory(result.data)
                    _state.update {
                        UiState(
                            loading = false,
                            weather = result.data,
                            city = city,
                            error = null,
                            fromCache = result.fromCache,
                            stale = result.stale
                        )
                    }
                    loadRegionCities(city.lat, city.lon)
                    loadAirQuality(city.lat, city.lon)
                }
                is WeatherResult.Error -> {
                    _state.update { it.copy(loading = false, error = result.message) }
                }
                WeatherResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        val current = _state.value
        val lat = current.weather?.lat ?: current.city?.lat ?: return
        val lon = current.weather?.lon ?: current.city?.lon ?: return
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            when (val result = repository.getWeather(lat, lon, forceRefresh = true)) {
                is WeatherResult.Success -> {
                    recordHistory(result.data)
                    _state.update {
                        it.copy(
                            refreshing = false,
                            weather = result.data,
                            error = null,
                            fromCache = result.fromCache,
                            stale = result.stale
                        )
                    }
                    loadRegionCities(lat, lon)
                    loadAirQuality(lat, lon)
                }
                is WeatherResult.Error -> {
                    _state.update { it.copy(refreshing = false, error = result.message) }
                }
                WeatherResult.Loading -> {}
            }
        }
    }

    private fun recordHistory(weather: WeatherData) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                com.meteoapp.stats.WeatherHistoryStore.record(getApplication(), weather)
            }
        }
    }

    private fun loadRegionCities(lat: Double, lon: Double) {
        viewModelScope.launch {
            when (val result = repository.getRegionCities(lat, lon)) {
                is WeatherResult.Success -> {
                    _state.update { it.copy(regionCities = result.data) }
                }
                else -> {}
            }
        }
    }

    private fun loadAirQuality(lat: Double, lon: Double) {
        viewModelScope.launch {
            when (val result = repository.getAirQuality(lat, lon)) {
                is WeatherResult.Success -> {
                    _state.update {
                        it.copy(airQuality = result.data.list.firstOrNull())
                    }
                }
                else -> {}
            }
        }
    }

    fun loadForCurrentLocation() {
        viewModelScope.launch {
            if (!locationHelper.hasLocationPermission()) {
                _state.update {
                    it.copy(
                        error = getApplication<Application>()
                            .getString(R.string.error_location_permission)
                    )
                }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = getApplication<Application>()
                            .getString(R.string.error_location_unavailable)
                    )
                }
                return@launch
            }
            val geoResult = repository.reverseGeocode(location.latitude, location.longitude)
            val city = (geoResult as? WeatherResult.Success)?.data?.firstOrNull()
                ?: GeoLocation(
                    name = getApplication<Application>().getString(R.string.current_position),
                    localNames = null,
                    lat = location.latitude,
                    lon = location.longitude,
                    country = "",
                    state = null
                )
            com.meteoapp.city.FavoriteCitiesStore.setCurrentCity(getApplication(), city)
            when (val result = repository.getWeather(location.latitude, location.longitude)) {
                is WeatherResult.Success -> {
                    recordHistory(result.data)
                    _state.update {
                        UiState(
                            loading = false,
                            weather = result.data,
                            city = city,
                            error = null,
                            fromCache = result.fromCache,
                            stale = result.stale
                        )
                    }
                    loadRegionCities(location.latitude, location.longitude)
                    loadAirQuality(location.latitude, location.longitude)
                }
                is WeatherResult.Error -> {
                    _state.update { it.copy(loading = false, error = result.message) }
                }
                WeatherResult.Loading -> {}
            }
        }
    }
}
