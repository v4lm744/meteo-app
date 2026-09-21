package com.meteoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.meteoapp.R
import com.meteoapp.data.WeatherResult
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.ui.MainActivity
import com.meteoapp.util.WeatherIcons
import com.meteoapp.util.WeatherUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            WidgetPrefs.remove(context, id)
            WidgetThemePrefs.remove(context, id)
        }
    }

    companion object {
        const val ACTION_UPDATE = "com.meteoapp.widget.UPDATE"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(
                    ComponentName(context, WeatherWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    private fun updateOne(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val city = WidgetPrefs.getCity(context, appWidgetId)
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            if (city != null) {
                putExtra(MainActivity.EXTRA_CITY_NAME, city.localNames?.fr ?: city.name)
                putExtra(MainActivity.EXTRA_CITY_LAT, city.lat)
                putExtra(MainActivity.EXTRA_CITY_LON, city.lon)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, appWidgetId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pi)

        if (city == null) {
            views.setTextViewText(R.id.widgetCity, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widgetTemp, "\u2014")
            views.setTextViewText(R.id.widgetDesc, context.getString(R.string.widget_configure_prompt))
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val displayName = city.localNames?.fr ?: city.name
        views.setTextViewText(R.id.widgetCity, displayName)
        views.setTextViewText(R.id.widgetTemp, "\u2026")
        views.setTextViewText(R.id.widgetDesc, context.getString(R.string.loading))
        appWidgetManager.updateAppWidget(appWidgetId, views)

        fetchWeather(context, appWidgetManager, appWidgetId, views, city)
    }

    /**
     * Récupère la météo en arrière-plan via goAsync() : le BroadcastReceiver reste actif
     * (PendingResult) jusqu'à la fin du traitement, ce qui respecte son cycle de vie
     * au lieu de lancer une coroutine orpheline sur un CoroutineScope non géré.
     */
    private fun fetchWeather(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        city: GeoLocation
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val repository = WeatherRepository(context)
                val result = repository.getWeather(city.lat, city.lon)
                withContext(Dispatchers.Main) {
                    renderResult(context, appWidgetManager, appWidgetId, views, city, result)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Met à jour un widget avec la météo de la ville associée, sans passer par le
     * cycle BroadcastReceiver. Réutilisé par la synchro automatique en arrière-plan
     * ([WidgetSyncWorker]) et conserve le même rendu que [onUpdate].
     */
    suspend fun syncOne(context: Context, appWidgetId: Int) {
        val city = WidgetPrefs.getCity(context, appWidgetId) ?: return
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.widget_weather)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_CITY_NAME, city.localNames?.fr ?: city.name)
            putExtra(MainActivity.EXTRA_CITY_LAT, city.lat)
            putExtra(MainActivity.EXTRA_CITY_LON, city.lon)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, appWidgetId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pi)

        withContext(Dispatchers.Main) {
            val displayName = city.localNames?.fr ?: city.name
            views.setTextViewText(R.id.widgetCity, displayName)
            views.setTextViewText(R.id.widgetTemp, "\u2026")
            views.setTextViewText(R.id.widgetDesc, context.getString(R.string.loading))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        val repository = WeatherRepository(context)
        val result = repository.getWeather(city.lat, city.lon)
        withContext(Dispatchers.Main) {
            renderResult(context, appWidgetManager, appWidgetId, views, city, result)
        }
    }

    private fun renderResult(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        city: GeoLocation,
        result: WeatherResult<com.meteoapp.data.model.WeatherData>
    ) {
        when (result) {
            is WeatherResult.Success -> {
                val current = result.data.current
                views.setTextViewText(R.id.widgetCity, city.localNames?.fr ?: city.name)
                views.setTextViewText(
                    R.id.widgetTemp,
                    WeatherUtils.formatTemp(context, current.temp)
                )
                val desc = current.weather.firstOrNull()?.description
                    ?.replaceFirstChar { it.uppercase() } ?: ""
                views.setTextViewText(R.id.widgetDesc, desc)

                val today = result.data.daily.firstOrNull()
                if (today != null) {
                    views.setTextViewText(
                        R.id.widgetMinMax,
                        context.getString(
                            R.string.min_max_format,
                            WeatherUtils.formatTemp(context, today.tempMax),
                            WeatherUtils.formatTemp(context, today.tempMin)
                        )
                    )
                } else {
                    views.setTextViewText(R.id.widgetMinMax, "")
                }

                try {
                    val opts: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val density = context.resources.displayMetrics.density
                    var w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
                    var h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                    if (w <= 0) w = (250 * density).toInt()
                    if (h <= 0) h = (70 * density).toInt()
                    val bg =
                        if (WidgetThemePrefs.getTheme(context, appWidgetId) == WidgetThemePrefs.Theme.DARK) {
                            WidgetGradient.buildDarkBackground(context.applicationContext, w, h)
                        } else {
                            WidgetGradient.buildBackground(
                                context.applicationContext, result.data, w, h
                            )
                        }
                    views.setImageViewBitmap(R.id.widgetBackground, bg)
                } catch (_: Exception) {
                }

                val condition = current.weather.firstOrNull()
                val iconRes = condition?.let {
                    WeatherIcons.forCondition(context, it.id, isDay = !it.icon.endsWith("n"))
                } ?: R.drawable.ic_wx_partly
                views.setImageViewResource(R.id.widgetIcon, iconRes)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            is WeatherResult.Error -> {
                views.setTextViewText(
                    R.id.widgetDesc, context.getString(R.string.error_generic)
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
            WeatherResult.Loading -> {}
        }
    }
}
