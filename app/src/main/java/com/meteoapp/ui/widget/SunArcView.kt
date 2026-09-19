package com.meteoapp.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Arc solaire : demi-cercle représentant la journée entre le lever et le
 * coucher du soleil. La portion déjà écoulée est tracée en doré avec un
 * balayage animé, et le soleil est positionné à l'heure actuelle.
 */
class SunArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val strokePx = 4f * density
    private val sunRadiusPx = 6f * density
    private val haloRadiusPx = 12f * density

    private var targetProgress = -1f
    private var animatedProgress = 0f
    private var isNight = false
    private var animator: ValueAnimator? = null

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4DFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB703.toInt()
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFB703
    }
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB703.toInt()
    }

    fun setSunTimes(sunriseSeconds: Long, sunsetSeconds: Long, nowSeconds: Long) {
        targetProgress = if (sunsetSeconds > sunriseSeconds) {
            (((nowSeconds - sunriseSeconds).toFloat()) / (sunsetSeconds - sunriseSeconds))
                .coerceIn(0f, 1f)
        } else {
            0f
        }
        isNight = nowSeconds < sunriseSeconds || nowSeconds >= sunsetSeconds
        animator?.cancel()
        val newAnimator = ValueAnimator.ofFloat(0f, targetProgress)
        newAnimator.duration = 800L
        newAnimator.interpolator = DecelerateInterpolator()
        newAnimator.addUpdateListener { animation ->
            animatedProgress = animation.animatedValue as Float
            invalidate()
        }
        newAnimator.start()
        animator = newAnimator
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (targetProgress < 0f) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val baseY = h - haloRadiusPx
        val radius = (min(w / 2f, h) - strokePx - haloRadiusPx).coerceAtLeast(0f)
        if (radius <= 0f) return
        val cx = w / 2f

        val arcRect = RectF(cx - radius, baseY - radius, cx + radius, baseY + radius)
        canvas.drawArc(arcRect, 180f, 180f, false, arcPaint)
        if (animatedProgress > 0f) {
            canvas.drawArc(arcRect, 180f, 180f * animatedProgress, false, progressPaint)
        }

        if (isNight) return
        val angle = Math.toRadians(180.0 + 180.0 * animatedProgress)
        val sx = cx + radius * cos(angle).toFloat()
        val sy = baseY + radius * sin(angle).toFloat()
        canvas.drawCircle(sx, sy, haloRadiusPx, haloPaint)
        canvas.drawCircle(sx, sy, sunRadiusPx, sunPaint)
    }
}
