package com.meteoapp.util

import android.content.Context
import android.graphics.drawable.Animatable
import android.widget.ImageView
import com.meteoapp.R

/**
 * Icônes météo vectorielles animées maison, en remplacement des PNG
 * OpenWeather : soleil, nuage/soleil, pluie, neige, orage, brume, nuit.
 *
 * Le mapping se fait sur le code de condition OpenWeather (`weather.id`)
 * et le suffixe jour/nuit (`weather.icon` se termine par `d` ou `n`).
 */
object WeatherIcons {

    private val FALLBACK_ICON_RES = R.drawable.ic_wx_partly

    /**
     * Retourne l'identifiant du drawable animé correspondant au code
     * condition OpenWeather et au suffixe jour (`true`) / nuit (`false`).
     * Toujours non nul : les conditions inconnues retombent sur l'icône
     * partielle (nuages), jamais sur un `getIdentifier(null)` qui planterait.
     */
    fun forCondition(context: Context, code: Long, isDay: Boolean): Int {
        return nameFor(code, isDay) ?: FALLBACK_ICON_RES
    }

    private fun iconResFor(code: Long, isDay: Boolean): Int? = when (code) {
        in 200..232 -> R.drawable.ic_wx_storm
        in 300..321 -> R.drawable.ic_wx_rain
        in 500..501 -> R.drawable.ic_wx_rain
        in 502..531 -> R.drawable.ic_wx_rain
        in 600..622 -> R.drawable.ic_wx_snow
        in 701..781 -> R.drawable.ic_wx_haze
        800L -> if (isDay) R.drawable.ic_wx_sun else R.drawable.ic_wx_night
        801L -> if (isDay) R.drawable.ic_wx_partly else R.drawable.ic_wx_night
        in 802..804 -> R.drawable.ic_wx_partly
        else -> null
    }

    /**
     * Charge l'icône dans l'[ImageView] et démarre l'animation le cas échéant
     * (AnimatedVectorDrawable). Ne change rien si le drawable est introuvable.
     */
    fun bind(view: ImageView, code: Long, isDay: Boolean) {
        val id = forCondition(view.context, code, isDay)
        if (id == 0) return
        val drawable = androidx.core.content.ContextCompat.getDrawable(view.context, id) ?: return
        view.setImageDrawable(drawable)
        (drawable as? Animatable)?.start()
    }

    /**
     * Identifiant du drawable animé pour un code icône OpenWeather
     * (ex. « 10d », « 02n »), ou `null` si inconnu.
     */
    fun forIconCode(iconCode: String, isDayOverride: Boolean? = null): Int? {
        if (iconCode.length < 2) return null
        val isDay = isDayOverride ?: iconCode.endsWith("d")
        val group = iconCode.substring(0, 2).toIntOrNull() ?: return null
        return nameFor(group * 100L, isDay)
    }

    /**
     * Icône drawable pour un code condition et jour/nuit, ou `null` si
     * inconnu (l'appelant garde alors son icône précédente).
     */
    fun nameFor(code: Long, isDay: Boolean): Int? = iconResFor(code, isDay)
}
