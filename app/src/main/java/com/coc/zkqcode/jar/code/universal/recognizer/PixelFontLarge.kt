package com.coc.zkqcode.jar.code.universal.recognizer

import android.graphics.Bitmap
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 大字号数字 OCR（源 函数315a，19~21 宽点阵字）。
 *
 * 游戏内大号数字（战后掠夺资源、部落战星数、CWL 奖章、联赛积分、都城币等）用一套
 * 比资源栏更大的点阵字体绘制。复用 [PixelFontOcr] 的通用引擎，这里只给出「大字号」
 * 调优参数与便捷区域读取，避免每个调用方各自写死阈值。
 *
 * 关键参数（对照源 函数315a 二值化 0-251）：
 *  - 大号数字为亮色文字（白/黄）压在深色或彩色底上，故 ink = 高亮度 & 低饱和；
 *  - 字高约 15~21px，故放宽 maxGlyphSize / sizeTolerance，并保证 minGlyphHeight 足够大，
 *    以免把噪点当成数字。
 *  - 字库见 [PIXEL_FONT_DIGITS]（含大字号变体），由 tools/harvest_pixel_font.py 重新采集。
 *
 * 真机复标项（T30）：大字号精确亮/饱和区间与字库需要按当前客户端重采。
 */
object PixelFontLarge {

    /** 大字号数字识别：默认参数按亮色大号文字调优。 */
    fun recognize(
        bitmap: Bitmap,
        minSimilarity: Double = 0.72,
        maxSaturation: Int = 60,
        grayMin: Int = 180,
        grayMax: Int = 255,
    ): String = PixelFontOcr.recognize(
        bitmap = bitmap,
        grayMin = grayMin,
        grayMax = grayMax,
        maxSaturation = maxSaturation,
        minSimilarity = minSimilarity,
        sizeTolerance = 4,
        maxGlyphSize = 60,
        minGlyphWidth = 5,
        minGlyphHeight = 10,
    )

    /** 截屏指定区域识别大字号数字；无截图或无内容返回 null。坐标基于当前截屏分辨率（横/竖屏均可）。 */
    suspend fun recognizeRegion(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        minSimilarity: Double = 0.72,
        maxSaturation: Int = 60,
        grayMin: Int = 180,
        grayMax: Int = 255,
    ): String? = withContext(Dispatchers.Default) {
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return@withContext null
        val w = endX - startX
        val h = endY - startY
        if (w <= 0 || h <= 0 || startX + w > screen.width || startY + h > screen.height) {
            return@withContext null
        }
        val cropped = Bitmap.createBitmap(screen, startX, startY, w, h)
        recognize(cropped, minSimilarity, maxSaturation, grayMin, grayMax).takeIf { it.isNotEmpty() }
    }

    /** 从识别串里抠出纯数字与分隔符（用于 "1,234" / "12/40" 这类形态）。 */
    fun digitsOnly(s: String): String = s.filter { it.isDigit() || it == '/' || it == ',' || it == '.' }
}
