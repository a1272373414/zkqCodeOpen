package com.coc.zkqcode.jar.code.universal.recognizer

import android.graphics.Bitmap
import android.graphics.Rect
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.waitForPlay
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.core.util.fileactions.LogHelper.showDebugInfo
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

data class RecognizedText(
    val text: String,
    val position: Rect?
)

object TextRecognizer {

    suspend fun recognize(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        useChinese: Boolean = true,
        threshold: Int = 140,
        saveImage: Boolean = false,
        applyPreprocess: Boolean = true,
        invertBinarization: Boolean = true,
        scale: Float = 1f
    ): List<RecognizedText> {
        val screenBuffer = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap
            ?: logAndRestart("in TextRecognizer, screen capture failed.")

        val width = endX - startX
        val height = endY - startY

        if (width <= 0 || height <= 0) {
            logAndRestart("Invalid crop area: width=$width, height=$height")
        }

        waitForPlay()

        try {
            if (startX + width <= screenBuffer.width && startY + height <= screenBuffer.height) {
                // 1. Crop the original region
                val croppedBitmap = Bitmap.createBitmap(screenBuffer, startX, startY, width, height)

                // 2. [Core Optimization] Apply preprocessing (optional)
                val bitmapToRecognize = if (applyPreprocess) {
                    preprocess(croppedBitmap, threshold = threshold, invertBinarization = invertBinarization)
                } else {
                    croppedBitmap
                }

                // 2.0 [Small text optimization] Upscale the bitmap before OCR when requested,
                //     because ML Kit struggles with glyphs smaller than ~20px (e.g. subtitles)
                val finalBitmap = if (scale != 1f) {
                    Bitmap.createScaledBitmap(
                        bitmapToRecognize,
                        (bitmapToRecognize.width * scale).toInt().coerceIn(1, 4096),
                        (bitmapToRecognize.height * scale).toInt().coerceIn(1, 4096),
                        true
                    )
                } else {
                    bitmapToRecognize
                }

                // 2.1 Save the image if requested
                if (saveImage) {
                    ScreenCaptureManager.getContext()?.let { context ->
                        try {
                            val file = File(context.filesDir, "test.png")
                            FileOutputStream(file).use { out ->
                                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            showDebugInfo("Image saved to: ${file.absolutePath}")
                        } catch (e: Exception) {
                            showDebugInfo("Failed to save image: ${e.message}")
                        }
                    }
                }

                // 3. Recognize the image
                return recognizeTextSync(finalBitmap, useChinese)
            } else {
                return emptyList()
            }
        } catch (e: CancellationException) {
            // Important: if it's a cancellation exception, must rethrow it for coroutine system to handle
            throw e
        } catch (e: Exception) {
            // Handle actual business errors here (e.g., out of memory, Bitmap creation failure, etc.)
            logAndRestart("Error during cropping or recognition: ${e.message}")
        }
    }

    /**
     * Image preprocessing: grayscale + binarization
     * Eliminates background interference, making it easier for ML Kit to recognize text contours
     */
    internal fun preprocess(src: Bitmap, threshold: Int, invertBinarization: Boolean): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            // Grayscale conversion formula
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

            // Binarization: swap foreground/background when invertBinarization is true
            pixels[i] = if (gray > threshold) {
                if (invertBinarization) -0x1000000 else -0x1
            } else {
                if (invertBinarization) -0x1 else -0x1000000
            }
        }

        val out = createBitmap(width, height)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    private suspend fun recognizeTextSync(bitmap: Bitmap, useChinese: Boolean): List<RecognizedText> =
        withContext(Dispatchers.IO) {
            val options = if (useChinese) {
                ChineseTextRecognizerOptions.Builder().build()
            } else {
                TextRecognizerOptions.DEFAULT_OPTIONS
            }
            val recognizer = TextRecognition.getClient(options)

            // The InputImage here receives our processed binarized Bitmap
            val image = InputImage.fromBitmap(bitmap, 0)

            try {
                val visionText = Tasks.await(recognizer.process(image))
                val result = mutableListOf<RecognizedText>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        result.add(RecognizedText(line.text, line.boundingBox))
                    }
                }
                result
            } catch (e: Exception) {
                showDebugInfo("UniversalTextRecognizer recognition failed: ${e.message}")
                emptyList()
            }
        }

    /**
     * Recognizes a region that is expected to contain only digits (and '.' '/' — e.g. troop
     * counts like "8/12", resource bars, or the remaining army). Runs ML Kit first (better when
     * the region may contain mixed text), then falls back to the offline [PixelFontOcr] whenever
     * ML Kit yields no digit — small in-game numbers are exactly where ML Kit is least reliable.
     *
     * The returned string keeps every recognized character; callers that need a number should
     * parse it (e.g. `Regex("\\d+")`).
     */
    suspend fun recognizeDigits(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        useChinese: Boolean = false,
        fallbackToPixelFont: Boolean = true,
        // ML Kit options
        threshold: Int = 140,
        saveImage: Boolean = false,
        applyPreprocess: Boolean = true,
        invertBinarization: Boolean = true,
        scale: Float = 1f,
        // PixelFontOcr options
        grayMin: Int = 200,
        grayMax: Int = 255,
        maxSaturation: Int = 50,
        minSimilarity: Double = 0.75,
        sizeTolerance: Int = 2,
        maxGlyphSize: Int = 50,
        waitForPlay: Boolean = false
    ): String {
        val mlkit = try {
            recognize(
                startX, startY, endX, endY, useChinese,
                threshold, saveImage, applyPreprocess, invertBinarization, scale
            )
        } catch (e: Exception) {
            emptyList()
        }
        val mlkitText = mlkit.joinToString("") { it.text }
        if (mlkitText.any { it.isDigit() }) {
            return mlkitText
        }
        if (fallbackToPixelFont) {
            PixelFontOcr.recognizeDigits(
                startX, startY, endX, endY,
                grayMin, grayMax, maxSaturation, minSimilarity, sizeTolerance, maxGlyphSize,
                waitForPlay
            )?.let { return it }
        }
        return mlkitText
    }
}