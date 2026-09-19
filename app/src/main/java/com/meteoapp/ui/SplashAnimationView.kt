package com.meteoapp.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.meteoapp.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Écran de lancement animé sur le thème météo : dégradé de ciel, soleil qui se
 * lève avec des rayons pivotants, nuages dérivants en parallaxe et apparition
 * en fondu du nom de l'application. Le dessin est entièrement fait sur le
 * Canvas (aucune dépendance externe) pour rester fluide et léger.
 */
class SplashAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface AnimationListener {
        fun onAnimationEnd()
    }

    var listener: AnimationListener? = null

    private val colorDeep = ContextCompat.getColor(context, R.color.md_blue_deep)
    private val colorMain = ContextCompat.getColor(context, R.color.md_blue_main)
    private val colorSky = ContextCompat.getColor(context, R.color.md_blue_sky)
    private val colorSun = ContextCompat.getColor(context, R.color.md_sun)
    private val colorWhite = ContextCompat.getColor(context, R.color.md_white)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    private val textSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }

    private val cloudPath = Path()

    private data class Cloud(
        val baseY: Float,
        val scale: Float,
        val speed: Float,
        val startOffset: Float,
        val alpha: Int
    )

    private val clouds = listOf(
        Cloud(0.30f, 1.0f, 0.16f, 0.00f, 220),
        Cloud(0.22f, 0.7f, 0.10f, 0.35f, 200),
        Cloud(0.42f, 1.25f, 0.20f, 0.65f, 180),
        Cloud(0.16f, 0.55f, 0.07f, 0.20f, 170)
    )

    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val durationMs = 2600L

    fun start() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { a ->
                progress = a.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    listener?.onAnimationEnd()
                }
            })
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textPaint.textSize = (w * 0.10f).coerceAtMost(64f * resources.displayMetrics.density)
        textSubPaint.textSize = (w * 0.035f).coerceAtMost(22f * resources.displayMetrics.density)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawBackground(canvas, w, h)
        drawSun(canvas, w, h)
        drawRays(canvas, w, h)
        drawClouds(canvas, w, h)
        drawTitle(canvas, w, h)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        // Le dégradé s'éclaire légèrement au fur et à mesure que le soleil monte.
        val lighten = progress * 0.5f
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                blend(colorDeep, colorMain, lighten * 0.6f),
                blend(colorMain, colorSky, lighten),
                blend(colorSky, colorSky, lighten)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
    }

    private fun drawSun(canvas: Canvas, w: Float, h: Float) {
        val p = progress
        val maxRadius = w.coerceAtMost(h) * 0.13f
        val radius = maxRadius * (0.55f + 0.45f * easeOut(p))
        // Le soleil se lève depuis le bas de l'écran vers le tiers supérieur.
        val startY = h + maxRadius
        val endY = h * 0.30f
        val cx = w * 0.5f
        val cy = lerp(startY, endY, easeOut(p))

        // Halo lumineux.
        val glowRadius = radius * 3.2f * (0.6f + 0.4f * p)
        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                withAlpha(colorSun, (90 * p).toInt().coerceIn(0, 255)),
                withAlpha(colorSun, 0)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)

        // Disque solaire.
        sunPaint.color = colorSun
        sunPaint.alpha = (255 * (0.3f + 0.7f * p)).toInt().coerceIn(0, 255)
        canvas.drawCircle(cx, cy, radius, sunPaint)
    }

    private fun drawRays(canvas: Canvas, w: Float, h: Float) {
        val p = progress
        if (p < 0.05f) return
        val cx = w * 0.5f
        val cy = h * 0.30f
        val maxRadius = w.coerceAtMost(h) * 0.13f
        val inner = maxRadius * (0.6f + 0.45f * easeOut(p)) * 1.15f
        val rayCount = 12
        // Les rayons pivotent et s'étendent progressivement.
        val rotation = p * 0.6f
        val reach = (maxRadius * 1.1f) * easeOut(p)
        rayPaint.color = colorSun
        rayPaint.strokeWidth = w * 0.012f
        rayPaint.strokeCap = Paint.Cap.ROUND
        rayPaint.alpha = (220 * p).toInt().coerceIn(0, 255)
        for (i in 0 until rayCount) {
            val angle = rotation + (i.toFloat() / rayCount) * (2f * Math.PI.toFloat())
            val sx = cx + cos(angle) * inner
            val sy = cy + sin(angle) * inner
            val ex = cx + cos(angle) * (inner + reach)
            val ey = cy + sin(angle) * (inner + reach)
            canvas.drawLine(sx, sy, ex, ey, rayPaint)
        }
    }

    private fun drawClouds(canvas: Canvas, w: Float, h: Float) {
        val p = progress
        // Les nuages apparaissent en fondu et dérivent vers la droite.
        val alphaFactor = (p * 2f).coerceAtMost(1f)
        for (cloud in clouds) {
            val drift = ((p + cloud.startOffset) * cloud.speed * 2f) % 1.4f
            val x = -w * 0.3f + drift * w * 1.6f
            val y = h * cloud.baseY
            val scale = w * 0.16f * cloud.scale
            cloudPaint.color = colorWhite
            cloudPaint.alpha = ((cloud.alpha * alphaFactor).toInt()).coerceIn(0, 255)
            canvas.drawPath(buildCloudPath(x, y, scale), cloudPaint)
        }
    }

    private fun buildCloudPath(cx: Float, cy: Float, scale: Float): Path {
        cloudPath.reset()
        cloudPath.addCircle(cx, cy, scale * 0.5f, Path.Direction.CW)
        cloudPath.addCircle(cx + scale * 0.45f, cy + scale * 0.08f, scale * 0.42f, Path.Direction.CW)
        cloudPath.addCircle(cx - scale * 0.45f, cy + scale * 0.08f, scale * 0.38f, Path.Direction.CW)
        cloudPath.addCircle(cx + scale * 0.12f, cy - scale * 0.22f, scale * 0.34f, Path.Direction.CW)
        cloudPath.addRect(cx - scale * 0.85f, cy + scale * 0.08f, cx + scale * 0.85f, cy + scale * 0.45f, Path.Direction.CW)
        return cloudPath
    }

    private fun drawTitle(canvas: Canvas, w: Float, h: Float) {
        val p = progress
        // Le titre apparaît en fondu + léger zoom après la moitié de l'animation.
        val titleP = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (titleP <= 0f) return
        val alpha = (255 * easeOut(titleP)).toInt().coerceIn(0, 255)
        val scale = 0.85f + 0.15f * easeOut(titleP)
        val cy = h * 0.62f
        canvas.save()
        canvas.scale(scale, scale, w * 0.5f, cy)
        textPaint.color = withAlpha(colorWhite, alpha)
        canvas.drawText(resources.getString(R.string.app_name), w * 0.5f, cy, textPaint)
        textSubPaint.color = withAlpha(colorWhite, (alpha * 0.7f).toInt().coerceIn(0, 255))
        canvas.drawText(resources.getString(R.string.splash_tagline), w * 0.5f, cy + textPaint.textSize * 0.55f, textSubPaint)
        canvas.restore()
    }

    private fun easeOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return 1f - (1f - x) * (1f - x)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun blend(c1: Int, c2: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        val r = (lerp((c1 shr 16 and 0xFF).toFloat(), (c2 shr 16 and 0xFF).toFloat(), tt)).toInt()
        val g = (lerp((c1 shr 8 and 0xFF).toFloat(), (c2 shr 8 and 0xFF).toFloat(), tt)).toInt()
        val b = (lerp((c1 and 0xFF).toFloat(), (c2 and 0xFF).toFloat(), tt)).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
    }
}
