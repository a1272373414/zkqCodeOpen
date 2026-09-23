package com.coc.zkqcode.jar.code.universal.colors

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager

/**
 * 按钮/图标"模板匹配"入口 —— [NccMatcher] 的接线层（task.md T02）。
 *
 * 为什么需要它：`findMultiColors` 要求逐点颜色完全命中，识别**文字按钮**时要手工量
 * 主色 + 大量偏移点，成本高且对底色/状态变化敏感；NCC 模板匹配只要求"形状稳定"，
 * 标定方式从"逐点量色"降为"裁一张按钮小图"（见 `tools/make_template.py`）。
 *
 * 用法：
 * ```
 * // 1) 用工具裁剪模板到 assets/templates/<name>.png：
 * //    python tools/make_template.py 截图.png x1 y1 x2 y2 btn_xxx
 * // 2) 运行时限区域搜索（区域越小越快，建议贴紧按钮所在栏/面板）：
 * val p = TemplateMatcher.find("btn_xxx", 1050, 38, 1245, 78, threshold = 0.7)
 * if (p != null) { TouchActions.tap(p.x, p.y) }
 * ```
 *
 * 注意：模板是该按钮在小尺寸 UI 上的**原尺寸**灰度外观，分辨率变化（非 1280x720）会失配。
 */
object TemplateMatcher {

    private const val DIR = "templates"

    /** 模板名 -> 已解码 Bitmap（避免每次匹配都读 assets）。 */
    private val cache = HashMap<String, Bitmap>()

    /** 从 assets/templates/<name>.png 加载模板（带缓存）；不存在时返回 null。 */
    fun load(name: String): Bitmap? {
        cache[name]?.let { return it }
        val ctx = ScreenCaptureManager.getContext() ?: return null
        val bitmap = try {
            ctx.assets.open("$DIR/$name.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
        return bitmap?.also { cache[name] = it }
    }

    /**
     * 在屏幕区域 [`startX`,`startY`]..[`endX`,`endY`] 内匹配模板 [name]，
     * 返回模板中心坐标；未达到 [threshold] 时返回 null。
     */
    suspend fun find(
        name: String,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        threshold: Double = 0.75
    ): Point? {
        val template = load(name) ?: return null
        return NccMatcher.findByNcc(template, startX, startY, endX, endY, threshold)
    }

    /** 便捷重载：在整屏（1280x720）内匹配。整屏 NCC 较慢，仅在必要时使用。 */
    suspend fun find(name: String, threshold: Double = 0.75): Point? =
        find(name, 0, 0, 1280, 720, threshold)
}
