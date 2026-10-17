package com.example.moodlens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.min

class EmotionPieChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val fontScale = resources.displayMetrics.scaledDensity
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#121827") }
    private var data: List<Pair<String, Int>> = emptyList()
    fun setData(values: List<Pair<String, Int>>) {
        data = values.filter { it.second > 0 }
        val total = data.sumOf { it.second }
        contentDescription = if (total == 0) "No emotion data" else "Emotion distribution: " + data.joinToString { "${it.first}: ${it.second}" }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = data.sumOf { it.second }
        text.textSize = 15 * fontScale; text.textAlign = Paint.Align.CENTER
        if (total == 0) { canvas.drawText("No emotion data yet", width / 2f, height / 2f, text); return }
        val rowHeight = maxOf(24 * density, 20 * fontScale)
        val available = height - rowHeight * data.size - 38 * density
        val size = min(min(width * .72f, 190 * density), available).coerceAtLeast(40 * density)
        val rect = RectF((width - size) / 2, 14 * density, (width + size) / 2, size + 14 * density)
        var angle = -90f
        data.forEach { (label, count) ->
            val sweep = 360f * count / total
            fill.color = EmotionStyle.color(label)
            canvas.drawArc(rect, angle, sweep, true, fill); angle += sweep
        }
        fill.color = Color.WHITE; canvas.drawCircle(rect.centerX(), rect.centerY(), size * .31f, fill)
        text.typeface = android.graphics.Typeface.DEFAULT_BOLD; text.textSize = 25 * fontScale
        canvas.drawText(total.toString(), rect.centerX(), rect.centerY() + 4 * density, text)
        text.typeface = android.graphics.Typeface.DEFAULT; text.textSize = 12 * fontScale
        canvas.drawText("detections", rect.centerX(), rect.centerY() + 23 * density, text)
        text.textSize = 13 * fontScale
        val top = rect.bottom + 24 * density
        data.forEachIndexed { index, (label, count) ->
            val y = top + index * rowHeight
            fill.color = EmotionStyle.color(label); canvas.drawCircle(12 * density, y - 4 * density, 5 * density, fill)
            text.textAlign = Paint.Align.LEFT
            canvas.drawText(label, 26 * density, y, text)
            text.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(Locale.getDefault(), "%d · %.0f%%", count, count * 100f / total), width - 8 * density, y, text)
        }
    }
}
