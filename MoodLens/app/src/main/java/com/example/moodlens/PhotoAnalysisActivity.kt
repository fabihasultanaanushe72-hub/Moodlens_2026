package com.example.moodlens

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class PhotoAnalysisActivity : MoodLensActivity() {
    private lateinit var image: ImageView
    private lateinit var result: TextView
    private lateinit var scores: LinearLayout
    private lateinit var overlay: FaceOverlayView
    private lateinit var detector: FaceDetectorHelper
    private lateinit var engine: EmotionEngine
    private val decoder = Executors.newSingleThreadExecutor()
    private var displayed: Bitmap? = null
    private var source = "GALLERY"
    private var busy = false
    private var detecting = false
    private var ready = false
    @Volatile private var disposed = false

    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !busy) load("GALLERY") { BitmapUtils.fromUri(this, uri) }
    }
    private val camera = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        if (response.resultCode == Activity.RESULT_OK && !busy) {
            val path = response.data?.getStringExtra("photoPath")
            if (path == null) showError("The camera did not return a photo.")
            else load("CAMERA") {
                try { BitmapUtils.fromFile(path) } finally { File(path).delete() }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_analysis)
        image = findViewById(R.id.imagePreview)
        result = findViewById(R.id.txtResult)
        scores = findViewById(R.id.scoreContainer)
        overlay = findViewById(R.id.photoOverlay)
        detector = FaceDetectorHelper(accurate = true)
        bindBack(R.id.btnPhotoBack)
        updateButtons()
        result.text = "Preparing emotion model…"
        engine = EmotionEngine(this) { outcome ->
            if (canUpdateUi()) outcome.fold(
                onSuccess = { ready = true; result.text = "Choose or capture a photo to begin."; updateButtons() },
                onFailure = { showError("The emotion model could not load. Please reopen this screen.") }
            )
        }
        findViewById<Button>(R.id.btnGallery).setOnClickListener { if (!busy) gallery.launch("image/*") }
        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            if (!busy) camera.launch(Intent(this, LiveDetectionActivity::class.java).putExtra(LiveDetectionActivity.EXTRA_MODE, LiveDetectionActivity.MODE_CAPTURE))
        }
    }
    private fun updateButtons() {
        listOf(R.id.btnGallery, R.id.btnTakePhoto).forEach {
            findViewById<Button>(it).apply { isEnabled = ready && !busy; alpha = if (isEnabled) 1f else 0.45f }
        }
        findViewById<ProgressBar>(R.id.progressBar).visibility = if (busy) View.VISIBLE else View.GONE
    }
    private fun load(newSource: String, read: () -> Bitmap) {
        if (!ready || busy) return
        busy = true; source = newSource; updateButtons()
        scores.removeAllViews(); overlay.clear(); result.text = "Opening photo…"
        decoder.execute {
            val decoded = runCatching(read)
            runOnUiThread {
                if (disposed || !canUpdateUi()) { decoded.getOrNull()?.recycle() }
                else decoded.fold(onSuccess = { analyze(it) }, onFailure = { showError("Couldn't open this photo. Choose another image.") })
            }
        }
    }
    private fun analyze(bitmap: Bitmap) {
        image.setImageBitmap(bitmap)
        displayed?.recycle(); displayed = bitmap
        image.visibility = View.VISIBLE
        findViewById<View>(R.id.txtEmpty).visibility = View.GONE
        result.text = "Finding the largest face…"
        detecting = true
        detector.detectLargestFace(bitmap,
            onSuccess = { face ->
                detecting = false
                if (disposed || !canUpdateUi()) { face.bitmap.recycle(); releasePhotoIfDisposed() }
                else {
                    result.text = "Analyzing expression…"
                    engine.predict(face.bitmap, true) { outcome ->
                        if (canUpdateUi()) outcome.fold(
                            onSuccess = { prediction ->
                                overlay.setResults(listOf(FaceOverlayView.FaceResult(face.originalBounds, prediction.label, prediction.confidence, false)), bitmap.width, bitmap.height, false, fitCenter = true)
                                showPrediction(prediction)
                            },
                            onFailure = { showError("Analysis failed. Please try another photo.") }
                        )
                    }
                }
            },
            onNoFace = { detecting = false; if (canUpdateUi() && !disposed) showError("No face found. Try a clear, front-facing photo."); releasePhotoIfDisposed() },
            onError = { detecting = false; if (canUpdateUi() && !disposed) showError("Face detection failed. Please try again."); releasePhotoIfDisposed() }
        )
    }
    private fun showPrediction(prediction: EmotionClassifier.Prediction) {
        busy = false; updateButtons()
        val targets = getSharedPreferences("moodlens", MODE_PRIVATE).getStringSet("targets", emptySet())!!.toSet()
        val matched = targets.any { it.equals(prediction.label, true) }
        result.text = String.format(Locale.getDefault(), "%s %s · %.1f%%%s%s", EmotionStyle.emoji(prediction.label), prediction.label,
            prediction.confidence * 100, if (matched) "\nTarget match" else "", if (prediction.confidence < 0.4f) "\nLow confidence • expression uncertain" else "")
        scores.removeAllViews()
        prediction.scores.forEach { (label, value) ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
            row.addView(TextView(this).apply {
                text = String.format(Locale.getDefault(), "%s%s · %.1f%%", label, if (targets.any { it.equals(label, true) }) "  • target" else "", value * 100)
                textSize = 14f; setTextColor(getColor(R.color.text_secondary))
            })
            row.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000; progress = (value * 1000).toInt()
                progressTintList = ColorStateList.valueOf(EmotionStyle.color(label))
                progressBackgroundTintList = ColorStateList.valueOf(getColor(R.color.border))
                layoutParams = LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(5) }
                contentDescription = "$label: ${(value * 100).toInt()} percent"
            })
            scores.addView(row)
        }
        DetectionStore.save(this, prediction.label, prediction.confidence, source, matched)
    }
    private fun showError(message: String) { busy = false; updateButtons(); result.text = message }
    private fun releasePhotoIfDisposed() { if (disposed) { displayed?.recycle(); displayed = null } }
    override fun onDestroy() {
        disposed = true
        image.setImageDrawable(null)
        // ML Kit may still be reading the photo. Its completion callbacks release it safely.
        if (!detecting) { displayed?.recycle(); displayed = null }
        engine.close(); detector.close(); decoder.shutdown()
        super.onDestroy()
    }
}
