package com.coc.zkqcode.core.util.basic

import android.graphics.Bitmap
import kotlin.math.roundToInt

/**
 * Maps design-time coordinates (the project's 1280x720 landscape baseline) to the actual screen
 * resolution, so tap / region coordinates do not have to be rewritten per device.
 *
 * The project assumes a 1280x720 landscape framebuffer; every hardcoded coordinate in the codebase
 * is expressed in that space. On a device whose capture is not exactly 1280x720, call [init] (or
 * [initFromBitmap]) once at startup with the real resolution, then wrap coordinates with [rx]/[ry]/[r].
 *
 * The mechanical part (this object) is complete; replacing the existing hardcoded literals with
 * [r] is a separate integration pass that should be done once the real device resolution is known
 * (see docs/复用优化方案20260919.md 优化项 5).
 */
object CoordNormalizer {
    const val BASE_WIDTH = 1280
    const val BASE_HEIGHT = 720

    @Volatile
    private var scaleX = 1.0
    @Volatile
    private var scaleY = 1.0
    @Volatile
    private var initialized = false

    /** Configure from the real screen / capture size. */
    fun init(actualWidth: Int, actualHeight: Int) {
        if (actualWidth <= 0 || actualHeight <= 0) return
        scaleX = actualWidth.toDouble() / BASE_WIDTH
        scaleY = actualHeight.toDouble() / BASE_HEIGHT
        initialized = true
    }

    /** Configure from a captured frame's dimensions. */
    fun initFromBitmap(bitmap: Bitmap) = init(bitmap.width, bitmap.height)

    val isInitialized: Boolean
        get() = initialized

    /** Scale a single x coordinate. */
    fun rx(x: Int): Int = (x * scaleX).roundToInt()

    /** Scale a single y coordinate. */
    fun ry(y: Int): Int = (y * scaleY).roundToInt()

    /** Scale an (x, y) pair to the actual resolution. */
    fun r(x: Int, y: Int): Pair<Int, Int> = rx(x) to ry(y)
}
