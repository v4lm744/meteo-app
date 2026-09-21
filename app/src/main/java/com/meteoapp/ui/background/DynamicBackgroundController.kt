package com.meteoapp.ui.background

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.Window
import com.meteoapp.databinding.ActivityMainBinding
import com.meteoapp.util.WeatherColors

/**
 * Fond d'écran dynamique : dégradé vertical animé (interpolation ARGB sur
 * 800 ms) entre les couleurs du ciel selon la météo et l'heure, plus la teinte
 * des icônes de la barre d'outils en fonction de la luminance du fond.
 */
class DynamicBackgroundController(
    private val binding: ActivityMainBinding,
    private val window: Window,
    private val toolbar: com.google.android.material.appbar.MaterialToolbar,
    private val onTintChanged: (Int) -> Unit
) {

    private var currentTop: Int = -1
    private var currentBottom: Int = -1
    private var animator: ValueAnimator? = null

    /**
     * Applique le dégradé [top]/[bottom] avec une transition douce, ou
     * immédiatement s'il n'y a pas encore de fond (premier affichage).
     */
    fun apply(top: Int, bottom: Int) {
        if (currentTop < 0 || currentBottom < 0) {
            currentTop = top
            currentBottom = bottom
            window.statusBarColor = top
            binding.root.background = gradientDrawable(top, bottom)
            tintToolbar(top)
            return
        }
        if (currentTop == top && currentBottom == bottom) return

        val fromTop = currentTop
        val fromBottom = currentBottom
        currentTop = top
        currentBottom = bottom

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800L
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                val blendedTop = argbBlend(fromTop, top, fraction)
                val blendedBottom = argbBlend(fromBottom, bottom, fraction)
                window.statusBarColor = blendedTop
                binding.root.background = gradientDrawable(blendedTop, blendedBottom)
            }
            start()
        }
        tintToolbar(top)
    }

    /** Teinte les icônes de la barre d'outils selon la luminance du fond. */
    fun tintToolbar(bgColor: Int) {
        val tint = WeatherColors.contrastColor(bgColor)
        toolbar.overflowIcon?.mutate()?.setTint(tint)
        toolbar.navigationIcon?.mutate()?.setTint(tint)
        toolbar.setTitleTextColor(tint)
        val isLightBg = tint == 0xFF000000.toInt()
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = isLightBg
        onTintChanged(tint)
    }

    /** Réinitialise l'état interne : prochain apply() sera immédiat. */
    fun reset() {
        currentTop = -1
        currentBottom = -1
        animator?.cancel()
    }

    private fun gradientDrawable(top: Int, bottom: Int): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, bottom)
        )

    private fun argbBlend(from: Int, to: Int, fraction: Float): Int {
        val inv = 1f - fraction
        fun channel(shift: Int): Int =
            (((from shr shift) and 0xFF) * inv + ((to shr shift) and 0xFF) * fraction).toInt()
        return 0xFF shl 24 or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
