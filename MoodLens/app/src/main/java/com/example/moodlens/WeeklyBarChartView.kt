package com.example.moodlens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(91, 80, 242)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(225, 221, 218)
        strokeWidth = density
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(92, 84, 81)
        textSize = 11f * scaledDensity
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(45, 39, 37)
        textSize = 11f * scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private var data: List<Pair<String, Int>> = emptyList()

    fun setData(values: List<Pair<String, Int>>) {
        data = values
        contentDescription = "Daily detection totals: " + values.joinToString { "${it.first}: ${it.second}" }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            canvas.drawText(
                "No weekly data yet",
                width / 2f,
                height / 2f,
                labelPaint
            )
            return
        }

        val left = 18f * density
        val right = width - 12f * density
        val top = 24f * density
        val bottom = height - 38f * density
        val chartHeight = (bottom - top).coerceAtLeast(1f)
        val chartWidth = (right - left).coerceAtLeast(1f)

        canvas.drawLine(left, bottom, right, bottom, gridPaint)
        canvas.drawLine(left, top, left, bottom, gridPaint)

        val maxValue = max(1, data.maxOfOrNull { it.second } ?: 1)
        val slotWidth = chartWidth / data.size.toFloat()
        val barWidth = slotWidth * 0.58f

        data.forEachIndexed { index, item ->
            val centerX = left + slotWidth * index + slotWidth / 2f
            val barHeight = chartHeight * item.second.toFloat() / maxValue.toFloat()
            val barLeft = centerX - barWidth / 2f
            val barTop = bottom - barHeight
            val barRight = centerX + barWidth / 2f

            canvas.drawRoundRect(
                barLeft,
                barTop,
                barRight,
                bottom,
                6f * density,
                6f * density,
                barPaint
            )

            canvas.drawText(
                item.second.toString(),
                centerX,
                (barTop - 7f * density).coerceAtLeast(top + 10f * density),
                valuePaint
            )

            canvas.drawText(
                item.first,
                centerX,
                bottom + 22f * density,
                labelPaint
            )
        }
    }
}
