package com.meteoapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            fusedClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        } catch (e: Exception) {
            try {
                fusedClient.lastLocation.await()
            } catch (e2: Exception) {
                null
            }
        }
    }
}
