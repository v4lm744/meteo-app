package com.meteoapp.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Boussole du vent : rose des vents minimale (N/E/S/O) avec une flèche
 * indiquant la direction D'OÙ vient le vent (convention météo : windDeg
 * pointe vers la source). La vitesse est affichée au centre.
 */
class WindCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private var windDeg = 0L
    private var speedLabel: String = ""

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0x33FFFFFF
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
        color = 0x66FFFFFF.toInt()
    }

    private val cardinalsOn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
        textAlign = Paint.Align.CENTER
        color = 0xCCFFFFFF.toInt()
        isFakeBoldText = true
    }

    private val cardinalsOff = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        color = 0x80FFFFFF.toInt()
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFD166.toInt()
    }

    private val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
    }

    private val circleRect = RectF()

    fun setWind(degrees: Long, speedText: String) {
        windDeg = ((degrees % 360) + 360) % 360
        speedLabel = speedText
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = (minOf(w, h) / 2f) - 4f * density
        circleRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawCircle(cx, cy, radius, circlePaint)

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45f).toDouble())
            val isMajor = i % 2 == 0
            val inner = radius * (if (isMajor) 0.82f else 0.88f)
            val outer = radius * 0.94f
            tickPaint.alpha = if (isMajor) 160 else 90
            canvas.drawLine(
                cx + radius * cos(angle).toFloat() * (inner / radius),
                cy + radius * sin(angle).toFloat() * (inner / radius),
                cx + radius * cos(angle).toFloat() * (outer / radius),
                cy + radius * sin(angle).toFloat() * (outer / radius),
                tickPaint
            )
        }

        val cardinalOffset = radius + 10f * density
        val westLabel = if (resources.configuration.locales[0].language == "fr") "O" else "W"
        canvas.drawText("N", cx, cy - cardinalOffset + cardinalsOn.ascent() * -0.4f, cardinalsOn)
        canvas.drawText("E", cx + cardinalOffset, cy + cardinalsOff.textSize * 0.35f, cardinalsOff)
        canvas.drawText("S", cx, cy + cardinalOffset + cardinalsOff.textSize * 0.3f, cardinalsOff)
        canvas.drawText(westLabel, cx - cardinalOffset, cy + cardinalsOff.textSize * 0.35f, cardinalsOff)

        val angleRad = Math.toRadians(windDeg.toDouble() - 90.0)
        val arrowLen = radius * 0.62f
        val tipX = cx + arrowLen * cos(angleRad).toFloat()
        val tipY = cy + arrowLen * sin(angleRad).toFloat()
        val tailX = cx - arrowLen * cos(angleRad).toFloat()
        val tailY = cy - arrowLen * sin(angleRad).toFloat()
        canvas.drawCircle(tailX, tailY, 3.5f * density, arrowPaint)
        canvas.drawLine(tailX, tailY, tipX, tipY, arrowPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            strokeCap = Paint.Cap.ROUND
        })
        arrowPaint.style = Paint.Style.FILL

        if (speedLabel.isNotEmpty()) {
            canvas.drawText(speedLabel, cx, cy + speedPaint.textSize * 0.35f, speedPaint)
        }
    }
}
