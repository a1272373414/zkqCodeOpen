package com.coc.zkqcode.jar.code.universal.colors

import android.graphics.Bitmap
import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager

/**
 * Normalized cross-correlation (NCC) template matcher — a port of the legacy `ncc.c`
 * (`ncc_match_gray`). Unlike [findMultiColors] which requires exact point colours, NCC is robust
 * to small colour drift / lighting changes, so it is useful for "shape is stable but colour may
 * vary slightly" targets like building icons or partially occluded decorations.
 *
 * The score is in [-1, 1]; a window whose score >= [threshold] is a match. The centre of the best
 * window is returned. For our 1280x720 screenshots with small templates (<= 64x64) and small
 * search regions this runs well under a frame, so no integral image is needed.
 */
object NccMatcher {

    /** Result of a match: the template centre (in image coordinates) and the NCC score. */
    data class Hit(val x: Int, val y: Int, val score: Double)

    internal data class Template(
        val gray: IntArray,
        val w: Int,
        val h: Int,
        val mean: Double,
        val norm: Double
    )

    /** Convert an ARGB_8888 bitmap to a grayscale (R+G+B)/3 buffer, same as `ncc.c`. */
    private fun toGray(bitmap: Bitmap): Pair<IntArray, Int> {
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h)
        for (i in px.indices) {
            val c = px[i]
            gray[i] = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
        }
        return gray to w
    }

    private fun buildTemplate(template: Bitmap): Template {
        val (gray, w) = toGray(template)
        val h = gray.size / w
        var sum = 0L
        for (v in gray) sum += v
        val mean = sum.toDouble() / (w * h)
        var normSq = 0.0
        for (v in gray) {
            val d = v - mean
            normSq += d * d
        }
        return Template(gray, w, h, mean, kotlin.math.sqrt(normSq))
    }

    /**
     * Core NCC search over an already-grayscale image buffer (row-major, `iw` columns).
     * Port of `ncc_match_gray` — returns the centre of the best window, or null below [threshold].
     */
    private fun match(
        img: IntArray,
        iw: Int,
        ih: Int,
        template: Template,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        threshold: Double
    ): Hit? {
        val rx1 = if (x1 < 0) 0 else x1
        val ry1 = if (y1 < 0) 0 else y1
        val rx2 = if (x2 >= iw) iw - 1 else x2
        val ry2 = if (y2 >= ih) ih - 1 else y2
        val sw = rx2 - rx1 + 1
        val sh = ry2 - ry1 + 1
        val tw = template.w
        val th = template.h
        if (tw > sw || th > sh || tw <= 0 || th <= 0) return null
        if (template.norm < 1e-6) return null // pure-colour template, meaningless

        var bestScore = -2.0
        var bestX = -1
        var bestY = -1
        val twth = tw * th
        val tmplGray = template.gray
        val tmplMean = template.mean
        val tmplNorm = template.norm

        var cy = ry1
        while (cy <= ry2 - th + 1) {
            var cx = rx1
            while (cx <= rx2 - tw + 1) {
                var winSum = 0L
                var crossSum = 0.0
                var dy = 0
                while (dy < th) {
                    val imgOff = (cy + dy) * iw + cx
                    val tOff = dy * tw
                    var dx = 0
                    while (dx < tw) {
                        val v = img[imgOff + dx]
                        val t = tmplGray[tOff + dx] - tmplMean
                        winSum += v
                        crossSum += v * t
                        dx++
                    }
                    dy++
                }
                val winMean = winSum.toDouble() / twth

                var winNormSq = 0.0
                dy = 0
                while (dy < th) {
                    val imgOff = (cy + dy) * iw + cx
                    var dx = 0
                    while (dx < tw) {
                        val d = img[imgOff + dx] - winMean
                        winNormSq += d * d
                        dx++
                    }
                    dy++
                }
                val denom = kotlin.math.sqrt(winNormSq) * tmplNorm
                if (denom >= 1e-6) {
                    val score = crossSum / denom
                    if (score > bestScore) {
                        bestScore = score
                        bestX = cx + tw / 2
                        bestY = cy + th / 2
                    }
                }
                cx++
            }
            cy++
        }
        if (bestScore < threshold) return null
        return Hit(bestX, bestY, bestScore)
    }

    /**
     * Locate [template] inside the screen region [`startX`,`startY`]..[`endX`,`endY`].
     * The template is typically a cropped icon bitmap. Returns the template centre on the screen,
     * or null when nothing reaches [threshold] (default 0.7, matching the legacy `nccMatch`).
     */
    suspend fun findByNcc(
        template: Bitmap,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        threshold: Double = 0.7
    ): Point? {
        val t = buildTemplate(template)
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return null
        val w = endX - startX
        val h = endY - startY
        if (w <= 0 || h <= 0 || startX + w > screen.width || startY + h > screen.height) return null
        val sub = Bitmap.createBitmap(screen, startX, startY, w, h)
        val (imgGray, iw) = toGray(sub)
        val ih = imgGray.size / iw
        val hit = match(imgGray, iw, ih, t, 0, 0, iw - 1, ih - 1, threshold) ?: return null
        return Point(startX + hit.x, startY + hit.y)
    }
}
