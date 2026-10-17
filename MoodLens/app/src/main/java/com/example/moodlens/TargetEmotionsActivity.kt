package com.example.moodlens

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatCheckBox

class TargetEmotionsActivity : MoodLensActivity() {
    private val boxes = mutableListOf<CheckBox>()
    private lateinit var summary: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_emotions)
        bindBack(R.id.btnTargetBack)
        summary = findViewById(R.id.txtSelectedTargets)
        val saved = savedInstanceState?.getStringArrayList("selection")?.toSet()
            ?: getSharedPreferences("moodlens", MODE_PRIVATE).getStringSet("targets", emptySet())!!.toSet()
        val container = findViewById<LinearLayout>(R.id.emotionContainer)
        EmotionLabels.ALL.forEach { label ->
            val box = AppCompatCheckBox(this).apply {
                text = "${EmotionStyle.emoji(label)}  $label"
                tag = label
                textSize = 18f
                setTextColor(getColor(R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                minHeight = dp(62)
                setPadding(dp(8), dp(10), dp(8), dp(10))
                buttonTintList = ColorStateList.valueOf(getColor(R.color.purple_main))
                isChecked = saved.contains(label)
                setOnCheckedChangeListener { _, _ -> updateSummary() }
            }
            boxes.add(box); container.addView(box)
        }
        updateSummary()
        findViewById<android.view.View>(R.id.btnSelectAll).setOnClickListener { boxes.forEach { it.isChecked = true } }
        findViewById<android.view.View>(R.id.btnClearTargets).setOnClickListener { boxes.forEach { it.isChecked = false } }
        findViewById<android.view.View>(R.id.btnSaveTargets).setOnClickListener {
            save(); Toast.makeText(this, "Selection applied", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnStartTargetCamera).setOnClickListener {
            if (selection().isEmpty()) { Toast.makeText(this, "Select at least one emotion", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            save()
            startActivity(Intent(this, LiveDetectionActivity::class.java)
                .putExtra(LiveDetectionActivity.EXTRA_MODE, LiveDetectionActivity.MODE_TARGET)
                .putStringArrayListExtra(LiveDetectionActivity.EXTRA_TARGETS, ArrayList(selection())))
        }
    }
    private fun selection(): Set<String> = boxes.filter { it.isChecked }.map { it.tag as String }.toSet()
    private fun save() { getSharedPreferences("moodlens", MODE_PRIVATE).edit().putStringSet("targets", selection()).apply() }
    private fun updateSummary() {
        val selected = selection()
        summary.text = if (selected.isEmpty()) "No target selected" else "${selected.size} selected · ${selected.joinToString(", ")}"
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("selection", ArrayList(selection())); super.onSaveInstanceState(outState)
    }
}
