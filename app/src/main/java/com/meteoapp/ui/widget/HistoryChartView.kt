package com.meteoapp.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.meteoapp.stats.DailyRecord
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Graphe des températures des 7 derniers jours : bande min/max par jour
 * (remplie) + courbes min et max, avec repères de jours de la semaine.
 */
class HistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var records: List<DailyRecord> = emptyList()

    private val maxPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFFFF7043.toInt()
    }

    private val minPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF4FC3F7.toInt()
    }

    private val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = 0x334FC3F7
    }

    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0x22FFFFFF
    }

    private val labelPaint = Paint().apply {
        isAntiAlias = true
        textSize = 24f
        color = 0xCCFFFFFF.toInt()
    }

    private val dayNames = arrayOf("dim", "lun", "mar", "mer", "jeu", "ven", "sam")

    private val maxPath = Path()
    private val minPath = Path()
    private val fillPath = Path()

    fun submit(newRecords: List<DailyRecord>) {
        records = newRecords
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (records.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padTop = 16f
        val labelSpace = 40f
        val chartH = h - padTop - labelSpace

        val minTemp = records.minOf { it.tempMin }
        val maxTemp = records.maxOf { it.tempMax }
        val span = max(1.0, maxTemp - minTemp)

        fun xAt(index: Int): Float =
            w * (index + 0.5f) / records.size

        fun yAt(temp: Double): Float =
            padTop + chartH * (1.0 - (temp - minTemp) / span).toFloat()

        val step = chartH / 2
        for (i in 0..2) {
            val y = padTop + i * step
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        maxPath.reset()
        minPath.reset()
        records.forEachIndexed { i, r ->
            val x = xAt(i)
            val yMax = yAt(r.tempMax)
            val yMin = yAt(r.tempMin)
            if (i == 0) {
                maxPath.moveTo(x, yMax)
                minPath.moveTo(x, yMin)
            } else {
                maxPath.lineTo(x, yMax)
                minPath.lineTo(x, yMin)
            }
        }
        fillPath.reset()
        fillPath.addPath(minPath)
        for (i in records.indices.reversed()) {
            fillPath.lineTo(xAt(i), yAt(records[i].tempMax))
        }
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(maxPath, maxPaint)
        canvas.drawPath(minPath, minPaint)

        records.forEachIndexed { i, r ->
            val dayOfWeek = Instant.ofEpochSecond(r.dayKey).atZone(ZoneOffset.UTC).dayOfWeek
            val label = when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> dayNames[1]
                java.time.DayOfWeek.TUESDAY -> dayNames[2]
                java.time.DayOfWeek.WEDNESDAY -> dayNames[3]
                java.time.DayOfWeek.THURSDAY -> dayNames[4]
                java.time.DayOfWeek.FRIDAY -> dayNames[5]
                java.time.DayOfWeek.SATURDAY -> dayNames[6]
                java.time.DayOfWeek.SUNDAY -> dayNames[0]
            }
            val x = min(w - 20f, max(20f, xAt(i)))
            canvas.drawText(label, x - labelPaint.measureText(label) / 2, h - 8f, labelPaint)
        }
    }
}
