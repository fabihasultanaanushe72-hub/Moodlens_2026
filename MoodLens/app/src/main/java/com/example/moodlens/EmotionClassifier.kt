package com.example.moodlens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.FileInputStream
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

class EmotionClassifier(context: Context) : AutoCloseable {

    data class Prediction(
        val label: String,
        val confidence: Float,
        val scores: List<Pair<String, Float>>
    )

    private enum class InputLayout {
        NHWC,
        NCHW
    }

    companion object {
        private const val MODEL_FILE = "emotion_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val FER_SIZE = 48
        private const val MODEL_SIZE = 224

        private val EXPECTED_LABELS = listOf(
            "Angry",
            "Disgust",
            "Fear",
            "Happy",
            "Neutral",
            "Sad",
            "Surprise"
        )

        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)


    }

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputLayout: InputLayout

    private val smallColorBitmap = Bitmap.createBitmap(
        FER_SIZE,
        FER_SIZE,
        Bitmap.Config.ARGB_8888
    )
    private val smallGrayBitmap = Bitmap.createBitmap(
        FER_SIZE,
        FER_SIZE,
        Bitmap.Config.ARGB_8888
    )
    private val modelBitmap = Bitmap.createBitmap(
        MODEL_SIZE,
        MODEL_SIZE,
        Bitmap.Config.ARGB_8888
    )

    private val smallColorCanvas = Canvas(smallColorBitmap)
    private val modelCanvas = Canvas(modelBitmap)
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val smallPixels = IntArray(FER_SIZE * FER_SIZE)
    private val grayPixels = IntArray(FER_SIZE * FER_SIZE)
    private val modelPixels = IntArray(MODEL_SIZE * MODEL_SIZE)

    private val normalizedLookup = Array(3) { channel ->
        FloatArray(256) { value ->
            val unit = value / 255f
            (unit - MEAN[channel]) / STD[channel]
        }
    }

    private val inputBuffer = ByteBuffer.allocateDirect(
        MODEL_SIZE * MODEL_SIZE * 3 * 4
    ).order(ByteOrder.nativeOrder())

    private lateinit var outputBuffer: ByteBuffer

    init {
        labels = context.assets.open(LABELS_FILE)
            .bufferedReader()
            .useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            }

        require(labels.size == 7) {
            "$LABELS_FILE must contain exactly 7 labels."
        }

        require(
            labels.map { it.lowercase() } == EXPECTED_LABELS.map { it.lowercase() }
        ) {
            "$LABELS_FILE must be ordered as: ${EXPECTED_LABELS.joinToString(", ")}"
        }

        // The uncompressed asset is mapped directly; avoid duplicating the 107 MiB model.
        val modelBuffer = context.assets.openFd(MODEL_FILE).use { asset ->
            FileInputStream(asset.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, asset.startOffset, asset.declaredLength)
            }
        }

        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply {
                setNumThreads(4)
            }
        )

        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()

        require(inputTensor.dataType() == DataType.FLOAT32) {
            "Expected FLOAT32 model input but found ${inputTensor.dataType()}."
        }

        inputLayout = when {
            inputShape.contentEquals(intArrayOf(1, MODEL_SIZE, MODEL_SIZE, 3)) -> {
                InputLayout.NHWC
            }

            inputShape.contentEquals(intArrayOf(1, 3, MODEL_SIZE, MODEL_SIZE)) -> {
                InputLayout.NCHW
            }

            else -> {
                throw IllegalArgumentException(
                    "Expected model input [1,224,224,3] or [1,3,224,224] but found ${inputShape.contentToString()}."
                )
            }
        }

        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()

        require(outputTensor.dataType() == DataType.FLOAT32) {
            "Expected FLOAT32 model output but found ${outputTensor.dataType()}."
        }

        require(outputShape.contentEquals(intArrayOf(1, labels.size))) {
            "Expected ${labels.size} output values but found ${outputShape.contentToString()}."
        }

        outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes())
            .order(ByteOrder.nativeOrder())
    }

    fun describeModel(): String {
        val input = interpreter.getInputTensor(0)
        val output = interpreter.getOutputTensor(0)
        return "Input ${input.shape().contentToString()} ${input.dataType()}, output ${output.shape().contentToString()} ${output.dataType()}"
    }

    @Synchronized
    fun classify(faceBitmap: Bitmap, useTta: Boolean = false): Prediction {
        val logits = runModel(faceBitmap)

        val averagedLogits = if (useTta) {
            val flipped = flipHorizontally(faceBitmap)
            try {
                val flippedLogits = runModel(flipped)
                FloatArray(logits.size) { index -> (logits[index] + flippedLogits[index]) / 2f }
            } finally { if (flipped !== faceBitmap) flipped.recycle() }
        } else {
            logits
        }

        require(averagedLogits.all { it.isFinite() }) { "The model returned invalid scores." }
        val probabilities = EmotionMath.softmax(averagedLogits)

        val scores = labels.mapIndexed { index, label ->
            label to probabilities[index]
        }.sortedByDescending { it.second }

        val best = scores.firstOrNull()
            ?: throw IllegalStateException("The FER model returned no scores.")

        return Prediction(
            label = best.first,
            confidence = best.second,
            scores = scores
        )
    }

    private fun runModel(faceBitmap: Bitmap): FloatArray {
        createInputBuffer(faceBitmap)
        outputBuffer.clear()

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val outputSize = interpreter.getOutputTensor(0).shape().last()
        return FloatArray(outputSize) {
            outputBuffer.float
        }
    }

    private fun createInputBuffer(faceBitmap: Bitmap) {
        smallColorCanvas.drawColor(Color.BLACK)
        smallColorCanvas.drawBitmap(
            faceBitmap,
            null,
            Rect(0, 0, FER_SIZE, FER_SIZE),
            scalePaint
        )

        smallColorBitmap.getPixels(
            smallPixels,
            0,
            FER_SIZE,
            0,
            0,
            FER_SIZE,
            FER_SIZE
        )

        for (index in smallPixels.indices) {
            val pixel = smallPixels[index]
            val gray = (
                0.299f * Color.red(pixel) +
                    0.587f * Color.green(pixel) +
                    0.114f * Color.blue(pixel)
                ).roundToInt().coerceIn(0, 255)
            grayPixels[index] = Color.rgb(gray, gray, gray)
        }

        smallGrayBitmap.setPixels(
            grayPixels,
            0,
            FER_SIZE,
            0,
            0,
            FER_SIZE,
            FER_SIZE
        )

        modelCanvas.drawColor(Color.BLACK)
        modelCanvas.drawBitmap(
            smallGrayBitmap,
            null,
            Rect(0, 0, MODEL_SIZE, MODEL_SIZE),
            scalePaint
        )

        modelBitmap.getPixels(
            modelPixels,
            0,
            MODEL_SIZE,
            0,
            0,
            MODEL_SIZE,
            MODEL_SIZE
        )

        inputBuffer.clear()

        if (inputLayout == InputLayout.NHWC) {
            for (pixel in modelPixels) {
                val gray = Color.red(pixel)
                inputBuffer.putFloat(normalizedLookup[0][gray])
                inputBuffer.putFloat(normalizedLookup[1][gray])
                inputBuffer.putFloat(normalizedLookup[2][gray])
            }
        } else {
            for (channel in 0 until 3) {
                for (pixel in modelPixels) {
                    val gray = Color.red(pixel)
                    inputBuffer.putFloat(normalizedLookup[channel][gray])
                }
            }
        }

        inputBuffer.rewind()
    }

    private fun flipHorizontally(source: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            setScale(-1f, 1f)
        }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }

    override fun close() {
        interpreter.close()
        smallColorBitmap.recycle()
        smallGrayBitmap.recycle()
        modelBitmap.recycle()
    }
}
