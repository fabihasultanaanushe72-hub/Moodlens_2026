package com.example.moodlens

import android.graphics.Color

object EmotionStyle {
    fun color(label: String): Int = Color.parseColor(when (label.lowercase(java.util.Locale.US)) {
        "happy" -> "#FFC125"
        "sad" -> "#3F83F8"
        "angry" -> "#EF4444"
        "fear" -> "#925AF3"
        "surprise" -> "#FF8C23"
        "disgust" -> "#20BA6D"
        else -> "#94A3B8"
    })
    fun emoji(label: String): String = when (label.lowercase(java.util.Locale.US)) {
        "happy" -> "🙂"; "sad" -> "☹️"; "angry" -> "😠"; "fear" -> "😨"
        "surprise" -> "😮"; "disgust" -> "🤢"; else -> "😐"
    }
}
