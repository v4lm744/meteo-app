package com.meteoapp.ui.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.meteoapp.R
import com.meteoapp.util.WeatherIcons

/**
 * SwipeRefreshLayout dont l'indicateur par défaut (cercle Material) est
 * remplacé par une icône météo animée (AVD de la condition courante),
 * qui oscille doucement pendant le rafraîchissement.
 */
class WeatherSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var weatherIcon: ImageView? = null
    private var bobAnimator: ValueAnimator? = null
    private var currentCode: Long = 800L
    private var currentIsDay: Boolean = true

    init {
        setProgressBackgroundColorSchemeColor(Color.TRANSPARENT)
        setColorSchemeColors(Color.TRANSPARENT, Color.TRANSPARENT)
    }

    /**
     * Met à jour la condition météo affichée pendant le rafraîchissement.
     * Appelé par le tableau de bord à chaque rendu.
     */
    fun setWeatherCondition(code: Long, isDay: Boolean) {
        currentCode = code
        currentIsDay = isDay
        if (isRefreshing) {
            bindWeatherDrawable(requireIcon())
        }
    }

    override fun setRefreshing(refreshing: Boolean) {
        super.setRefreshing(refreshing)
        if (refreshing) {
            val icon = requireIcon()
            bindWeatherDrawable(icon)
            icon.visibility = VISIBLE
            icon.animate().alpha(1f).setDuration(200L).start()
            startBob(icon)
        } else {
            bobAnimator?.cancel()
            bobAnimator = null
            weatherIcon?.animate()?.alpha(0f)?.withEndAction {
                weatherIcon?.visibility = GONE
            }?.setDuration(200L)?.start()
        }
    }

    private fun requireIcon(): ImageView {
        return weatherIcon ?: createIcon().also { weatherIcon = it }
    }

    private fun createIcon(): ImageView {
        val density = resources.displayMetrics.density
        val icon = ImageView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (44 * density).toInt(),
                (44 * density).toInt(),
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = (16 * density).toInt()
            }
            alpha = 0f
            visibility = GONE
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(icon)
        return icon
    }

    private fun bindWeatherDrawable(icon: ImageView) {
        val res = WeatherIcons.forCondition(context, currentCode, currentIsDay)
        if (res != 0) {
            icon.setImageResource(res)
            (icon.drawable as? android.graphics.drawable.Animatable)?.start()
        }
    }

    private fun startBob(icon: ImageView) {
        bobAnimator?.cancel()
        bobAnimator = ObjectAnimator.ofFloat(icon, "translationY", 0f, 10f, 0f).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    override fun onDetachedFromWindow() {
        bobAnimator?.cancel()
        bobAnimator = null
        super.onDetachedFromWindow()
    }
}
