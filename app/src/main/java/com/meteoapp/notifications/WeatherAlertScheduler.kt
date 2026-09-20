package com.meteoapp.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Planification du contrôle périodique des alertes météo (toutes les 6 h) :
 * vérifie les prévisions des 12 prochaines heures de la ville courante
 * et notifie si un seuil est dépassé.
 */
object WeatherAlertScheduler {

    private const val WORK_NAME = "weather_alert_check"

    fun reschedule(context: Context) {
        val appContext = context.applicationContext
        val enabled = AlertPrefs.isEnabled(appContext)
        val workManager = WorkManager.getInstance(appContext)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<WeatherAlertWorker>(6, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
