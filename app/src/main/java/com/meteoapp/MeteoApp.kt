package com.meteoapp

import android.app.Application
import org.osmdroid.config.Configuration

class MeteoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val baseDir = getExternalFilesDir(null)?.absolutePath
            ?: filesDir.absolutePath
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = baseDir
            osmdroidTileCache = "$baseDir/osmdroid"
            load(this@MeteoApp, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MeteoApp))
        }
    }
}
