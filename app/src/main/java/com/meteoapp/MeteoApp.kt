package com.meteoapp

import android.app.Application
import androidx.work.Configuration
import org.osmdroid.config.Configuration
import com.meteoapp.widget.WidgetSyncScheduler
import java.io.File

class MeteoApp : Application(), Configuration.Provider {

    /**
     * Configuration de WorkManager fournie à la demande (on-demand initialization).
     * On retire l'auto-initialisation par défaut (manifeste) afin que getInstance()
     * s'initialise via ce provider, y compris sous Robolectric où androidx.startup
     * n'est pas exécuté.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        val baseDir = getExternalFilesDir(null) ?: filesDir
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = baseDir
            osmdroidTileCache = File(baseDir, "osmdroid")
            load(this@MeteoApp, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MeteoApp))
        }
        WidgetSyncScheduler.reschedule(this)
    }
}
