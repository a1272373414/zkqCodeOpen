package com.coc.zkqcode.jar.code.universal.deploy

import android.graphics.Point
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Battlefield deploy geometry, ported verbatim from the legacy source project
 * (`cocfz-apk-test`, `doc/decrypt/awcocx_main.lua`).
 *
 * The legacy script only supports a PORTRAIT 720x1280 / dpi320 device (its own guard asks for
 * "请把分辨率设置竖屏：720*1280-dpi：320"), while this project plays in landscape 1280x720. Both
 * are the same rectangle, so the conversion is a pure 90 degree rotation at a 1:1 pixel scale -
 * no rescaling and no re-calibration is involved:
 *
 *     realX = sourceY
 *     realY = SCREEN_H - sourceX
 *
 * The legacy deploy model that this file reproduces:
 *  - `函数327a(上下, 兵数, 放兵类型, 波)` splits `兵数` over the quadrants of one side
 *    (四面: `兵数/4` per quadrant, single side: all of `兵数` on `援兵位置`);
 *  - every quadrant owns a deploy LINE with 开始(start) / 结束(end) / 中间(middle) / 总长(length);
 *  - `函数151a(兵数)` cuts 总长 into `兵数` equal steps;
 *  - `函数147a/148a/149a/150a` walk that line, each tap landing in the MIDDLE of one sub-segment
 *    (`开始 + 偏移 - (前进 + 前进/2)`);
 *  - 炸弹 / 地震 and the trailing "extra" taps use `中间 + 偏移` instead.
 *
 * All arithmetic below stays in the SOURCE portrait frame (so the numbers can be compared with the
 * lua line by line) and is converted to real pixels only by [sourceToReal] at the very end.
 */
object DeployGeometry {
    const val SCREEN_W = 1280
    const val SCREEN_H = 720

    /** Source `XY差值`: in the portrait frame 1px across ~ 1.334px down. */
    const val XY_RATIO = 1.334134615384615

    /** Source `结束X = 开始X - 200`. */
    private const val LINE_LENGTH = 200.0

    /**
     * Real length of an `N` source-pixel offset. The real offset of a quadrant is
     * `(±N * XY_RATIO, ±N)`, so its length is `N * sqrt(1 + XY_RATIO^2)` (~1.667 * N).
     */
    private val OFFSET_SCALE = sqrt(1.0 + XY_RATIO * XY_RATIO)

    /**
     * 图腾's inward pull for WAVE 1, in REAL pixels on purpose: this is the unit the placement is
     * tuned in on screen. The source used a fixed `95` source px here (~158 real px), which put the
     * totem too close to the deploy ring; this is the single knob to turn when tuning the whole
     * totem chain in or out (bigger = closer to the battlefield centre, every wave shifts with it).
     */
    private const val TOTEM_INWARD_REAL = 100.0

    /**
     * 图腾 波次递进, ported from the source's `放图腾` / `图腾波数` (awcocx_main.lua L26904 + L27730).
     *
     * The source fires 5 totem waves (`间隔` = 0 / 8000 / 15000 / 22000 / 28000 ms after the army
     * finished deploying) and passes the wave index to `函数327a`, which maps 波1..4 to
     * 90 / 140 / 190 / 210 and 波>4 to 230 source px of inward pull. Only the *steps* are taken from
     * the source: wave 1 stays on [TOTEM_INWARD_REAL] so the on-screen tuning above is preserved.
     */
    private val TOTEM_WAVE_DELTA_SOURCE = doubleArrayOf(0.0, 50.0, 100.0, 120.0, 140.0)

    /** Source px added to 图腾's pull for [wave] (1-based; clamps to the last table entry). */
    private fun totemWaveDelta(wave: Int): Double =
        TOTEM_WAVE_DELTA_SOURCE[(wave - 1).coerceIn(0, TOTEM_WAVE_DELTA_SOURCE.lastIndex)]

    /**
     * Argument of the source `函数98a(缩进直)`: the main path passes `-60` (`-20` for the 清边
     * troops). A negative value pushes 开始 OUTWARD and pulls 结束 INWARD by the same amount, so
     * 中间 never moves while 总长 becomes `200 - 2 * 缩进直` (= 320 for the default).
     */
    var indent: Int = -60

