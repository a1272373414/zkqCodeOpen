package com.coc.zkqcode.jar.code.universal.recognizer

import android.graphics.Bitmap
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.waitForPlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Offline pixel-font OCR used as a fallback when ML Kit cannot read the small in-game numbers
 * (troop counts "8/12", resource bars, remaining army, ...).
 *
 * The glyph library [PIXEL_FONT_DIGITS] is harvested from the current game build
 * (regenerate with tools/harvest_pixel_font.py); every entry is
 * "字符|高,宽|64进制点阵串", the bitmap is row-major / MSB first and bit=1 is ink.
 *
 * Pipeline (same idea as the legacy TURING OCR):
 *   1. Binarize the region: luminance in [grayMin, grayMax] AND saturation <= maxSaturation
 *      -> ink mask (the saturation term keeps the coloured resource bars out of the ink)
 *   2. Label connected components (8-connectivity) and drop noise
 *   3. Group components into text lines by vertical overlap, sort each line left to right
 *   4. Compare every component against the glyph library and keep the best score
 */
object PixelFontOcr {

    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"

    private class Glyph(val char: Char, val width: Int, val height: Int, val bits: BooleanArray)

    /** Lazily parsed glyph library (1891 variants of 0-9, '.' and '/'). */
    private val glyphs: List<Glyph> by lazy { parseFontLibrary() }

    private fun parseFontLibrary(): List<Glyph> {
        val result = ArrayList<Glyph>(2000)
        val lines = PIXEL_FONT_DIGITS.split('\n')
        for (line in lines) {
            val entry = line.trim()
            if (entry.isEmpty()) continue
            val parts = entry.split('|')
            if (parts.size < 3) continue
            val ch = parts[0].firstOrNull() ?: continue
            val dims = parts[1].split(',')
            if (dims.size != 2) continue
            val height = dims[0].toIntOrNull() ?: continue
            val width = dims[1].toIntOrNull() ?: continue
            if (width <= 0 || height <= 0) continue
            val data = parts.subList(2, parts.size).joinToString("|")

            // 6 bits per base-64 char, most significant bit first
            val raw = StringBuilder(data.length * 6)
            for (c in data) {
                val value = ALPHABET.indexOf(c)
                if (value < 0) continue
                for (i in 5 downTo 0) {
                    raw.append(if ((value shr i) and 1 == 1) '1' else '0')
                }
            }
            val needed = width * height
            if (raw.length < needed) continue
            // The legacy loader keeps only the last width*height bits (leading bits are padding)
            val start = raw.length - needed
            val bits = BooleanArray(needed)
            for (i in 0 until needed) {
                bits[i] = raw[start + i] == '1'
            }
            result.add(Glyph(ch, width, height, bits))
        }
        return result
    }

    /**
     * Recognize digits inside [bitmap].
     *
     * @param grayMin/grayMax luminance range treated as ink (the legacy libraries use different
     *                        ranges per screen, e.g. "0-240" or "138-255")
     * @param maxSaturation   maximum allowed max-min channel difference for an ink pixel. The
     *                        in-game numbers are near-white text drawn on strongly coloured bars
     *                        (gold / elixir / dark elixir / gems); without this filter the bar
     *                        itself becomes ink and neighbouring digits merge into one blob.
     *                        Must match the criterion used to harvest [PIXEL_FONT_DIGITS].
     * @param minSimilarity   minimal bit agreement required to accept a glyph (0..1)
     * @param sizeTolerance   allowed difference between the component size and the glyph size
     * @param unknownChar     placeholder written when nothing matches
     */
    fun recognize(
        bitmap: Bitmap,
        grayMin: Int = 200,
        grayMax: Int = 255,
        maxSaturation: Int = 50,
        minSimilarity: Double = 0.75,
        sizeTolerance: Int = 2,
        maxGlyphSize: Int = 50,
        minGlyphWidth: Int = 2,
        minGlyphHeight: Int = 3,
        unknownChar: Char = '?'
    ): String {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return ""
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val ink = BooleanArray(width * height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
            val saturation = maxOf(r, g, b) - minOf(r, g, b)
            ink[i] = gray in grayMin..grayMax && saturation <= maxSaturation
        }

        val components = labelComponents(ink, width, height)
            .filter { it.width in minGlyphWidth..maxGlyphSize && it.height in minGlyphHeight..maxGlyphSize }
            .sortedWith(compareBy<Component> { it.top }.thenBy { it.left })

        if (components.isEmpty()) return ""

        // Group components that overlap vertically into one text line
        val lines = ArrayList<MutableList<Component>>()
        for (component in components) {
            val line = lines.firstOrNull { component.top <= it.maxOf { c -> c.bottom } && component.bottom >= it.minOf { c -> c.top } }
            if (line != null) {
                line.add(component)
            } else {
                lines.add(mutableListOf(component))
            }
        }

        return lines.joinToString("\n") { line ->
            line.sortedBy { it.left }.joinToString("") { component ->
                val match = matchGlyph(component, minSimilarity, sizeTolerance)
                match ?: unknownChar.toString()
            }
        }
    }

