package com.example.moodlens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

open class MoodLensActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        val content = findViewById<android.widget.FrameLayout>(android.R.id.content)
        if (this is MainActivity || this is HistoryActivity || this is AnalyticsActivity) {
            val screen = content.getChildAt(0)
            content.removeView(screen)
            val shell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.color.background)
            }
            shell.addView(screen, LinearLayout.LayoutParams(-1, 0, 1f))
            val nav = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(getColor(R.color.card_white))
                elevation = dp(4).toFloat()
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            val destinations = listOf(
                Triple("Home", R.drawable.ic_home, MainActivity::class.java),
                Triple("History", R.drawable.ic_history, HistoryActivity::class.java),
                Triple("Analytics", R.drawable.ic_analytics, AnalyticsActivity::class.java)
            )
            destinations.forEach { (label, icon, destination) ->
                val selected = javaClass == destination
                nav.addView(TextView(this).apply {
                    text = label
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    minHeight = dp(54)
                    compoundDrawablePadding = dp(4)
                    val color = getColor(if (selected) R.color.purple_main else R.color.text_secondary)
                    setTextColor(color)
                    val drawable = androidx.core.content.ContextCompat.getDrawable(this@MoodLensActivity, icon)!!.mutate()
                    drawable.setTint(color)
                    drawable.setBounds(0, 0, dp(24), dp(24))
                    setCompoundDrawables(null, drawable, null, null)
                    if (selected) setBackgroundResource(R.drawable.bg_soft)
                    isClickable = true
                    isFocusable = true
                    contentDescription = if (selected) "$label, current screen" else label
                    setOnClickListener {
                        if (!selected) startActivity(Intent(this@MoodLensActivity, destination).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            shell.addView(nav)
            content.addView(shell)
        }
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    protected fun canUpdateUi(): Boolean = !isFinishing && !isDestroyed
    protected fun bindBack(id: Int) { findViewById<View>(id).setOnClickListener { finish() } }
}
