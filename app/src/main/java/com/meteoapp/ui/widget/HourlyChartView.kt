package com.meteoapp.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.meteoapp.data.model.HourlyData
import com.meteoapp.util.WeatherUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Graphique des 24 prochaines heures : courbe des températures + barres de
 * probabilité de précipitations en bas. Dessin canvas sans dépendance,
 * dans l'esprit de [HistoryChartView].
 */
class HourlyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var hours: List<HourlyData> = emptyList()

    private val density = resources.displayMetrics.density

    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFFFFD166.toInt()
    }

    private val tempFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x26FFD166
    }

    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF4FC3F7.toInt()
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x22FFFFFF
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density
        color = 0xCCFFFFFF.toInt()
    }

    private val tempPath = Path()
    private val fillPath = Path()

    fun submit(newHours: List<HourlyData>) {
        hours = newHours.take(CHART_HOURS)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hours.size < 2) return
        val w = width.toFloat()
        val h = height.toFloat()
        val padTop = 4f * density
        val padBottom = 16f * density
        val rainBandH = h * 0.22f
        val chartBottom = h - padBottom - rainBandH
        val chartH = (chartBottom - padTop).coerceAtLeast(1f)

        val minTemp = hours.minOf { it.temp }
        val maxTemp = hours.maxOf { it.temp }
        val span = max(1.0, maxTemp - minTemp)

        fun xAt(i: Int): Float = w * i / (hours.size - 1)
        fun yAt(temp: Double): Float = padTop + chartH * (1.0 - (temp - minTemp) / span).toFloat()

        for (i in 1..2) {
            val y = padTop + chartH * i / 3f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        val rainBase = h - padBottom
        val barW = (w / hours.size) * 0.5f
        hours.forEachIndexed { i, hour ->
            val pop = (hour.pop ?: 0.0).coerceIn(0.0, 1.0)
            if (pop >= 0.05) {
                val barH = rainBandH * pop.toFloat()
                val cx = xAt(i)
                canvas.drawRoundRect(
                    cx - barW / 2, rainBase - barH, cx + barW / 2, rainBase,
                    barW / 4, barW / 4, rainPaint
                )
            }
        }

        tempPath.reset()
        hours.forEachIndexed { i, hour ->
            val x = xAt(i)
            val y = yAt(hour.temp)
            if (i == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
        }
        fillPath.reset()
        fillPath.set(tempPath)
        fillPath.lineTo(xAt(hours.size - 1), chartBottom)
        fillPath.lineTo(xAt(0), chartBottom)
        fillPath.close()
        canvas.drawPath(fillPath, tempFillPaint)
        canvas.drawPath(tempPath, tempPaint)

        labelPaint.textSize = 11f * density
        val first = hours.first()
        val labelStart = WeatherUtils.formatHour(context, first.dt, first.timezoneOffset)
        canvas.drawText(labelStart, 2f * density, h - 4f * density, labelPaint)
        val last = hours.last()
        val labelEnd = WeatherUtils.formatHour(context, last.dt, last.timezoneOffset)
        val endW = labelPaint.measureText(labelEnd)
        canvas.drawText(labelEnd, w - endW - 2f * density, h - 4f * density, labelPaint)
    }


    companion object {
        private const val CHART_HOURS = 24
    }
}
