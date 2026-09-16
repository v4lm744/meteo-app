package com.meteoapp.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Gère la planification (et l'annulation) du [WidgetSyncWorker] périodique.
 *
 * Un seul travail périodique identifié par [WORK_NAME] est maintenu : remplacer
 * l'intervalle revient à annuler puis replanifier via [ExistingPeriodicWorkPolicy.UPDATE].
 * Quand la synchro automatique est désactivée ([SyncPrefs.SYNC_DISABLED]), le travail
 * est purement annulé.
 */
object WidgetSyncScheduler {

    private const val WORK_NAME = "widget_sync_periodic"

    /**
     * (Re)planifie la synchro automatique selon l'intervalle enregistré dans
     * [SyncPrefs]. À appeler au démarrage de l'application et après tout changement
     * dans le menu Paramètres.
     */
    fun reschedule(context: Context) {
        val minutes = SyncPrefs.getIntervalMinutes(context)
        schedule(context, minutes)
    }

    /**
     * Planifie explicitement la synchro pour [minutes]. Une valeur de
     * [SyncPrefs.SYNC_DISABLED] annule tout travail existant.
     */
    fun schedule(context: Context, minutes: Int) {
        val workManager = WorkManager.getInstance(context)
        if (minutes <= 0) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        // Toutes les valeurs proposées respectent la période minimale de 15 min
        // imposée par WorkManager.
        val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
            minutes.toLong(), TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
