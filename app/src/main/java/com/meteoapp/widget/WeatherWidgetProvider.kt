package com.meteoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.meteoapp.R
import com.meteoapp.data.Result
import com.meteoapp.data.WeatherRepository
import com.meteoapp.data.model.GeoLocation
import com.meteoapp.ui.MainActivity
import com.meteoapp.util.WeatherUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.AppWidgetTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("unused")
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

        // Intent pour ouvrir l'app au tap
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
            views.setTextViewText(R.id.widgetTemp, "—")
            views.setTextViewText(R.id.widgetDesc, context.getString(R.string.widget_configure_prompt))
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val displayName = city.localNames?.fr ?: city.name
        views.setTextViewText(R.id.widgetCity, displayName)
        views.setTextViewText(R.id.widgetTemp, "…")
        views.setTextViewText(R.id.widgetDesc, context.getString(R.string.loading))
        appWidgetManager.updateAppWidget(appWidgetId, views)

        fetchWeather(context, appWidgetManager, appWidgetId, views, city)
    }

    private fun fetchWeather(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        city: GeoLocation
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = WeatherRepository()
            val result = repository.getWeather(city.lat, city.lon)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        val current = result.data.current
                        views.setTextViewText(R.id.widgetCity, city.localNames?.fr ?: city.name)
                        views.setTextViewText(
                            R.id.widgetTemp,
                            "${WeatherUtils.roundToInt(current.temp)}°"
                        )
                        val desc = current.weather.firstOrNull()?.description
                            ?.replaceFirstChar { it.uppercase() } ?: ""
                        views.setTextViewText(R.id.widgetDesc, desc)

                        val today = result.data.daily.firstOrNull()
                        if (today != null) {
                            views.setTextViewText(
                                R.id.widgetMinMax,
                                "Max ${WeatherUtils.roundToInt(today.tempMax)}°  Min ${WeatherUtils.roundToInt(today.tempMin)}°"
                            )
                        } else {
                            views.setTextViewText(R.id.widgetMinMax, "")
                        }

                        // Fond dégradé dynamique selon météo + heure
                        try {
                            val opts = appWidgetManager.getAppWidgetOptions(appWidgetId)
                            val density = context.resources.displayMetrics.density
                            var w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
                            var h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
                            if (w <= 0) w = (250 * density).toInt()
                            if (h <= 0) h = (70 * density).toInt()
                            val bg = WidgetGradient.buildBackground(
                                context.applicationContext, result.data, w, h
                            )
                            views.setImageViewBitmap(R.id.widgetBackground, bg)
                        } catch (_: Exception) {
                        }

                        val iconCode = current.weather.firstOrNull()?.icon
                        if (!iconCode.isNullOrEmpty()) {
                            try {
                                Glide.with(context.applicationContext)
                                    .asBitmap()
                                    .load(WeatherUtils.iconUrl(iconCode))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(
                                        AppWidgetTarget(
                                            context.applicationContext,
                                            R.id.widgetIcon,
                                            views,
                                            appWidgetId
                                        )
                                    )
                            } catch (_: Exception) {
                            }
                        }
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    is Result.Error -> {
                        views.setTextViewText(
                            R.id.widgetDesc, context.getString(R.string.error_generic)
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    Result.Loading -> {}
                }
            }
        }
    }
}
