package com.coc.zkqcode.jar.code.universal.recognizer

import android.graphics.Rect

/**
 * 面向业务的中文读取封装（T11 / T32）：部落名 / 村庄名 / 玩家名 / 竞赛任务描述。
 *
 * 项目此前已经接入 ML Kit 中文识别（[TextRecognizer] 的 `ChineseTextRecognizerOptions`），
 * 但调用方要自己拼坐标、自己拼字符串、自己处理"识别不出来"的情况，容易各写各的。
 * 这里统一成"给一个区域，返回一段干净文本"的入口，并补上像素字库兜底：
 *
 *  1. 先用 ML Kit 中文识别（主路径，覆盖任意汉字）；
 *  2. ML Kit 的结果并非照单全收，而是过了「两道收紧闸」才采信：
 *     - 阈值闸 [minConfidence]：只保留置信度达标的行（<=0 关闭）；
 *     - 逻辑闸 [requireCJK]：结果必须含至少一个中文字，避免把图标/噪声当文字（英文名场景关掉）；
 *  3. 没过闸（或 ML Kit 没读到东西）时，再用 [PixelFontChinese] 像素字库兜底（离线、快，但只认采集过的字）；
 *  4. 两者都没有结果就返回 null —— **绝不能返回硬猜的文本**，调用方必须自己决定降级策略。
 *
 * TODO(真机标定)：各业务区域坐标待实机确认，先用调用方传入的矩形，不写死。
 */
object ChineseTextReader {

    /**
     * 读取指定区域的中文文本。
     *
     * @param usePixelFontFallback ML Kit 无结果时是否用像素字库兜底（默认 true）
     * @return 识别到的文本（已去掉首尾空白）；识别不出返回 null
     */
    suspend fun read(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        threshold: Int = 140,
        scale: Float = 2f,
        usePixelFontFallback: Boolean = true,
        // 收紧闸1（阈值）：只采信 ML Kit 置信度 >= 此值的行；<=0 表示不过滤
        minConfidence: Float = 0f,
        // 收紧闸2（逻辑）：结果必须含至少一个中文字才采信，否则视为没读到（避免把图标/噪声当文字）
        // 英文名（如玩家名）场景传 false 关闭
        requireCJK: Boolean = true
    ): String? {
        val mlkit = TextRecognizer.recognize(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            useChinese = true,
            threshold = threshold,
            invertBinarization = true,
            scale = scale
        )
        // 仅保留置信度达标的行
        val kept = if (minConfidence > 0f) mlkit.filter { it.confidence >= minConfidence } else mlkit
        val text = kept.joinToString("") { it.text }.trim()
        // 中文读取器默认要求结果里至少含一个中文字；否则不采信，继续走像素字库兜底
        if (text.isNotEmpty() && (!requireCJK || hasCJK(text))) return text

        if (usePixelFontFallback && !PixelFontChinese.isEmpty()) {
            PixelFontChinese.recognizeRegion(startX, startY, endX, endY)?.trim()?.let {
                if (it.isNotEmpty()) return it
            }
        }
        return null
    }

    /** 判断字符串是否包含至少一个中文字（CJK 统一表意文字）。 */
    private fun hasCJK(s: String): Boolean = s.any { Character.isIdeographic(it.code) }

    /**
     * 判断指定区域的文本里是否包含任一关键字（用于"任务描述里有没有『建筑大师』"这类判定）。
     *
     * 中文 OCR 不可能 100% 准确，所以这里做的是**包含匹配**：只要读到的文本里出现关键字
     * （或去掉空格后仍包含）就算命中，避免因为个别字识别错就漏判。
     */
    suspend fun containsAny(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        keywords: List<String>,
        threshold: Int = 140,
        scale: Float = 2f,
        minConfidence: Float = 0f,
        requireCJK: Boolean = true
    ): Boolean {
        val text = read(
            startX, startY, endX, endY, threshold, scale, true, minConfidence, requireCJK
        ) ?: return false
        val compact = text.replace(Regex("\\s"), "")
        return keywords.any { keyword ->
            text.contains(keyword) || compact.contains(keyword.replace(Regex("\\s"), ""))
        }
    }

    /**
     * 按文字定位按钮：识别区域 → 找到含关键字的行 → 返回该行在**屏幕坐标**下的包围盒。
     *
     * 典型用法：
     * ```kotlin
     * ChineseTextReader.locate("进攻", startX, startY, endX, endY)?.let {
     *     TouchActions.tap(it.centerX(), it.centerY())
     * }
     * ```
     *
     * 坐标反算：ML Kit 返回的 [Rect] 是「裁剪 + 放大后 bitmap」里的坐标，必须
     * `/ scale` 并加上裁剪偏移 (startX,startY) 才是真实屏幕坐标，否则点击整体偏移。
     *
     * @param keyword 要找的文案（子串匹配，抗空白）
     * @return 命中行在屏幕坐标下的包围盒；没找到返回 null
     */
    suspend fun locate(
        keyword: String,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        threshold: Int = 140,
        scale: Float = 2f,
        minConfidence: Float = 0f,
        requireCJK: Boolean = true
    ): Rect? {
        val mlkit = TextRecognizer.recognize(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            useChinese = true,
            threshold = threshold,
            invertBinarization = true,
            scale = scale
        )
        // 复用双闸：仅保留置信度达标的行
        val kept = if (minConfidence > 0f) mlkit.filter { it.confidence >= minConfidence } else mlkit
        val target = keyword.replace(Regex("\\s"), "")
        // 子串匹配，抗空白（"建筑大师" 也能匹配 "建筑 大师"）
        val hit = kept.firstOrNull { line ->
            line.text.replace(Regex("\\s"), "").contains(target)
        } ?: return null
        val box = hit.position ?: return null
        // 反算回屏幕坐标：box 处于放大后的 bitmap，需 /scale 并加裁剪偏移
        val left = (startX + box.left / scale).toInt()
        val top = (startY + box.top / scale).toInt()
        val right = (startX + box.right / scale).toInt()
        val bottom = (startY + box.bottom / scale).toInt()
        return Rect(left, top, right, bottom)
    }
}
