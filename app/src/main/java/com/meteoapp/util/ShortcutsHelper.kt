package com.meteoapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.meteoapp.R
import com.meteoapp.ui.MainActivity

/**
 * Raccourcis dynamiques : les 4 premières villes favorites apparaissent
 * en appui long sur l'icône de l'app, chacune ouvrant directement sa
 * météo dans le tableau de bord.
 */
object ShortcutsHelper {

    private const val MAX_SHORTCUTS = 4

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val favorites = com.meteoapp.city.FavoriteCitiesStore.getFavorites(appContext)
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(appContext) &&
            ShortcutManagerCompat.getDynamicShortcuts(appContext).isEmpty() &&
            favorites.isEmpty()
        ) {
            return
        }

        val shortcuts = favorites.take(MAX_SHORTCUTS).mapIndexed { index, city ->
            val label = city.displayName(appContext)
            ShortcutInfoCompat.Builder(appContext, "city_${city.lat}_${city.lon}")
                .setShortLabel(label)
                .setIcon(IconCompat.createWithAdaptiveBitmap(locationBitmap(appContext)))
                .setIntent(
                    Intent(appContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(MainActivity.EXTRA_CITY_NAME, city.name)
                        putExtra(MainActivity.EXTRA_CITY_LAT, city.lat)
                        putExtra(MainActivity.EXTRA_CITY_LON, city.lon)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                .setRank(index)
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(appContext, shortcuts) }
        if (shortcuts.isNotEmpty()) {
            runCatching { ShortcutManagerCompat.reportShortcutUsed(appContext, shortcuts.first().id) }
        }
    }

    private fun locationBitmap(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location)
            ?: return androidx.core.graphics.createBitmap(96, 96)
        return drawable.toBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        )
    }
}
