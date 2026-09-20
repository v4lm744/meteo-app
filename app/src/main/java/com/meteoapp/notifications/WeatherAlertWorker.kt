package com.meteoapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meteoapp.R
import com.meteoapp.ui.MainActivity
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.WeatherResult

/**
 * Worker des alertes météo : récupère la météo de la ville courante et
 * poste une notification critique (CATEGORY_ALARM) si les prévisions des
 * 12 prochaines heures dépassent un seuil configuré. Chaque alerte est
 * silencée 12 h après son envoi pour éviter le spam.
 */
class WeatherAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "weather_alerts"
        private const val NOTIFICATION_ID = 4001
        private const val PREFS_SEEN = "meteo_alert_seen"
        private const val SEEN_WINDOW_MS = 12 * 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!AlertPrefs.isEnabled(context)) return Result.success()

        val city = com.meteoapp.city.FavoriteCitiesStore.currentNotificationCity(context)
            ?: return Result.success()

        val repository = WeatherRepository(context)
        val result = repository.getWeather(city.lat, city.lon)
        val weather = (result as? WeatherResult.Success)?.data ?: return Result.success()

        val alerts = WeatherAlertEvaluator.evaluate(context, weather)
        if (alerts.isEmpty()) return Result.success()

        val fresh = filterAlreadySeen(context, alerts)
        if (fresh.isEmpty()) return Result.success()

        if (!hasNotificationPermission(context)) return Result.success()
        ensureChannel(context)
        postAlert(context, city.localNames?.fr ?: city.name, fresh)
        return Result.success()
    }

    private fun filterAlreadySeen(
        context: Context,
        alerts: List<WeatherAlertEvaluator.Alert>
    ): List<WeatherAlertEvaluator.Alert> {
        val prefs = context.getSharedPreferences(PREFS_SEEN, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        return alerts.filter { alert ->
            val seenAt = prefs.getLong(alert.key, 0L)
            val isFresh = now - seenAt >= SEEN_WINDOW_MS
            if (isFresh) {
                prefs.edit { putLong(alert.key, now) }
            }
            isFresh
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alert_channel_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun postAlert(
        context: Context,
        cityName: String,
        alerts: List<WeatherAlertEvaluator.Alert>
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, NOTIFICATION_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = alerts.joinToString("\n") { it.message }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_weather_placeholder)
            .setContentTitle(context.getString(R.string.alert_notification_title, cityName))
            .setContentText(alerts.first().message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
