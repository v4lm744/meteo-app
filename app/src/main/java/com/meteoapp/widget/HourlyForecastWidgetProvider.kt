package com.meteoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.meteoapp.R
import com.meteoapp.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget collection des prévisions horaires : liste défilante des
 * prochaines heures (icône, heure, température, probabilité de pluie) via
 * un RemoteViewsService. La ville est partagée avec le widget principal
 * ([WidgetPrefs]) mais possède son propre stockage pour rester autonome.
 */
class HourlyForecastWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            val city = WidgetPrefs.getCity(context, id)
            val views = RemoteViews(context.packageName, R.layout.widget_hourly_forecast)
            if (city == null) {
                views.setTextViewText(R.id.hourlyWidgetTitle, context.getString(R.string.widget_configure_prompt))
                views.setEmptyView(R.id.hourlyWidgetList, R.id.hourlyWidgetEmpty)
                appWidgetManager.updateAppWidget(id, views)
                return@forEach
            }
            views.setTextViewText(R.id.hourlyWidgetTitle, city.displayName(context))
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CITY_NAME, city.displayName(context))
                putExtra(MainActivity.EXTRA_CITY_LAT, city.lat)
                putExtra(MainActivity.EXTRA_CITY_LON, city.lon)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.hourlyWidgetRoot,
                PendingIntent.getActivity(
                    context, id, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            val listIntent = Intent(context, HourlyForecastRemoteViewsService::class.java).apply {
                data = intentData(id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            views.setRemoteAdapter(R.id.hourlyWidgetList, listIntent)
            views.setEmptyView(R.id.hourlyWidgetList, R.id.hourlyWidgetEmpty)
            appWidgetManager.updateAppWidget(id, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.hourlyWidgetList)
            CoroutineScope(Dispatchers.IO).launch {
                HourlyForecastStore.refresh(context, id, city)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            HourlyForecastStore.remove(context, id)
            WidgetPrefs.remove(context, id)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {

        /** Uri unique par widget pour éviter le partage d'intent entre instances. */
        fun intentData(appWidgetId: Int) =
            android.net.Uri.parse("hourly-widget://$appWidgetId")

        fun triggerUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HourlyForecastWidgetProvider::class.java))
            Intent(context, HourlyForecastWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(this)
            }
        }
    }
}
