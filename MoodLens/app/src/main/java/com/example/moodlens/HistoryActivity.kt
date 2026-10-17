package com.example.moodlens

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : MoodLensActivity() {
    private lateinit var container: LinearLayout
    private var filter = "ALL"
    private var request = 0
    private var loaded = 0
    private var loading = false
    private val tabs = mapOf(R.id.filterAll to "ALL", R.id.filterLive to "LIVE", R.id.filterCamera to "CAMERA", R.id.filterGallery to "GALLERY")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        container = findViewById(R.id.historyContainer)
        filter = savedInstanceState?.getString("filter") ?: "ALL"
        bindBack(R.id.btnHistoryBack)
        tabs.forEach { (id, value) -> findViewById<View>(id).setOnClickListener { filter = value; load(reset = true) } }
        findViewById<View>(R.id.btnLoadMore).setOnClickListener { if (!loading) load(reset = false) }
        findViewById<View>(R.id.btnClearHistory).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Clear history?")
                .setMessage("This removes all saved detections from this device.")
                .setNegativeButton("Cancel", null).setPositiveButton("Clear") { _, _ ->
                    DetectionStore.clearAll(this, onComplete = { if (canUpdateUi()) load(true) },
                        onError = { if (canUpdateUi()) showError() })
                }.show()
        }
    }
    override fun onResume() { super.onResume(); load(true) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("filter", filter); super.onSaveInstanceState(outState) }
    private fun load(reset: Boolean) {
        val token = ++request
        loading = true
        tabs.forEach { (id, value) -> findViewById<View>(id).isSelected = value == filter }
        findViewById<View>(R.id.btnLoadMore).isEnabled = false
        if (reset) { loaded = 0; container.removeAllViews(); findViewById<TextView>(R.id.txtHistoryCount).text = "Loading history…" }
        DetectionStore.loadPage(this, filter, loaded, 50,
            onResult = { page ->
                if (token == request && canUpdateUi()) {
                    loading = false
                    page.items.forEach { container.addView(historyCard(it)) }
                    loaded += page.items.size
                    findViewById<TextView>(R.id.txtHistoryCount).text = "$loaded of ${page.count} detections${if (filter == "ALL") "" else " · ${filter.lowercase(Locale.getDefault())}"}"
                    findViewById<TextView>(R.id.txtHistoryEmpty).apply {
                        visibility = if (page.count == 0) View.VISIBLE else View.GONE
                        text = if (filter == "ALL") "No detections yet. Try Live Detection or Photo Analysis." else "No detections for this source yet."
                    }
                    findViewById<View>(R.id.btnLoadMore).apply { visibility = if (loaded < page.count) View.VISIBLE else View.GONE; isEnabled = true }
                }
            }, onError = { if (token == request && canUpdateUi()) { loading = false; showError() } })
    }
    private fun showError() { findViewById<TextView>(R.id.txtHistoryCount).text = "Couldn't load history. Reopen this screen to retry." }
    private fun historyCard(item: DetectionEntity): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(18)); setBackgroundResource(R.drawable.bg_white_card)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        }
        card.addView(TextView(this).apply {
            text = EmotionStyle.emoji(item.emotion); textSize = 30f; gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_soft); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(56), dp(56)))
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(details, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
        details.addView(TextView(this).apply {
            text = item.emotion; textSize = 18f; setTextColor(getColor(R.color.text_primary)); setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        details.addView(TextView(this).apply {
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.timestamp))
            text = "$date\n${DetectionStore.sourceLabel(item.source)}${if (item.targetMatched) " · Target match" else ""}"
            textSize = 12f; setTextColor(getColor(R.color.text_secondary)); setPadding(0, dp(4), 0, dp(4))
        })
        details.addView(TextView(this).apply {
            text = String.format(Locale.getDefault(), "Model score · %.1f%%", item.confidence * 100)
            textSize = 13f; setTextColor(getColor(R.color.purple_main))
        })
        return card
    }
}
