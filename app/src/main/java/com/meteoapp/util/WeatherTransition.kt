package com.meteoapp.util

import android.animation.ValueAnimator
import android.widget.TextView
import com.meteoapp.R
import java.util.WeakHashMap

/**
 * Animations de transition des valeurs météo lors d'un rafraîchissement :
 * la température principale compte de l'ancienne vers la nouvelle valeur
 * (effet « Google Météo »), au lieu de changer d'un coup.
 */
object WeatherTransition {

    private const val DURATION_MS = 900L

    private val previousAnimators = WeakHashMap<TextView, ValueAnimator>()

    fun animateTemperature(
        textView: TextView,
        formatter: (Double) -> String,
        target: Double
    ) {
        previousAnimators[textView]?.cancel()
        val start = textView.getTag(R.id.tag_last_temp) as? Double
        if (start == null || start == target) {
            textView.text = formatter(target)
            textView.setTag(R.id.tag_last_temp, target)
            return
        }
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION_MS
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                val value = start + (target - start) * fraction
                textView.text = formatter(value)
            }
        }
        animator.start()
        previousAnimators[textView] = animator
        textView.setTag(R.id.tag_last_temp, target)
    }
}
