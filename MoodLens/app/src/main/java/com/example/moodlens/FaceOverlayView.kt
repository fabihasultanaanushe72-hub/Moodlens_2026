package com.example.moodlens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class FaceResult(
        val bounds: Rect,
        val label: String,
        val confidence: Float,
        val targetMatched: Boolean
    )

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 15f * scaledDensity
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var results: List<FaceResult> = emptyList()
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var targetMode = false
    private var fitCenter = false

    fun setResults(
        results: List<FaceResult>,
        sourceWidth: Int,
        sourceHeight: Int,
        targetMode: Boolean,
        fitCenter: Boolean = false
    ) {
        this.results = results
        this.sourceWidth = sourceWidth
        this.sourceHeight = sourceHeight
        this.targetMode = targetMode
        this.fitCenter = fitCenter
        contentDescription = results.joinToString { "${it.label}: ${(it.confidence * 100).toInt()} percent" }
        invalidate()
    }

    fun clear() {
        results = emptyList()
        contentDescription = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (sourceWidth <= 0 || sourceHeight <= 0 || results.isEmpty()) {
            return
        }

        val scaleX = width.toFloat() / sourceWidth
        val scaleY = height.toFloat() / sourceHeight
        val scale = if (fitCenter) min(scaleX, scaleY) else max(scaleX, scaleY)

        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale
        val offsetX = (width - scaledWidth) / 2f
        val offsetY = (height - scaledHeight) / 2f

        for (result in results) {
            val color = if (targetMode) Color.rgb(19, 181, 164) else EmotionStyle.color(result.label)
            boxPaint.color = color
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            hsv[2] = min(hsv[2], 0.65f)
            labelBackgroundPaint.color = Color.HSVToColor(hsv)

            val mappedRect = RectF(
                result.bounds.left * scale + offsetX,
                result.bounds.top * scale + offsetY,
                result.bounds.right * scale + offsetX,
                result.bounds.bottom * scale + offsetY
            )

            canvas.drawRoundRect(
                mappedRect,
                12f * density,
                12f * density,
                boxPaint
            )

            val prefix = if (targetMode) "TARGET • " else ""
            val label = String.format(
                Locale.US,
                "%s%s %.1f%%",
                prefix,
                result.label,
                result.confidence * 100f
            )

            val paddingX = 10f * density
            val paddingY = 7f * density
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize

            val labelWidth = (textWidth + paddingX * 2f).coerceAtMost(width.toFloat())
            val labelLeft = mappedRect.left.coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
            val preferredBottom = mappedRect.top - 7f * density
            val minimumBottom = textHeight + paddingY * 2f
            val labelBottom = preferredBottom.coerceAtLeast(minimumBottom)
            val labelTop = labelBottom - textHeight - paddingY * 2f
            val labelRight = (labelLeft + textWidth + paddingX * 2f)
                .coerceAtMost(width.toFloat())

            canvas.drawRoundRect(
                RectF(labelLeft, labelTop, labelRight, labelBottom),
                8f * density,
                8f * density,
                labelBackgroundPaint
            )

            canvas.drawText(
                label,
                labelLeft + paddingX,
                labelBottom - paddingY,
                textPaint
            )
        }
    }
}
