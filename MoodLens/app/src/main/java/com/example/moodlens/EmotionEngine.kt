package com.example.moodlens

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

class EmotionEngine(context: Context, onReady: (Result<Unit>) -> Unit) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    private var classifier: EmotionClassifier? = null

    init {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching { classifier = EmotionClassifier(app) }
            main.post { if (!closed) onReady(result) }
        }
    }

    fun predict(face: Bitmap, useTta: Boolean, onResult: (Result<EmotionClassifier.Prediction>) -> Unit) {
        if (closed) { face.recycle(); return }
        executor.execute {
            val result = try {
                runCatching {
                    check(!closed) { "Analysis was cancelled" }
                    (classifier ?: error("Emotion model is unavailable")).classify(face, useTta)
                }
            } finally { if (!face.isRecycled) face.recycle() }
            main.post { if (!closed) onResult(result) }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        executor.execute { classifier?.close(); classifier = null }
        executor.shutdown()
    }
}
