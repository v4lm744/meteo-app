package com.meteoapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meteoapp.R

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
        if (ids.isEmpty() && manager.getAppWidgetIds(
                ComponentName(context, HourlyForecastWidgetProvider::class.java)
            ).isEmpty()
        ) return Result.success()

        // Un seul appel réseau par ville : plusieurs widgets sur la même
        // ville partagent les mêmes données via le cache du repository.
        val provider = WeatherWidgetProvider()
        var anySuccess = false
        for (id in ids.sorted()) {
            try {
                provider.syncOne(context, id)
                anySuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "Échec de synchro du widget $id", e)
            }
        }
        if (syncHourlyWidget(context)) {
            anySuccess = true
        }
        return if (anySuccess) Result.success() else Result.retry()
    }

    /**
     * Rafraîchit le stockage du widget collection horaire pour chaque
     * instance installée, puis notifie les listes RemoteViews.
     */
    @Suppress("DEPRECATION")
    private suspend fun syncHourlyWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val hourlyIds = manager.getAppWidgetIds(
            ComponentName(context, HourlyForecastWidgetProvider::class.java)
        )
        if (hourlyIds.isEmpty()) return true
        var anySuccess = false
        for (id in hourlyIds.sorted()) {
            val city = WidgetPrefs.getCity(context, id) ?: continue
            try {
                if (HourlyForecastStore.refresh(context, id, city)) {
                    anySuccess = true
                    manager.notifyAppWidgetViewDataChanged(id, R.id.hourlyWidgetList)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Échec de synchro du widget horaire $id", e)
            }
        }
        return anySuccess
    }

    companion object {
        private const val TAG = "WidgetSyncWorker"
    }
}
