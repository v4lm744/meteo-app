package com.meteoapp

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class MeteoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val baseDir = getExternalFilesDir(null) ?: filesDir
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = baseDir
            osmdroidTileCache = File(baseDir, "osmdroid")
            load(this@MeteoApp, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MeteoApp))
        }
    }
}
