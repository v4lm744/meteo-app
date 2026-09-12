package com.meteoapp.data

import com.meteoapp.BuildConfig
import com.meteoapp.data.api.ApiClient
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.data.model.OneCallResponse

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cityNotFound: Boolean = false) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class WeatherRepository {

    private val api = ApiClient.api

    private val apiKey: String
        get() = BuildConfig.OPEN_WEATHER_API_KEY

    val isApiKeyConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun getWeather(lat: Double, lon: Double): Result<OneCallResponse> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée", cityNotFound = false)
        }
        return try {
            val response = api.getOneCall(lat = lat, lon = lon, apiKey = apiKey)
            Result.Success(response)
        } catch (e: retrofit2.HttpException) {
            Result.Error(
                message = if (e.code() == 401 || e.code() == 403) "Clé API invalide" else "Erreur serveur",
                cityNotFound = e.code() == 404
            )
        } catch (e: Exception) {
            Result.Error("Vérifiez votre connexion internet")
        }
    }

    suspend fun searchCity(query: String): Result<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        return try {
            val results = api.geocode(query = query.trim(), apiKey = apiKey)
            Result.Success(results)
        } catch (e: Exception) {
            Result.Error("Erreur lors de la recherche")
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<List<GeoLocation>> {
        if (!isApiKeyConfigured) {
            return Result.Error("Clé API non configurée")
        }
        return try {
            val results = api.reverseGeocode(lat = lat, lon = lon, apiKey = apiKey)
            Result.Success(results)
        } catch (e: Exception) {
            Result.Error("Erreur lors de la localisation")
        }
    }
}
