package com.meteoapp.util

import android.os.Build
import android.view.Window

/**
 * Barres système : à partir d'Android 15 (API 35), le mode edge-to-edge est
 * imposé et Window.setStatusBarColor est ignoré (le fond de l'activité reste
 * visible sous la barre transparente). L'attribution dynamique ne conserve
 * donc d'effet que sur les versions antérieures.
 */
object SystemBars {

    fun setStatusBarColorCompat(window: Window, color: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = color
        }
    }
}
