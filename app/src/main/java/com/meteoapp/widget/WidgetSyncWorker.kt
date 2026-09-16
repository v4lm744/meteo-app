package com.meteoapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Travail en arrière-plan planifié par [WidgetSyncScheduler] qui rafraîchit la
 * météo de tous les widgets d'accueil installés. Lancé périodiquement selon
 * l'intervalle choisi par l'utilisateur dans le menu Paramètres.
 *
 * Le système ne garantit pas d'intervalle inférieur à ~30 min pour
 * `updatePeriodMillis` du widget ; ce Worker comble donc ce manque pour les
 * intervalles plus courts et garantit un rafraîchissement régulier.
 */
class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, WeatherWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return Result.success()

        val provider = WeatherWidgetProvider()
        var anySuccess = false
        for (id in ids) {
            try {
                provider.syncOne(context, id)
                anySuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "Échec de synchro du widget $id", e)
            }
        }
        return if (anySuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "WidgetSyncWorker"
    }
}
