package com.example.moodlens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LiveDetectionActivity : MoodLensActivity() {
    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_TARGETS = "targets"
        const val MODE_LIVE = "live"
        const val MODE_TARGET = "target"
        const val MODE_CAPTURE = "capture"
    }
    private lateinit var preview: PreviewView
    private lateinit var overlay: FaceOverlayView
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var permissionButton: Button
    private lateinit var captureButton: Button
    private lateinit var detector: FaceDetectorHelper
    private var engine: EmotionEngine? = null
    private var ready = false
    private var provider: ProcessCameraProvider? = null
    private var capture: ImageCapture? = null
    private val frames = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val processing = AtomicBoolean(false)
    @Volatile private var generation = 0
    @Volatile private var active = false
    @Volatile private var destroyed = false
    @Volatile private var lastFrameAt = 0L
    private var facing = CameraSelector.LENS_FACING_FRONT
    private var mode = MODE_LIVE
    private var targets: Set<String> = emptySet()
    private val history = ArrayDeque<Map<String, Float>>()
    private var stableLabel = ""
    private var stableCount = 0
    private var savedLabel = ""
    private var savedAt = 0L
    private var captureFile: File? = null
    private var permissionRequested = false

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { permissionButton.visibility = View.GONE; startCamera() }
        else { permissionButton.visibility = View.VISIBLE; result.text = "Allow camera access to continue." }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_detection)
        preview = findViewById(R.id.previewView)
        overlay = findViewById(R.id.overlayView)
        status = findViewById(R.id.txtCameraStatus)
        result = findViewById(R.id.txtLiveResult)
        permissionButton = findViewById(R.id.btnCameraPermission)
        captureButton = findViewById(R.id.btnCapture)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_LIVE
        targets = intent.getStringArrayListExtra(EXTRA_TARGETS)?.toSet()
            ?: getSharedPreferences("moodlens", MODE_PRIVATE).getStringSet("targets", emptySet())!!.toSet()
        status.text = when (mode) { MODE_CAPTURE -> "Photo Camera"; MODE_TARGET -> "Target Camera"; else -> "Live Detection" }
        detector = FaceDetectorHelper(accurate = false)
        if (mode == MODE_CAPTURE) {
            overlay.visibility = View.GONE
            captureButton.visibility = View.VISIBLE
            result.text = "Center a face and capture your photo."
        } else {
            result.text = "Preparing emotion model…"
            engine = EmotionEngine(this) { outcome ->
                if (canUpdateUi()) outcome.fold(
                    onSuccess = { ready = true; result.text = if (hasPermission()) watchingText() else "Allow camera access to continue." },
                    onFailure = { result.text = "Could not load the emotion model. Please reopen this screen." }
                )
            }
        }
        bindBack(R.id.btnBack)
        findViewById<View>(R.id.btnSwitch).setOnClickListener {
            facing = if (facing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }
        captureButton.setOnClickListener { takePhoto() }
        permissionButton.setOnClickListener {
            if (!hasPermission() && permissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } else requestPermission()
        }
        if (!hasPermission()) requestPermission()
    }

    private fun hasPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun requestPermission() { permissionRequested = true; permission.launch(Manifest.permission.CAMERA) }
    override fun onStart() {
        super.onStart()
        active = true
        if (hasPermission()) { permissionButton.visibility = View.GONE; startCamera() }
    }
    override fun onStop() {
        active = false
        generation++
        provider?.unbindAll()
        capture = null
        super.onStop()
    }
    private fun watchingText(): String = if (mode == MODE_TARGET) {
        if (targets.isEmpty()) "No target emotions selected" else "Watching for ${targets.sorted().joinToString(", ")}"
    } else "Point the camera at one face."
    private fun current(epoch: Int) = !destroyed && active && epoch == generation && canUpdateUi()
    private fun resetTracking() { history.clear(); stableLabel = ""; stableCount = 0; overlay.clear() }

    private fun startCamera() {
        if (!active || !hasPermission()) return
        val epoch = ++generation
        resetTracking()
        capture = null
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (!current(epoch)) return@addListener
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                var selector = CameraSelector.Builder().requireLensFacing(facing).build()
                if (!cameraProvider.hasCamera(selector)) {
                    facing = if (facing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                    selector = CameraSelector.Builder().requireLensFacing(facing).build()
                }
                check(cameraProvider.hasCamera(selector)) { "No available camera" }
                val rotation = preview.display?.rotation ?: 0
                val previewCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(rotation).build().also { it.setSurfaceProvider(preview.surfaceProvider) }
                cameraProvider.unbindAll()
                if (mode == MODE_CAPTURE) {
                    val photo = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(rotation).setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    cameraProvider.bindToLifecycle(this, selector, previewCase, photo)
                    capture = photo
                } else {
                    val analysis = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(rotation).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    val mirror = facing == CameraSelector.LENS_FACING_FRONT
                    analysis.setAnalyzer(frames) { frame -> analyze(frame, epoch, mirror) }
                    cameraProvider.bindToLifecycle(this, selector, previewCase, analysis)
                }
            } catch (error: Exception) {
                result.text = "Camera unavailable. Try switching cameras or reopening this screen."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(frame: ImageProxy, epoch: Int, mirror: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!active || epoch != generation || now - lastFrameAt < 250L || !processing.compareAndSet(false, true)) {
            frame.close(); return
        }
        lastFrameAt = now
        val bitmap = try { CameraFrameUtils.toBitmap(frame, mirror) }
        catch (error: Exception) { processing.set(false); return }
        finally { frame.close() }
        main.post {
            if (!current(epoch) || !ready) { bitmap.recycle(); processing.set(false); return@post }
            val width = bitmap.width
            val height = bitmap.height
            detector.detectLargestFace(bitmap,
                onSuccess = { face ->
                    bitmap.recycle()
                    if (!current(epoch)) { face.bitmap.recycle(); processing.set(false) }
                    else engine!!.predict(face.bitmap, false) { outcome ->
                        processing.set(false)
                        if (current(epoch)) outcome.fold(
                            onSuccess = { prediction ->
                                history.addLast(prediction.scores.toMap())
                                if (history.size > 3) history.removeFirst()
                                val best = EmotionMath.average(history.toList()).first()
                                val matched = targets.any { it.equals(best.first, true) }
                                val threshold = if (mode == MODE_TARGET) 0.38f else 0.32f
                                if (best.second < threshold || (mode == MODE_TARGET && !matched)) {
                                    overlay.clear()
                                    stableLabel = ""; stableCount = 0
                                    result.text = if (mode == MODE_TARGET) watchingText() else "Face found • expression uncertain"
                                } else {
                                    val faceResult = FaceOverlayView.FaceResult(face.originalBounds, best.first, best.second, mode == MODE_TARGET && matched)
                                    overlay.setResults(listOf(faceResult), width, height, mode == MODE_TARGET)
                                    result.text = String.format(Locale.getDefault(), "%s · %.1f%%%s", best.first, best.second * 100, if (mode == MODE_TARGET) " · Target match" else "")
                                    if (stableLabel == best.first) stableCount++ else { stableLabel = best.first; stableCount = 1 }
                                    saveStable(faceResult)
                                }
                            },
                            onFailure = { overlay.clear(); result.text = "Analysis failed. Try reopening this screen." }
                        )
                    }
                },
                onNoFace = {
                    bitmap.recycle(); processing.set(false)
                    if (current(epoch)) { resetTracking(); result.text = "No face found • keep one face centered" }
                },
                onError = {
                    bitmap.recycle(); processing.set(false)
                    if (current(epoch)) { resetTracking(); result.text = "Face detection failed. Please try again." }
                }
            )
        }
    }
    private fun saveStable(face: FaceOverlayView.FaceResult) {
        if (stableCount < 2 || face.confidence < 0.42f) return
        val now = SystemClock.elapsedRealtime()
        if (savedAt != 0L && now - savedAt < if (savedLabel == face.label) 12000L else 3000L) return
        DetectionStore.save(this, face.label, face.confidence, if (mode == MODE_TARGET) "TARGET" else "LIVE", face.targetMatched)
        savedLabel = face.label; savedAt = now
    }
    private fun takePhoto() {
        val photo = capture ?: run { result.text = "Camera is still starting. Try again shortly."; return }
        if (!captureButton.isEnabled) return
        captureButton.isEnabled = false
        val file = File(cacheDir, "moodlens_${System.currentTimeMillis()}.jpg")
        captureFile = file
        photo.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    captureFile = null
                    if (destroyed || isFinishing) { file.delete(); return }
                    setResult(Activity.RESULT_OK, Intent().putExtra("photoPath", file.absolutePath)); finish()
                }
                override fun onError(error: ImageCaptureException) {
                    file.delete(); captureFile = null
                    if (canUpdateUi()) { captureButton.isEnabled = true; result.text = "Photo capture failed. Please try again." }
                }
            })
    }
    override fun onDestroy() {
        destroyed = true
        generation++
        provider?.unbindAll()
        engine?.close()
        detector.close()
        frames.shutdown()
        captureFile?.delete()
        super.onDestroy()
    }
}
