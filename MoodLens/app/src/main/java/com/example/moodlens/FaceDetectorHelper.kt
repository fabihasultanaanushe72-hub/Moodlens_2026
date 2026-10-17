package com.example.moodlens

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorHelper(
    accurate: Boolean = true
) : AutoCloseable {

    data class FaceCrop(
        val bitmap: Bitmap,
        val originalBounds: Rect
    )

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(
                if (accurate) {
                    FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
                } else {
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                }
            )
            .setMinFaceSize(0.10f)
            .build()
    )

    fun detectFaces(
        bitmap: Bitmap,
        onSuccess: (List<FaceCrop>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val results = faces.mapNotNull { face ->
                    val cropRect = expandedAndClampedRect(
                        face.boundingBox,
                        bitmap.width,
                        bitmap.height
                    )

                    if (cropRect.width() <= 0 || cropRect.height() <= 0) {
                        null
                    } else {
                        val region = Bitmap.createBitmap(
                            bitmap,
                            cropRect.left,
                            cropRect.top,
                            cropRect.width(),
                            cropRect.height()
                        )

                        // Always own a separate crop, even when the box covers the whole image.
                        val cropped = region.copy(Bitmap.Config.ARGB_8888, false) ?: error("Could not allocate a face crop")
                        if (region !== bitmap) region.recycle()
                        FaceCrop(
                            bitmap = cropped,
                            originalBounds = Rect(face.boundingBox)
                        )
                    }
                }.sortedByDescending {
                    it.originalBounds.width() * it.originalBounds.height()
                }

                onSuccess(results)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun detectLargestFace(
        bitmap: Bitmap,
        onSuccess: (FaceCrop) -> Unit,
        onNoFace: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        detectFaces(
            bitmap = bitmap,
            onSuccess = { faces ->
                val largest = faces.firstOrNull()
                faces.drop(1).forEach { it.bitmap.recycle() }
                if (largest == null) {
                    onNoFace()
                } else {
                    onSuccess(largest)
                }
            },
            onError = onError
        )
    }

    private fun expandedAndClampedRect(
        source: Rect,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        val extraX = (source.width() * 0.22f).toInt()
        val extraY = (source.height() * 0.22f).toInt()

        val left = (source.left - extraX).coerceAtLeast(0)
        val top = (source.top - extraY).coerceAtLeast(0)
        val right = (source.right + extraX).coerceAtMost(imageWidth)
        val bottom = (source.bottom + extraY).coerceAtMost(imageHeight)

        return Rect(left, top, right, bottom)
    }

    override fun close() {
        detector.close()
    }
}