    /** One quadrant deploy line, in source portrait coordinates. */
    class Line internal constructor(
        val startX: Double,
        val startY: Double,
        val endX: Double,
        val endY: Double
    ) {
        val middleX: Double get() = (startX + endX) / 2
        val middleY: Double get() = (startY + endY) / 2

        /** Source `总长`. */
        val totalLength: Double get() = startX - endX

        /**
         * Direction of 结束 relative to 开始 along the source Y axis. The source `函数98a` and
         * `函数147a..函数150a` all take their Y sign from it (左上 / 右下 run down-left, 右上 /
         * 左下 run down-right in the portrait frame).
         */
        val ySign: Double get() = if (endY < startY) -1.0 else 1.0
    }

    /**
     * Source quadrant seeds, i.e. the `走行 == 0` branch of the battle geometry: the four seeds
     * are `开始X/Y`, then `结束X = 开始X - 200` and `结束Y = 开始Y ± 200 * XY差值`.
     *
     * The runtime boundary scan (`函数121a/122a/131a/137a`, which refines 开始/结束 through the
     * 走行 counters) is NOT ported: the seeds are exactly what the source itself falls back to and
     * they already describe the deployable ring hugging the base diamond.
     */
    private class Seed(val side: DeploySide, val sx: Double, val sy: Double, val ySign: Double)

    private val SEEDS = listOf(
        Seed(DeploySide.TOP_LEFT, 580.0, 507.0, -1.0),
        Seed(DeploySide.TOP_RIGHT, 580.0, 763.0, +1.0),
        Seed(DeploySide.BOTTOM_LEFT, 413.0, 237.0, +1.0),
        Seed(DeploySide.BOTTOM_RIGHT, 413.0, 1029.0, -1.0)
    )

    private val LINES: Map<DeploySide, Line> = SEEDS.associate { seed ->
        val i = indent.toDouble()
        seed.side to Line(
            // 函数98a: 开始X -= 缩进直, 开始Y ± 缩进直 * XY差值
            startX = seed.sx - i,
            startY = seed.sy + seed.ySign * i * XY_RATIO,
            // 结束X += 缩进直, 结束Y ∓ 缩进直 * XY差值
            endX = seed.sx - LINE_LENGTH + i,
            endY = seed.sy + seed.ySign * LINE_LENGTH * XY_RATIO - seed.ySign * i * XY_RATIO
        )
    }

    /** Converts a source portrait point into this project's landscape frame. */
    fun sourceToReal(sourceX: Double, sourceY: Double): Point =
        Point(sourceY.roundToInt(), (SCREEN_H - sourceX).roundToInt())

    /** The source deploy line of [side]. */
    fun line(side: DeploySide): Line = LINES.getValue(side)

    /** 开始 of [side] in real pixels. */
    fun start(side: DeploySide): Point = line(side).let { sourceToReal(it.startX, it.startY) }

    /** 结束 of [side] in real pixels. */
    fun end(side: DeploySide): Point = line(side).let { sourceToReal(it.endX, it.endY) }

    /** 中间 of [side] in real pixels. */
    fun middle(side: DeploySide): Point = line(side).let { sourceToReal(it.middleX, it.middleY) }

    /**
     * The source `偏移X/偏移Y` signs for an inward pull of `n` source pixels:
     * X is `-n` on the 上 quadrants and `+n` on the 下 ones, Y is `+n * XY差值` on the 左
     * quadrants and `-n * XY差值` on the 右 ones.
     */
    private fun offsetPair(side: DeploySide, n: Double): Pair<Double, Double> {
        val offX = if (side.isTop) -n else n
        val offY = if (side.isLeft) n * XY_RATIO else -n * XY_RATIO
        return offX to offY
    }

