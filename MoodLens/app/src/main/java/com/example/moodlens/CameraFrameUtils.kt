package com.example.moodlens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

object CameraFrameUtils {

    fun toBitmap(
        imageProxy: ImageProxy,
        mirrorHorizontally: Boolean
    ): Bitmap {
        val baseBitmap = imageProxy.toBitmap()
        val rotation = imageProxy.imageInfo.rotationDegrees

        if (rotation == 0 && !mirrorHorizontally) {
            return baseBitmap
        }

        val matrix = Matrix().apply {
            if (rotation != 0) {
                postRotate(rotation.toFloat())
            }
            if (mirrorHorizontally) {
                postScale(-1f, 1f)
            }
        }

        val transformed = Bitmap.createBitmap(
            baseBitmap,
            0,
            0,
            baseBitmap.width,
            baseBitmap.height,
            matrix,
            true
        )

        if (transformed !== baseBitmap && !baseBitmap.isRecycled) {
            baseBitmap.recycle()
        }

        return transformed
    }
}
