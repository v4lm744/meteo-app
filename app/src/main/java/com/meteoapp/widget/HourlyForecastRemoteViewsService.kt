package com.meteoapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.meteoapp.R
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils

/**
 * Service du widget collection horaire : fournit à la ListView du widget les
 * prévisions stockées par [HourlyForecastStore] (le factory ne peut pas
 * faire d'appels réseau).
 */
class HourlyForecastRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        HourlyForecastFactory(applicationContext, intent)

    class HourlyForecastFactory(
        private val context: Context,
        intent: Intent
    ) : RemoteViewsFactory {

        private val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        private var entries: List<HourlyForecastStore.HourSnapshot> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            entries = HourlyForecastStore.loadSnapshot(context, appWidgetId)
        }

        override fun onDestroy() {}

        override fun getCount(): Int = entries.size

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_hourly_item)
            val entry = entries.getOrNull(position) ?: return views
            views.setTextViewText(
                R.id.itemHour,
                WeatherUtils.formatHour(context, entry.dt, entry.timezoneOffset)
            )
            views.setTextViewText(R.id.itemTemp, WeatherUtils.formatTemp(context, entry.temp))
            val pop = entry.pop
            if (pop >= 0.05) {
                views.setTextViewText(
                    R.id.itemPop,
                    context.getString(R.string.format_percent, (pop * 100).toInt())
                )
                views.setViewVisibility(R.id.itemPop, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.itemPop, View.INVISIBLE)
            }
            val iconRes = WeatherIcons.forCondition(
                context,
                entry.conditionId,
                isDay = !entry.icon.endsWith("n")
            )
            views.setImageViewResource(R.id.itemIcon, iconRes)
            views.setOnClickFillInIntent(R.id.itemRoot, Intent())
            return views
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long =
            entries.getOrNull(position)?.dt ?: position.toLong()

        override fun hasStableIds(): Boolean = true
    }
}
