package com.meteoapp

import android.app.Application
import org.osmdroid.config.Configuration

class MeteoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = getExternalFilesDir(null)?.absolutePath
            osmdroidTileCache = "${getExternalFilesDir(null)?.absolutePath}/osmdroid"
            load(this@MeteoApp, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MeteoApp))
        }
    }
}
