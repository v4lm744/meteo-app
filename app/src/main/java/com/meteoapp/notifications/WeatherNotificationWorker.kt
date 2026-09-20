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
 * Travail quotidien de notification météo : récupère la météo de la ville
 * courante (dernière consultée, ou première des favoris) et affiche une
 * notification récapitulative (température, condition, min/max du jour).
 */
class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "weather_daily"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        val city = com.meteoapp.city.FavoriteCitiesStore.currentNotificationCity(context)
        if (city == null || !NotificationPrefs.isEnabled(context)) {
            return Result.success()
        }

        val repository = WeatherRepository(context)
        val weather = when (val result = repository.getWeather(city.lat, city.lon)) {
            is WeatherResult.Success -> result.data
            else -> return Result.success()
        }

        postNotification(context, city.name, weather)
        return Result.success()
    }

    private fun postNotification(
        context: Context,
        cityName: String,
        weather: com.meteoapp.data.model.WeatherData
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

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
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

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