    /**
     * Capture the screen and recognize digits inside the given region.
     * Returns null when the capture fails or nothing is recognized.
     */
    suspend fun recognizeDigits(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        grayMin: Int = 200,
        grayMax: Int = 255,
        maxSaturation: Int = 50,
        minSimilarity: Double = 0.75,
        sizeTolerance: Int = 2,
        maxGlyphSize: Int = 50,
        waitForPlay: Boolean = false
    ): String? = withContext(Dispatchers.Default) {
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return@withContext null
        val width = endX - startX
        val height = endY - startY
        if (width <= 0 || height <= 0 || startX + width > screen.width || startY + height > screen.height) {
            return@withContext null
        }
        if (waitForPlay) {
            waitForPlay()
        }
        val cropped = Bitmap.createBitmap(screen, startX, startY, width, height)
        recognize(
            bitmap = cropped,
            grayMin = grayMin,
            grayMax = grayMax,
            maxSaturation = maxSaturation,
            minSimilarity = minSimilarity,
            sizeTolerance = sizeTolerance,
            maxGlyphSize = maxGlyphSize
        ).takeIf { it.isNotEmpty() }
    }

    private class Component(
        val left: Int, val top: Int, val right: Int, val bottom: Int,
        val mask: BooleanArray, val width: Int, val height: Int
    )

    private fun labelComponents(ink: BooleanArray, width: Int, height: Int): List<Component> {
        val visited = BooleanArray(width * height)
        val stack = IntArray(width * height)
        val result = ArrayList<Component>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (!ink[index] || visited[index]) continue
                var top = 0
                stack[top++] = index
                visited[index] = true
                var minX = x
                var maxX = x
                var minY = y
                var maxY = y
                while (top > 0) {
                    val current = stack[--top]
                    val cx = current % width
                    val cy = current / width
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy
                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny < 0 || ny >= height) continue
                        for (dx in -1..1) {
                            val nx = cx + dx
                            if (nx < 0 || nx >= width) continue
                            val ni = ny * width + nx
                            if (ink[ni] && !visited[ni]) {
                                visited[ni] = true
                                stack[top++] = ni
                            }
                        }
                    }
                }
                val cw = maxX - minX + 1
                val ch = maxY - minY + 1
                val mask = BooleanArray(cw * ch)
                for (cy in minY..maxY) {
                    for (cx in minX..maxX) {
                        if (ink[cy * width + cx]) mask[(cy - minY) * cw + (cx - minX)] = true
                    }
                }
                result.add(Component(minX, minY, maxX, maxY, mask, cw, ch))
            }
        }
        return result
    }

    private fun matchGlyph(component: Component, minSimilarity: Double, sizeTolerance: Int): String? {
        var bestChar: Char? = null
        var bestScore = 0.0
        for (glyph in glyphs) {
            if (abs(glyph.width - component.width) > sizeTolerance) continue
            if (abs(glyph.height - component.height) > sizeTolerance) continue
            val rows = maxOf(glyph.height, component.height)
            val cols = maxOf(glyph.width, component.width)
            var matched = 0
            var total = 0
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val glyphBit = y < glyph.height && x < glyph.width && glyph.bits[y * glyph.width + x]
                    val inkBit = y < component.height && x < component.width && component.mask[y * component.width + x]
                    if (glyphBit == inkBit) matched++
                    total++
                }
            }
            val score = matched.toDouble() / total.toDouble()
            if (score > bestScore) {
                bestScore = score
                bestChar = glyph.char
            }
        }
        return if (bestChar != null && bestScore >= minSimilarity) bestChar.toString() else null
    }
}
