package com.meteoapp.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Planification de la notification météo quotidienne : un travail
 * périodique de 24 h, initial delay calculé pour déclencher vers l'heure
 * choisie dans les Paramètres. [reschedule] est appelé après chaque
 * changement de configuration et au démarrage de l'application.
 */
object WeatherNotificationScheduler {

    private const val WORK_NAME = "weather_daily_notification"

    fun reschedule(context: Context) {
        if (!NotificationPrefs.isEnabled(context)) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = delayUntilNextRun(NotificationPrefs.getHour(context))
        val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Millisecondes jusqu'au prochain passage à [hour]:00 de l'heure locale.
     */
    fun delayUntilNextRun(hour: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)
        val targetDate = if (now.toLocalTime() > LocalTime.of(hour.coerceIn(0, 23), 0)) {
            now.toLocalDate().plusDays(1)
        } else {
            now.toLocalDate()
        }
        val target = LocalDateTime.of(targetDate, LocalTime.of(hour.coerceIn(0, 23), 0))
        return target.atZone(zone).toInstant().toEpochMilli() - nowMillis
    }
}
