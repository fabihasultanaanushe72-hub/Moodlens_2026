package com.example.moodlens

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsActivity : MoodLensActivity() {

    private var days = 0
    private var request = 0
    private val tabs = mapOf(R.id.periodAll to 0, R.id.periodWeek to 7, R.id.periodMonth to 30)
    private lateinit var totalText: TextView
    private lateinit var commonText: TextView
    private lateinit var averageText: TextView
    private lateinit var emptyText: TextView
    private lateinit var sourceContainer: LinearLayout
    private lateinit var emotionPieChart: EmotionPieChartView
    private lateinit var weeklyBarChart: WeeklyBarChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        totalText = findViewById(R.id.txtTotalDetections)
        commonText = findViewById(R.id.txtMostCommon)
        averageText = findViewById(R.id.txtAverageConfidence)
        emptyText = findViewById(R.id.txtAnalyticsEmpty)
        sourceContainer = findViewById(R.id.sourceAnalyticsContainer)
        emotionPieChart = findViewById(R.id.emotionPieChart)
        weeklyBarChart = findViewById(R.id.weeklyBarChart)

        bindBack(R.id.btnAnalyticsBack)
        days = savedInstanceState?.getInt("days") ?: 0
        tabs.forEach { (id, period) -> findViewById<View>(id).setOnClickListener { days = period; loadAnalytics() } }
    }

    override fun onResume() {
        super.onResume()
        loadAnalytics()
    }

    private fun loadAnalytics() {
        val token = ++request
        tabs.forEach { (id, period) -> findViewById<View>(id).isSelected = days == period }
        val cutoff = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }.timeInMillis
        DetectionStore.loadAll(this, onError = {
            if (token == request && canUpdateUi()) {
                emptyText.visibility = View.VISIBLE; emptyText.text = "Couldn't load analytics. Reopen this screen to retry."
            }
        }) { allItems ->
            if (token != request || !canUpdateUi()) return@loadAll
            val items = if (days == 0) allItems else allItems.filter { it.timestamp >= cutoff }
            emptyText.text = "No detections in this period yet."
            sourceContainer.removeAllViews()

            if (items.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                totalText.text = "0"
                commonText.text = "—"
                averageText.text = "—"
                emotionPieChart.setData(emptyList())
                weeklyBarChart.setData(buildLastSevenDayCounts(emptyList()))
                return@loadAll
            }

            emptyText.visibility = View.GONE

            val total = items.size
            val averageConfidence = items.map { it.confidence }.average() * 100.0
            val emotionCounts = items.groupingBy { it.emotion }.eachCount()
            val sourceCounts = items.groupingBy { it.source }.eachCount()
            val mostCommon = emotionCounts.maxByOrNull { it.value }?.key ?: "—"

            totalText.text = total.toString()
            commonText.text = mostCommon
            averageText.text = String.format(
                Locale.US,
                "%.1f%%",
                averageConfidence
            )

            val emotionData = EmotionLabels.ALL.map { emotion ->
                val count = emotionCounts.entries
                    .firstOrNull { it.key.equals(emotion, ignoreCase = true) }
                    ?.value ?: 0
                emotion to count
            }
            emotionPieChart.setData(emotionData)

            val maxSourceCount = sourceCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            sourceCounts.toList()
                .sortedByDescending { it.second }
                .forEach { (source, count) ->
                    addBarRow(
                        container = sourceContainer,
                        label = sourceLabel(source),
                        count = count,
                        max = maxSourceCount
                    )
                }

            weeklyBarChart.setData(buildLastSevenDayCounts(items))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("days", days); super.onSaveInstanceState(outState) }

    private fun sourceLabel(source: String): String {
        return when (source.uppercase(Locale.US)) {
            "LIVE" -> "Live Detection"
            "TARGET" -> "Target Camera"
            "GALLERY" -> "Gallery Photo"
            "CAMERA", "CAMERA PHOTO" -> "Camera Photo"
            "PHOTO" -> "Photo Analysis"
            else -> source
        }
    }

    private fun addBarRow(
        container: LinearLayout,
        label: String,
        count: Int,
        max: Int
    ) {
        val density = resources.displayMetrics.density

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val labelText = TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(getColor(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val countText = TextView(this).apply {
            text = count.toString()
            textSize = 15f
            setTextColor(getColor(R.color.text_secondary))
        }

        val progress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            this.max = max
            this.progress = count
            progressTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.purple_main))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (12 * density).toInt()
            ).apply {
                topMargin = (6 * density).toInt()
            }
        }

        topRow.addView(labelText)
        topRow.addView(countText)
        wrapper.addView(topRow)
        wrapper.addView(progress)
        container.addView(wrapper)
    }

    private fun buildLastSevenDayCounts(
        items: List<DetectionEntity>
    ): List<Pair<String, Int>> {
        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val counts = items.groupingBy {
            dateKeyFormat.format(Date(it.timestamp))
        }.eachCount()

        val calendar = Calendar.getInstance()
        val days = mutableListOf<Pair<String, Int>>()

        repeat(7) {
            val date = calendar.time
            val key = dateKeyFormat.format(date)
            days.add(displayFormat.format(date) to (counts[key] ?: 0))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return days.reversed()
    }
}
