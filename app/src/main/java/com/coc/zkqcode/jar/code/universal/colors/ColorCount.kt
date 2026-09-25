package com.coc.zkqcode.jar.code.universal.colors

import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import kotlin.math.abs

/**
 * 源 lua 的 getColorNum 等价实现：统计区域内、容差内匹配任一颜色的像素数量。
 *
 * 重要坐标系说明：
 * 源脚本在【竖屏 720×1280】坐标系下扫描；本项目截图是【横屏 1280×720】。
 * 两者是同一画面的 90° 旋转，映射关系为：
 *   横屏 X = 源竖屏 Y
 *   横屏 Y = 720 - 源竖屏 X
 * 因此本文件所有对外接口接收的都是【源竖屏坐标】，内部自动旋转到横屏 buffer 后再统计。
 *
 * 颜色串与源完全一致，均为 BGR 顺序（如 "2973C2-090909"），
 * 切勿再做 RGB↔BGR 转换——[BuilderBaseAttackColors] 中已有的色串也是按 BGR 解析的。
 */

// 源竖屏尺寸：宽 720，高 1280（高即横屏宽）
private const val SRC_W = 720
private const val SRC_H = 1280

/** 单个颜色及其容差（BGR 顺序，t 为每通道容差）。 */
data class ColorTol(val r: Int, val g: Int, val b: Int, val t: Int)

/** 解析 "RRGGBB-TTTTTT|RRGGBB-TTTTTT|..." 形式的颜色容差列表（BGR 顺序）。 */
fun parseBgrToleranceList(raw: String): List<ColorTol> {
    if (raw.isEmpty()) return emptyList()
    return raw.split("|").mapNotNull { part ->
        val (hex, tolHex) = if (part.contains("-")) {
            val idx = part.indexOf("-")
            part.substring(0, idx) to part.substring(idx + 1)
        } else {
            part to "000000"
        }
        if (hex.length < 6) return@mapNotNull null
        val int = hex.toIntOrNull(16) ?: return@mapNotNull null
        // 源色串为 BGR：低字节是 B，高字节是 R
        val b = int and 0xFF
        val g = (int shr 8) and 0xFF
        val r = (int shr 16) and 0xFF
        val t = if (tolHex.length >= 6) (tolHex.toIntOrNull(16) ?: 0) and 0xFF else 0
        ColorTol(r, g, b, t)
    }
}

/**
 * 统计横屏 buffer 中，由源竖屏矩形旋转后区域内、容差内匹配任一颜色的像素数。
 * @param cap 已截取的画面（建议在一次扫描前截取一次后复用，对应源的 keepCapture/releaseCapture）
 * @param sx1,sy1,sx2,sy2 源竖屏坐标系下的矩形（顺序随意，内部取 min/max）
 */
fun countColorsInRegionSource(
    cap: ScreenCaptureManager.CaptureResult,
    sx1: Int, sy1: Int, sx2: Int, sy2: Int,
    colors: List<ColorTol>
): Int {
    if (colors.isEmpty()) return 0
    val w = cap.width
    val h = cap.height
    val buf = cap.buffer
    val stride = cap.rowStride
    // 旋转：横屏 X = 源 Y；横屏 Y = 720 - 源 X
    val rx1 = sy1.coerceIn(0, w - 1)
    val rx2 = sy2.coerceIn(0, w - 1)
    val ry1 = (SRC_W - sx1).coerceIn(0, h - 1)
    val ry2 = (SRC_W - sx2).coerceIn(0, h - 1)
    val xStart = minOf(rx1, rx2)
    val xEnd = maxOf(rx1, rx2)
    val yStart = minOf(ry1, ry2)
    val yEnd = maxOf(ry1, ry2)
    var count = 0
    for (y in yStart..yEnd) {
        for (x in xStart..xEnd) {
            val o = y * stride + x * 4
            val b = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val r = buf.get(o + 2).toInt() and 0xFF
            for (c in colors) {
                if (abs(b - c.b) <= c.t && abs(g - c.g) <= c.t && abs(r - c.r) <= c.t) {
                    count++
                    break
                }
            }
        }
    }
    return count
}
