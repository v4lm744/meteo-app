package com.meteoapp.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Barre de plage de température façon Google Météo : un segment en dégradé
 * (bleu froid vers orange/rouge chaud) positionné entre le minimum et le
 * maximum de la semaine, avec un point blanc pour la température actuelle
 * sur la ligne du jour.
 */
class TempRangeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val trackHeightPx = 4f * density
    private val dotRadiusPx = 3.5f * density

    private var weekMin = 0.0
    private var weekMax = 1.0
    private var valueMin = 0.0
    private var valueMax = 1.0
    private var currentTemp: Double? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private var shaderStartX = -1f
    private var shaderEndX = -1f

    private var dotAnimator: android.animation.ValueAnimator? = null
    private var animatedCurrentTemp: Double? = null

    fun update(
        valueMin: Double,
        valueMax: Double,
        weekMin: Double,
        weekMax: Double,
        currentTemp: Double?
    ) {
        this.valueMin = valueMin
        this.valueMax = valueMax
        this.weekMin = weekMin
        this.weekMax = weekMax
        shaderStartX = -1f
        shaderEndX = -1f

        val previous = animatedCurrentTemp ?: currentTemp
        animatedCurrentTemp = currentTemp
        if (currentTemp == null || previous == null || previous == currentTemp) {
            invalidate()
            return
        }
        dotAnimator?.cancel()
        dotAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700L
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                animatedCurrentTemp = previous + (currentTemp - previous) * fraction
                invalidate()
            }
            start()
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        dotAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val top = (h - trackHeightPx) / 2f
        val bottom = top + trackHeightPx
        val radius = trackHeightPx / 2f

        canvas.drawRoundRect(0f, top, w, bottom, radius, radius, trackPaint)

        val span = (weekMax - weekMin).coerceAtLeast(1.0)
        val startFraction = (((valueMin - weekMin) / span).coerceIn(0.0, 1.0)).toFloat()
        val endFraction = (((valueMax - weekMin) / span).coerceIn(0.0, 1.0)).toFloat()
        if (endFraction <= startFraction) return

        val startX = startFraction * w
        val endX = endFraction * w
        if (shaderStartX != startX || shaderEndX != endX) {
            shaderStartX = startX
            shaderEndX = endX
            barPaint.shader = LinearGradient(
                startX, 0f, endX, 0f,
                colorFor(valueMin), colorFor(valueMax), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(startX, top, endX, bottom, radius, radius, barPaint)

        currentTemp?.let { _ ->
            val temp = animatedCurrentTemp ?: return
            val fraction = (((temp - weekMin) / span).coerceIn(0.0, 1.0)).toFloat()
            canvas.drawCircle(fraction * w, h / 2f, dotRadiusPx, dotPaint)
        }
    }

    private fun colorFor(temp: Double): Int {
        val t = ((temp + 10.0) / 45.0).coerceIn(0.0, 1.0)
        val (from, to, local) = when {
            t < 0.5 -> Triple(intArrayOf(0x5A, 0xA9, 0xE6), intArrayOf(0xFF, 0xB7, 0x03), (t / 0.5).toFloat())
            else -> Triple(intArrayOf(0xFF, 0xB7, 0x03), intArrayOf(0xE6, 0x39, 0x46), ((t - 0.5) / 0.5).toFloat())
        }
        val r = lerp(from[0], to[0], local)
        val g = lerp(from[1], to[1], local)
        val b = lerp(from[2], to[2], local)
        return Color.argb(0xFF, r, g, b)
    }

    private fun lerp(from: Int, to: Int, fraction: Float): Int =
        (from + (to - from) * fraction).toInt()
}
