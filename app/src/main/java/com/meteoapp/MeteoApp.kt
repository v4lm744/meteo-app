package com.meteoapp

import android.app.Application
import org.osmdroid.config.Configuration
import com.meteoapp.notifications.WeatherAlertScheduler
import com.meteoapp.notifications.WeatherNotificationScheduler
import com.meteoapp.util.ThemePrefs
import com.meteoapp.widget.WidgetSyncScheduler
import java.io.File

class MeteoApp : Application(), androidx.work.Configuration.Provider {

    /**
     * Configuration de WorkManager fournie à la demande (on-demand initialization).
     * On retire l'auto-initialisation par défaut (manifeste) afin que getInstance()
     * s'initialise via ce provider, y compris sous Robolectric où androidx.startup
     * n'est pas exécuté.
     */
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        ThemePrefs.applyMode(this)
        if (ThemePrefs.isDynamicColorsSupported(this) &&
            ThemePrefs.isDynamicColorsEnabled(this)
        ) {
            com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this)
        }
        val baseDir = getExternalFilesDir(null) ?: filesDir
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = baseDir
            osmdroidTileCache = File(baseDir, "osmdroid")
            load(this@MeteoApp, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MeteoApp))
        }
        WidgetSyncScheduler.reschedule(this)
        WeatherNotificationScheduler.reschedule(this)
        WeatherAlertScheduler.reschedule(this)
    }
}