    /**
     * The source `函数327a` per-type inward pull `N`, in source pixels.
     *
     * Only 图腾 is wave-dependent: wave 1 is anchored on [TOTEM_INWARD_REAL] (given in real pixels
     * because that is the unit the placement is tuned in on screen) and every later wave adds the
     * source's own step ([TOTEM_WAVE_DELTA_SOURCE]), so the totems march toward the centre.
     */
    fun typeOffsetN(type: DeployType, wave: Int): Double = when (type) {
        DeployType.TOTEM -> TOTEM_INWARD_REAL / OFFSET_SCALE + totemWaveDelta(wave)
        else -> type.inwardOffset.toDouble()
    }

    /**
     * 函数147a/148a/149a/150a: the [index]-th tap of a `count`-way split of the [side] line,
     * i.e. `开始 + 偏移 - (前进 + 前进/2)` with `前进 = index * 总长 / count`.
     */
    fun spreadTap(
        side: DeploySide,
        index: Int,
        count: Int,
        type: DeployType,
        wave: Int = 0
    ): Point {
        val l = line(side)
        val n = count.coerceAtLeast(1)
        val advance = (index + 0.5) * (l.totalLength / n)
        val (offX, offY) = offsetPair(side, typeOffsetN(type, wave))
        return sourceToReal(
            l.startX + offX - advance,
            l.startY + offY + l.ySign * advance * XY_RATIO
        )
    }

    /** `中间 + 偏移` tap, used by 炸弹 / 地震 and by every trailing "extra" tap. */
    fun middleTap(side: DeploySide, type: DeployType, wave: Int = 0): Point {
        val l = line(side)
        val (offX, offY) = offsetPair(side, typeOffsetN(type, wave))
        return sourceToReal(l.middleX + offX, l.middleY + offY)
    }

    /** `中间 + 偏移` tap with a raw inward pull `n` instead of a type (地震's 80/160 fan-out). */
    fun middleTapAtOffset(side: DeploySide, type: DeployType, n: Double, wave: Int = 0): Point {
        val l = line(side)
        val (typeX, typeY) = offsetPair(side, typeOffsetN(type, wave))
        val (fanX, fanY) = offsetPair(side, n)
        return sourceToReal(l.middleX + typeX + fanX, l.middleY + typeY + fanY)
    }

    /**
     * 函数142a「单面真滑屏」fingers: finger 1 is held 5 source px to the right of 中间 and 5 source
     * px toward the battlefield centre on Y; finger 2 sits exactly on 中间. Every stroke moves
     * finger 1 to 开始 and finger 2 to 结束, then back.
     */
    fun holdPoint(side: DeploySide): Point {
        val l = line(side)
        return sourceToReal(l.middleX + 5, l.middleY - 5 * l.ySign)
    }

    /**
     * 部署栏翻页滑动的 y（源 `函数135a/136a` 的"长滑"）。
     *
     * The source swipes along its portrait x=157 (just beside the bar strip), which after the
     * rotation becomes a HORIZONTAL swipe along the bar; y is kept inside the bar's card row
     * (the bar region is y 587..710) so the game treats it as a bar scroll.
     */
    private const val BAR_SWIPE_Y = 650
    private const val BAR_SWIPE_FROM_X = 1150
    private const val BAR_SWIPE_TO_X = 250

    /**
     * 部署栏翻页滑动起终点（实际像素）: [forward] = 左滑看后面的兵种（源 `函数135a`），
     * false = 右滑翻回（源 `函数136a`）。
     */
    fun barSwipe(forward: Boolean): IntArray =
        if (forward) {
            intArrayOf(BAR_SWIPE_FROM_X, BAR_SWIPE_Y, BAR_SWIPE_TO_X, BAR_SWIPE_Y)
        } else {
            intArrayOf(BAR_SWIPE_TO_X, BAR_SWIPE_Y, BAR_SWIPE_FROM_X, BAR_SWIPE_Y)
        }

    /** The two quadrants making up the "上" (top) side. */
    val topSides: List<DeploySide> get() = listOf(DeploySide.TOP_LEFT, DeploySide.TOP_RIGHT)

    /** The two quadrants making up the "下" (bottom) side. */
    val bottomSides: List<DeploySide> get() = listOf(DeploySide.BOTTOM_LEFT, DeploySide.BOTTOM_RIGHT)
}
