package com.meteoapp.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Animations d'entrée en cascade façon Google Météo : chaque bloc de
 * contenu apparaît en fondu + translation verticale douce, décalé de
 * 60 ms par bloc, avec un interpolateur décélérant.
 */
object EntranceAnimator {

    private const val STAGGER_MS = 60L
    private const val DURATION_MS = 350L
    private const val TRANSLATION_DP = 24f

    /**
     * Applique l'animation d'entrée aux vues fournies dans l'ordre.
     * Les vues sont remises à l'état initial avant de rejouer.
     */
    fun cascade(vararg views: View) {
        var delay = 0L
        for (view in views) {
            if (view.visibility != View.VISIBLE) continue
            view.alpha = 0f
            view.translationY = TRANSLATION_DP
            val fadeIn = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            val slide = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, TRANSLATION_DP, 0f)
            val set = AnimatorSet().apply {
                playTogether(fadeIn, slide)
                duration = DURATION_MS
                startDelay = delay
                interpolator = DecelerateInterpolator()
            }
            set.start()
            delay += STAGGER_MS
        }
    }
}
