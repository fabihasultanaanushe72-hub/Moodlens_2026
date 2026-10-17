package com.example.moodlens

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

object DetectionStore {
    data class HistoryPage(val items: List<DetectionEntity>, val count: Int)
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    fun save(context: Context, emotion: String, confidence: Float, source: String, targetMatched: Boolean) {
        require(confidence.isFinite() && confidence in 0f..1f)
        val app = context.applicationContext
        executor.execute {
            runCatching { MoodLensDatabase.getInstance(app).detectionDao().insert(DetectionEntity(emotion, confidence, System.currentTimeMillis(), source, targetMatched)) }
                .onFailure { Log.e("MoodLens", "Could not save detection", it); main.post { android.widget.Toast.makeText(app, "Couldn't save this detection", android.widget.Toast.LENGTH_SHORT).show() } }
        }
    }
    fun loadAll(context: Context, onError: (Throwable) -> Unit = {}, onResult: (List<DetectionEntity>) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val outcome = runCatching { MoodLensDatabase.getInstance(app).detectionDao().getAll() }
            main.post { outcome.fold(onResult, onError) }
        }
    }
    fun loadPage(context: Context, source: String, offset: Int, limit: Int, onResult: (HistoryPage) -> Unit, onError: (Throwable) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val outcome = runCatching {
                val dao = MoodLensDatabase.getInstance(app).detectionDao()
                HistoryPage(dao.getPage(source, limit, offset), dao.countFiltered(source))
            }
            main.post { outcome.fold(onResult, onError) }
        }
    }
    fun clearAll(context: Context, onComplete: () -> Unit, onError: (Throwable) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val outcome = runCatching { MoodLensDatabase.getInstance(app).detectionDao().clearAll() }
            main.post { outcome.fold(onSuccess = { onComplete() }, onFailure = onError) }
        }
    }
    fun sourceLabel(source: String): String = when (source.uppercase(java.util.Locale.US)) {
        "LIVE" -> "Live"; "TARGET" -> "Target camera"; "GALLERY" -> "Gallery"
        "CAMERA", "CAMERA PHOTO" -> "Camera photo"; else -> "Photo analysis"
    }
}
