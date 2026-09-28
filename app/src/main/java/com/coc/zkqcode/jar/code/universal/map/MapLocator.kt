package com.coc.zkqcode.jar.code.universal.map

import android.graphics.Bitmap
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchIn
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchOut
import com.coc.zkqcode.jar.code.universal.CameraState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 地图缩放等级检测与设定（移植自 coc-assist MapLocator）。
 *
 * 原理：村庄外围是一圈深色森林（树木）。镜头越「缩小（zoom-out，拉远）」，屏幕四边露出越多
 * 森林；越「放大（zoom-in，拉近）」，四边是村庄地面、森林占比低。于是采样四边边缘带、统计
 * 森林色占比，即可反推当前缩放等级（0=最近，3=最远）。
 *
 * 给出 0-3 四级（对齐 coc-assist）。阈值与森林色区间按经验给默认值，
 * **需真机复标（T30）**：在 zoom=3 时四边取森林像素回填 [FOREST_MIN]/[FOREST_MAX]，
 * 再据各等级森林占比标定 [ZOOM_RATIO_THRESHOLDS]。
 *
 * 与本项目镜头状态的关系：检测结果写入 [CameraState.mainVillageZoomLevel] /
 * [CameraState.builderBaseZoomLevel]，供其它模块判断是否需要缩小/平移。
 */
object MapLocator {

    /** 四边采样带宽度（屏幕像素，横/竖屏通用）。 */
    private const val EDGE_BAND_PX = 40

    /**
     * 森林色区间（RGB）。CoC 外围森林偏暗绿。
     * 待 T30 复标：用取色工具在 zoom=3 时四边取森林像素，回填精确区间。
     */
    private val FOREST_MIN = intArrayOf(20, 60, 30)   // R,G,B 下限
    private val FOREST_MAX = intArrayOf(90, 140, 90)  // R,G,B 上限

    /**
     * 森林占比 → 缩放等级 的升序阈值（>= 该占比则落入对应等级）。
     * 索引 0 → 达到该占比判为 level 1，索引 1 → level 2，索引 2 → level 3；低于全部则为 level 0。
     * 待 T30 复标。
     */
    private val ZOOM_RATIO_THRESHOLDS = floatArrayOf(0.12f, 0.28f, 0.50f)

    /** 双指手势坐标（pinchIn=缩小/拉远，pinchOut=放大/拉近，见于 ZoomSmallMainBase）。待 T30 复标。 */
    private const val P1X = 141
    private const val P1Y = 423
    private const val P2X = 1052
    private const val P2Y = 352
    private const val P3X = 638
    private const val P3Y = 365

    /** 检测主世界缩放等级（0-3），并写入 [CameraState.mainVillageZoomLevel]；失败返回 -1。 */
    suspend fun detectMainVillageZoomLevel(): Int {
        val level = detectZoomLevel()
        CameraState.mainVillageZoomLevel = level
        return level
    }

    /** 同 [detectMainVillageZoomLevel]，写 [CameraState.builderBaseZoomLevel]。 */
    suspend fun detectBuilderBaseZoomLevel(): Int {
        val level = detectZoomLevel()
        CameraState.builderBaseZoomLevel = level
        return level
    }

    /**
     * 将主世界镜头设定到目标缩放等级（0-3）。
     * 通过反复 pinch（缩小=pinchIn / 放大=pinchOut）单调逼近，每步重新检测避免过冲；
     * 当前已是目标或无法继续（抵达 [maxSteps] 次手势）则停。
     *
     * 注意：CoC 的 pinch 步进较粗，逼近不一定精确；本函数保证「单调逼近」且幂等。
     * 待 T30 复标：pinch 坐标、步进幅度、达到各等级的判定阈值。
     */
    suspend fun setMainVillageZoomLevel(target: Int, maxSteps: Int = 6) {
        if (target !in 0..3) return
        repeat(maxSteps) {
            val cur = detectMainVillageZoomLevel()
            if (cur < 0 || cur == target) return
            if (cur < target) {
                pinchIn(P1X, P1Y, P2X, P2Y, P3X, P3Y)   // 拉远（缩小）
            } else {
                pinchOut(P1X, P1Y, P2X, P2Y, P3X, P3Y)  // 拉近（放大）
            }
            delayWithMultiplier(300)
        }
    }

    /** 通用检测：截屏 → 四边采样 → 森林占比 → 等级。纯算法，与村庄类型无关。 */
    private suspend fun detectZoomLevel(): Int = withContext(Dispatchers.Default) {
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return@withContext -1
        val ratio = forestRatio(screen)
        if (ratio < 0f) return@withContext -1
        var level = 0
        for (i in ZOOM_RATIO_THRESHOLDS.indices) {
            if (ratio >= ZOOM_RATIO_THRESHOLDS[i]) level = i + 1
        }
        level
    }

    /** 计算四边边缘带的森林像素占比（0f~1f）；异常返回 -1f。 */
    private fun forestRatio(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return -1f
        val band = EDGE_BAND_PX.coerceAtMost(minOf(w, h) / 4)
        if (band <= 0) return -1f
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var total = 0
        var forest = 0
        // 上、下边缘带（整行）
        for (y in 0 until band) {
            for (x in 0 until w) {
                if (isForest(pixels[y * w + x])) forest++
                total++
            }
        }
        for (y in h - band until h) {
            for (x in 0 until w) {
                if (isForest(pixels[y * w + x])) forest++
                total++
            }
        }
        // 左、右边缘带（纵向取中间段，避开已采样的四角）
        for (y in band until h - band) {
            for (x in 0 until band) {
                if (isForest(pixels[y * w + x])) forest++
                total++
            }
            for (x in w - band until w) {
                if (isForest(pixels[y * w + x])) forest++
                total++
            }
        }
        if (total == 0) return -1f
        return forest.toFloat() / total.toFloat()
    }

    private fun isForest(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r in FOREST_MIN[0]..FOREST_MAX[0] &&
                g in FOREST_MIN[1]..FOREST_MAX[1] &&
                b in FOREST_MIN[2]..FOREST_MAX[2]
    }
}
