package com.example.moodlens

import org.junit.Assert.*
import org.junit.Test

class EmotionMathTest {
    @Test fun extremeLogitsRemainFiniteAndNormalized() {
        val result = EmotionMath.softmax(floatArrayOf(10000f, 9999f, -10000f))
        assertTrue(result.all { it.isFinite() && it in 0f..1f })
        assertEquals(1.0, result.sum().toDouble(), 0.000001)
        assertTrue(result[0] > result[1])
    }
    @Test fun equalLogitsGiveEqualProbabilities() {
        val result = EmotionMath.softmax(FloatArray(7) { 5f })
        result.forEach { assertEquals(1f / 7f, it, 0.000001f) }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonFiniteOutput() {
        EmotionMath.softmax(floatArrayOf(Float.NaN))
    }
    @Test fun smoothingUsesLabelsInsteadOfRankPositions() {
        val result = EmotionMath.average(listOf(mapOf("Happy" to .8f, "Sad" to .2f), mapOf("Sad" to .9f, "Happy" to .1f)))
        assertEquals("Sad", result.first().first)
        assertEquals(.55f, result.first().second, .000001f)
    }
}
