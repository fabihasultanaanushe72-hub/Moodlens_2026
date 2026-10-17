package com.example.moodlens

import kotlin.math.exp

object EmotionMath {
    fun softmax(logits: FloatArray): FloatArray {
        require(logits.isNotEmpty() && logits.all { it.isFinite() })
        val peak = logits.maxOrNull()!!
        val exps = logits.map { exp((it.toDouble() - peak.toDouble())) }
        val sum = exps.sum()
        return FloatArray(logits.size) { (exps[it] / sum).toFloat() }
    }

    fun average(frames: List<Map<String, Float>>): List<Pair<String, Float>> {
        if (frames.isEmpty()) return emptyList()
        val keys = frames.flatMap { it.keys }.distinct()
        return keys.map { key -> key to (frames.sumOf { (it[key] ?: 0f).toDouble() } / frames.size).toFloat() }
            .sortedByDescending { it.second }
    }
}
