package com.meteoapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.meteoapp.R
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.AirPollutionItem
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import com.meteoapp.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private val repository = WeatherRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> = _state

    private var weatherJob: Job? = null

    val isApiKeyConfigured: Boolean
        get() = repository.isApiKeyConfigured

    fun loadWeatherForCity(city: GeoLocation) {
        weatherJob?.cancel()
        com.meteoapp.city.FavoriteCitiesStore.setCurrentCity(getApplication(), city)
        weatherJob = viewModelScope.launch {
            _state.value = _state.value?.copy(loading = true, error = null, city = city)
            when (val result = repository.getWeather(city.lat, city.lon)) {
                is WeatherResult.Success -> {
                    recordHistory(result.data)
                    _state.value = UiState(
                        loading = false,
                        weather = result.data,
                        city = city,
                        error = null,
                        fromCache = result.fromCache,
                        stale = result.stale
                    )
                    loadRegionCities(city.lat, city.lon)
                    loadAirQuality(city.lat, city.lon)
                }
                is WeatherResult.Error -> {
                    _state.value = _state.value?.copy(
                        loading = false,
                        error = result.message
                    )
                }
                WeatherResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        val current = _state.value ?: return
        val lat = current.weather?.lat ?: current.city?.lat ?: return
        val lon = current.weather?.lon ?: current.city?.lon ?: return
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            _state.value = current.copy(refreshing = true, error = null)
            when (val result = repository.getWeather(lat, lon, forceRefresh = true)) {
                is WeatherResult.Success -> {
                    recordHistory(result.data)
                    _state.value = current.copy(
                        refreshing = false,
                        weather = result.data,
                        error = null,
                        fromCache = result.fromCache,
                        stale = result.stale
                    )
                    loadRegionCities(lat, lon)
                    loadAirQuality(lat, lon)
                }
                is WeatherResult.Error -> {
                    _state.value = current.copy(refreshing = false, error = result.message)
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
                    _state.value = _state.value?.copy(regionCities = result.data)
                }
                else -> {}
            }
        }
    }

    private fun loadAirQuality(lat: Double, lon: Double) {
        viewModelScope.launch {
            when (val result = repository.getAirQuality(lat, lon)) {
                is WeatherResult.Success -> {
                    _state.value = _state.value?.copy(
                        airQuality = result.data.list.firstOrNull()
                    )
                }
                else -> {}
            }
        }
    }

    fun loadForCurrentLocation() {
        viewModelScope.launch {
            if (!locationHelper.hasLocationPermission()) {
                _state.value = _state.value?.copy(
                    error = getApplication<Application>().getString(R.string.error_location_permission)
                )
                return@launch
            }
            _state.value = _state.value?.copy(loading = true, error = null)
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _state.value = _state.value?.copy(
                    loading = false,
                    error = getApplication<Application>().getString(R.string.error_location_unavailable)
                )
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
                    _state.value = UiState(
                        loading = false,
                        weather = result.data,
                        city = city,
                        error = null,
                        fromCache = result.fromCache,
                        stale = result.stale
                    )
                    loadRegionCities(location.latitude, location.longitude)
                    loadAirQuality(location.latitude, location.longitude)
                }
                is WeatherResult.Error -> {
                    _state.value = _state.value?.copy(
                        loading = false,
                        error = result.message
                    )
                }
                WeatherResult.Loading -> {}
            }
        }
    }
}
