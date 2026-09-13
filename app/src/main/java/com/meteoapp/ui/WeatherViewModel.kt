package com.meteoapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.meteoapp.data.Result
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.RegionCity
import com.meteoapp.data.model.WeatherData
import com.meteoapp.location.LocationHelper
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val weather: WeatherData? = null,
    val city: GeoLocation? = null,
    val regionCities: List<RegionCity> = emptyList(),
    val error: String? = null
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> = _state

    val isApiKeyConfigured: Boolean
        get() = repository.isApiKeyConfigured

    fun loadWeatherForCity(city: GeoLocation) {
        viewModelScope.launch {
            _state.value = _state.value?.copy(loading = true, error = null, city = city)
            when (val result = repository.getWeather(city.lat, city.lon)) {
                is Result.Success -> {
                    _state.value = UiState(
                        loading = false,
                        weather = result.data,
                        city = city,
                        error = null
                    )
                    loadRegionCities(city.lat, city.lon)
                }
                is Result.Error -> {
                    _state.value = _state.value?.copy(
                        loading = false,
                        error = result.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }

    fun refresh() {
        val current = _state.value ?: return
        val lat = current.weather?.lat ?: current.city?.lat ?: return
        val lon = current.weather?.lon ?: current.city?.lon ?: return
        viewModelScope.launch {
            _state.value = current.copy(refreshing = true, error = null)
            when (val result = repository.getWeather(lat, lon)) {
                is Result.Success -> {
                    _state.value = current.copy(
                        refreshing = false,
                        weather = result.data,
                        error = null
                    )
                    loadRegionCities(lat, lon)
                }
                is Result.Error -> {
                    _state.value = current.copy(refreshing = false, error = result.message)
                }
                Result.Loading -> {}
            }
        }
    }

    private fun loadRegionCities(lat: Double, lon: Double) {
        viewModelScope.launch {
            when (val result = repository.getRegionCities(lat, lon)) {
                is Result.Success -> {
                    _state.value = _state.value?.copy(regionCities = result.data)
                }
                else -> {}
            }
        }
    }

    fun loadForCurrentLocation() {
        viewModelScope.launch {
            if (!locationHelper.hasLocationPermission()) {
                _state.value = _state.value?.copy(
                    error = "Autorisation de localisation requise"
                )
                return@launch
            }
            _state.value = _state.value?.copy(loading = true, error = null)
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _state.value = _state.value?.copy(
                    loading = false,
                    error = "Position indisponible"
                )
                return@launch
            }
            val geoResult = repository.reverseGeocode(location.latitude, location.longitude)
            val city = (geoResult as? Result.Success)?.data?.firstOrNull()
                ?: GeoLocation(
                    name = "Position actuelle",
                    localNames = null,
                    lat = location.latitude,
                    lon = location.longitude,
                    country = "",
                    state = null
                )

            when (val result = repository.getWeather(location.latitude, location.longitude)) {
                is Result.Success -> {
                    _state.value = UiState(
                        loading = false,
                        weather = result.data,
                        city = city,
                        error = null
                    )
                    loadRegionCities(location.latitude, location.longitude)
                }
                is Result.Error -> {
                    _state.value = _state.value?.copy(
                        loading = false,
                        error = result.message
                    )
                }
                Result.Loading -> {}
            }
        }
    }
}
