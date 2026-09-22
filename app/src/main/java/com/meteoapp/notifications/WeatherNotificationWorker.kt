package com.meteoapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meteoapp.R
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.WeatherResult
import com.meteoapp.ui.MainActivity
import com.meteoapp.util.WeatherUtils

/**
 * Travail quotidien de notification météo : récupère la météo de chaque ville
 * favorite (la ville courante est incluse si elle est favorite ou dernière
 * consultée) et affiche une notification récapitulative par ville (température,
 * condition, min/max du jour). Chaque ville possède son propre ID de
 * notification, ce qui permet de les examiner ou supprimer indépendamment.
 */
class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "weather_daily"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!NotificationPrefs.isEnabled(context)) {
            return Result.success()
        }
        val cities = com.meteoapp.city.FavoriteCitiesStore
            .dailyNotificationCities(context)
        if (cities.isEmpty()) {
            return Result.success()
        }
        val repository = com.meteoapp.data.ServiceLocator.weatherRepository(context)
        cities.forEachIndexed { index, city ->
            val weather = when (
                val result = repository.getWeather(city.lat, city.lon)
            ) {
                is WeatherResult.Success -> result.data
                else -> return@forEachIndexed
            }
            postNotification(
                context,
                city.displayName(context),
                city.lat,
                city.lon,
                weather,
                NOTIFICATION_ID_BASE + index
            )
        }
        return Result.success()
    }

    private fun postNotification(
        context: Context,
        cityName: String,
        lat: Double,
        lon: Double,
        weather: com.meteoapp.data.model.WeatherData,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
        )
        val temp = WeatherUtils.formatTemp(context, weather.current.temp)
        val condition = weather.current.weather.firstOrNull()?.description
            ?.replaceFirstChar { it.uppercase() } ?: ""
        val today = weather.daily.firstOrNull()
        val minMax = today?.let {
            context.getString(
                R.string.min_max_format,
                WeatherUtils.formatTemp(context, it.tempMax),
                WeatherUtils.formatTemp(context, it.tempMin)
            )
        } ?: ""
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CITY_NAME, cityName)
            putExtra(MainActivity.EXTRA_CITY_LAT, lat)
            putExtra(MainActivity.EXTRA_CITY_LON, lon)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_placeholder)
            .setContentTitle(context.getString(R.string.notification_title, cityName))
            .setContentText(
                buildString {
                    append(temp)
                    if (condition.isNotEmpty()) append(" · ").append(condition)
                    if (minMax.isNotEmpty()) append(" · ").append(minMax)
                }
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
