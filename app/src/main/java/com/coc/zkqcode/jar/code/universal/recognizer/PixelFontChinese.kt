package com.coc.zkqcode.jar.code.universal.recognizer

import android.graphics.Bitmap
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.RunShell
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.fileactions.FileHelper
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 中文像素字库（T11 / T31）。
 *
 * 背景：**源脚本并没有中文字库** —— `doc/decrypt/` 全部 lua 里 `font_chinese / chinese / 中文 /
 * hanzi / 汉字` 均为 0 命中，`函数21a`（L2477）里那份"文字识别库"实际是 0-9 的数字字模。
 * 所以中文字模只能自己采集，无法从源脚本照搬。
 *
 * 定位：**兜底**。项目已接入 ML Kit `ChineseTextRecognizerOptions`（见 [TextRecognizer]），
 * 中文识别以 ML Kit 为主路径；本字库只在 ML Kit 不可用 / 极小字号 / 需要离线复现时用。
 *
 * 字库格式与 [PIXEL_FONT_DIGITS] 完全一致：`字|高,宽|64进制点阵串`（行优先、MSB first、
 * bit=1 为墨、解析时取最后 高*宽 位）。采集方式有两种：
 *  1. PC 侧 `tools/harvest_chinese_font.py`：截图 + 人工标注文字 → 生成字库条目；
 *  2. App 侧 [harvestFromScreen]：用 ML Kit 中文识别当"老师"自动标注，按字等分取框采集。
 *
 * 流水线刻意与 `PixelFontOcr` 同源（二值化 → 8 连通域 → 行分组 → 字库比对），但**独立实现**，
 * 不改动已经过实机验证的数字识别逻辑。
 *
 * 重要约束：中文常用字 3500+，字库不可能全覆盖，且中文字形笔画密集、游戏内还有描边/阴影，
 * 像素匹配的误判率远高于数字。因此**识别不出必须返回空串交回 ML Kit，绝不能硬猜**。
 */
object PixelFontChinese {

    /** 与 PixelFontOcr 相同的 64 进制字符集。 */
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz&#"

    /** 采集字模时写出的文件路径（sdcard 公共目录，沿用 Memory.kt 的 zkqFiles 约定）。 */
    private const val HARVEST_PATH = "/sdcard/zkqFiles/chinese_font.txt"

    /**
     * 中文像素字库条目。初始为空，靠 [harvestFromScreen] 或 `tools/harvest_chinese_font.py`
     * 采集后把输出粘贴到这里（或由采集直接写入 [HARVEST_PATH]）。
     */
    internal const val PIXEL_FONT_CHINESE: String = """
"""

    private class Glyph(val text: String, val width: Int, val height: Int, val bits: BooleanArray)

    /** 编译期字库（惰性解析）。 */
    private val glyphs: List<Glyph> by lazy { parseLibrary(PIXEL_FONT_CHINESE) }

    /** 运行时采集到的字模（不落盘也能立即参与匹配，便于边采集边验证）。 */
    private val runtimeGlyphs = ArrayList<Glyph>()

    /** 采集去重：同一个"字 + 尺寸"只保留第一份。 */
    private val harvestedKeys = HashSet<String>()

    /** 字库是否为空（为空时 [recognize] 直接返回空串，交给 ML Kit）。 */
    fun isEmpty(): Boolean = glyphs.isEmpty() && runtimeGlyphs.isEmpty()

    /** 当前可用字模总数（编译期 + 运行时采集）。 */
    fun size(): Int = glyphs.size + runtimeGlyphs.size

    /**
     * 识别 [bitmap] 里的中文。
     *
     * 参数语义与 `PixelFontOcr.recognize` 一致；[minSimilarity] 对中文建议不低于 0.8，
     * 宁可识别不出也不要错认。
     *
     * @return 识别结果（多行用 `\n` 分隔）；字库为空或全部未命中时返回空串。
     */
    fun recognize(
        bitmap: Bitmap,
        grayMin: Int = 200,
        grayMax: Int = 255,
        maxSaturation: Int = 60,
        minSimilarity: Double = 0.82,
        sizeTolerance: Int = 2,
        maxGlyphSize: Int = 60,
        minGlyphWidth: Int = 6,
        minGlyphHeight: Int = 6,
        unknownChar: Char = '?'
    ): String {
        if (isEmpty()) return ""
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return ""

        val ink = binarize(bitmap, grayMin, grayMax, maxSaturation)
        val components = labelComponents(ink, width, height)
            .filter { it.width in minGlyphWidth..maxGlyphSize && it.height in minGlyphHeight..maxGlyphSize }
            .sortedWith(compareBy<Component> { it.top }.thenBy { it.left })
        if (components.isEmpty()) return ""

        // 汉字/日文字符常见由多个不连通笔画（偏旁/部首）组成，必须先把相邻笔画合并成"字格"再匹配。
        val cells = mergeComponentsIntoCells(components)

        val lines = ArrayList<MutableList<Cell>>()
        for (cell in cells) {
            val line = lines.firstOrNull {
                cell.top <= it.maxOf { c -> c.bottom } && cell.bottom >= it.minOf { c -> c.top }
            }
            if (line != null) line.add(cell) else lines.add(mutableListOf(cell))
        }

        return lines.joinToString("\n") { line ->
            line.sortedBy { it.left }.joinToString("") { cell ->
                matchGlyph(cell, minSimilarity, sizeTolerance) ?: unknownChar.toString()
            }
        }
    }

    /**
     * 截屏后识别指定区域的中文。
     * @return 识别结果；失败/未命中返回 null。
     */
    suspend fun recognizeRegion(
        startX: Int, startY: Int, endX: Int, endY: Int,
        grayMin: Int = 200, grayMax: Int = 255,
        maxSaturation: Int = 60, minSimilarity: Double = 0.82,
        sizeTolerance: Int = 2, maxGlyphSize: Int = 60
    ): String? = withContext(Dispatchers.Default) {
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return@withContext null
        val w = endX - startX
        val h = endY - startY
        if (w <= 0 || h <= 0 || startX + w > screen.width || startY + h > screen.height) return@withContext null
        val cropped = Bitmap.createBitmap(screen, startX, startY, w, h)
        recognize(
            bitmap = cropped,
            grayMin = grayMin, grayMax = grayMax,
            maxSaturation = maxSaturation, minSimilarity = minSimilarity,
            sizeTolerance = sizeTolerance, maxGlyphSize = maxGlyphSize
        ).takeIf { it.isNotEmpty() && it.isNotBlank() }
    }

    /**
     * 用 ML Kit 中文识别当前屏幕，把识别到的每个字**按字等分取框**采集成字模（自动标注）。
     *
     * 原理：ML Kit 的 `Text.Element` 通常是一个词（如"建筑大师"），其中文是等宽的，
     * 所以把 element 的框按字数水平等分，每一份就是一个字的区域，二值化后直接编码成字模。
     * 这样不需要人工标注，跑几轮部落名 / 村庄名 / 玩家名界面就能积累字库。
     *
     * @param onlyChinese 只采集含中文的 element（默认 true，跳过纯数字/英文，避免污染字库）
     * @return 本次新增的字模数量
     */
    suspend fun harvestFromScreen(onlyChinese: Boolean = true): Int = withContext(Dispatchers.IO) {
        val screen = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap ?: return@withContext 0
        val visionText = try {
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            Tasks.await(recognizer.process(InputImage.fromBitmap(screen, 0)))
        } catch (e: Exception) {
            ShowMessage("中文字库采集失败：${e.message}")
            return@withContext 0
        }
        val ink = binarize(screen, 200, 255, 60)
        var added = 0
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    val text = element.text
                    if (text.isEmpty()) continue
                    if (onlyChinese && text.none { it in '\u4e00'..'\u9fff' }) continue
                    // 等分切框的前提是"框宽 = 字数 × 字宽"，一旦 element 里混了数字/英文
                    // （如 "40s"、"5/8"）纯等分就会整体偏移。这里按**加权宽度**切：
                    // 中文占 1 份宽，其它字符按半宽估算，既保住采集量又显著减少偏移。
                    val weights = DoubleArray(text.length) {
                        if (text[it] in '\u4e00'..'\u9fff') 1.0 else 0.5
                    }
                    val totalWeight = weights.sum()
                    if (totalWeight <= 0.0) continue
                    val unit = box.width().toFloat() / totalWeight.toFloat()
                    var cursor = box.left.toFloat()
                    for ((index, ch) in text.withIndex()) {
                        val charWidth = unit * weights[index].toFloat()
                        if (ch in '\u4e00'..'\u9fff') {
                            val left = cursor.toInt()
                            val right = (cursor + charWidth).toInt()
                            val glyph = encodeComponent(
                                ink, screen.width, left, box.top, right, box.bottom, ch.toString()
                            )
                            if (glyph != null && addGlyph(glyph)) added++
                        }
                        cursor += charWidth
                    }
                }
            }
        }
        ShowMessage("中文字库采集：本轮新增 $added 个字模，累计 ${size()} 个")
        flushRejectionLog()
        added
    }

    /** 导出字库文本（可直接粘贴进 [PIXEL_FONT_CHINESE]，或落盘给 PC 侧工具合并）。 */
    fun exportEntries(): String {
        val sb = StringBuilder()
        for (glyph in glyphs + runtimeGlyphs) {
            sb.append(glyph.text).append('|')
                .append(glyph.height).append(',').append(glyph.width).append('|')
                .append(encodeBits(glyph.bits)).append('\n')
        }
        return sb.toString()
    }

    /**
     * 保存前先把 [HARVEST_PATH] 已有字库读回来合并进 [runtimeGlyphs]，
     * 避免每次保存只用"本次会话采集"覆盖掉之前多轮积累的成果。
     */
    private suspend fun loadExistingHarvest() {
        val lines = try {
            RunShell.run("cat $HARVEST_PATH", false)
        } catch (e: Exception) {
            emptyList()
        }
        if (lines.isEmpty()) return
        var merged = 0
        for (glyph in parseLibrary(lines.joinToString("\n"))) {
            if (addGlyph(glyph)) merged++
        }
        if (merged > 0) ShowMessage("中文字库合并已有 $merged 条")
        flushRejectionLog()
    }

    /** 把采集到的字模写到 [HARVEST_PATH]（经 WebSocket 文件通道，与 Memory.kt 同一套机制）。 */
    suspend fun saveHarvested(): Boolean {
        loadExistingHarvest()
        val content = exportEntries()
        if (content.isBlank()) {
            ShowMessage("中文字库为空，无需保存")
            return false
        }
        return FileHelper.writeJson(HARVEST_PATH, content)
    }

    // ---------------------------------------------------------------- 字库编解码

    private fun parseLibrary(library: String): List<Glyph> {
        val result = ArrayList<Glyph>()
        for (line in library.split('\n')) {
            val entry = line.trim()
            if (entry.isEmpty()) continue
            val parts = entry.split('|')
            if (parts.size < 3) continue
            val text = parts[0]
            if (text.isEmpty()) continue
            val dims = parts[1].split(',')
            if (dims.size != 2) continue
            val height = dims[0].toIntOrNull() ?: continue
            val width = dims[1].toIntOrNull() ?: continue
            if (width <= 0 || height <= 0) continue
            val data = parts.subList(2, parts.size).joinToString("|")
            val bits = decodeBits(data, width * height) ?: continue
            result.add(Glyph(text, width, height, bits))
        }
        return result
    }

    /** 64 进制串 → 位数组（取最后 [needed] 位，与 PixelFontOcr 一致）。 */
    private fun decodeBits(data: String, needed: Int): BooleanArray? {
        val raw = StringBuilder(data.length * 6)
        for (c in data) {
            val value = ALPHABET.indexOf(c)
            if (value < 0) continue
            for (i in 5 downTo 0) raw.append(if ((value shr i) and 1 == 1) '1' else '0')
        }
        if (raw.length < needed) return null
        val start = raw.length - needed
        val bits = BooleanArray(needed)
        for (i in 0 until needed) bits[i] = raw[start + i] == '1'
        return bits
    }

    /** 位数组 → 64 进制串（不足 6 位的部分在**前面**补 0，保证解析时取尾部对齐）。 */
    private fun encodeBits(bits: BooleanArray): String {
        val total = bits.size
        val padding = (6 - total % 6) % 6
        val sb = StringBuilder()
        var index = -padding
        while (index < total) {
            var value = 0
            for (b in 0..5) {
                value = value shl 1
                val bitIndex = index + b
                if (bitIndex in 0 until total && bits[bitIndex]) value = value or 1
            }
            sb.append(ALPHABET[value])
            index += 6
        }
        return sb.toString()
    }

    /** 从二值图中裁剪 [left,top,right,bottom) 并编码成字模；区域无墨则返回 null。 */
    private fun encodeComponent(
        ink: BooleanArray, screenWidth: Int,
        left: Int, top: Int, right: Int, bottom: Int,
        text: String
    ): Glyph? {
        val x0 = left.coerceAtLeast(0)
        val y0 = top.coerceAtLeast(0)
        val x1 = right.coerceAtMost(screenWidth)
        val y1 = bottom
        val w = x1 - x0
        val h = y1 - y0
        if (w <= 0 || h <= 0) return null
        val bits = BooleanArray(w * h)
        var hasInk = false
        for (y in 0 until h) {
            for (x in 0 until w) {
                val sx = x0 + x
                val sy = y0 + y
                if (sy * screenWidth + sx !in ink.indices) continue
                if (ink[sy * screenWidth + sx]) {
                    bits[y * w + x] = true
                    hasInk = true
                }
            }
        }
        if (!hasInk) return null
        return Glyph(text, w, h, bits)
    }

    // ---------------------------------------------------------------- 采集去重与校验

    /**
     * 同字样本的最低一致率：低于它说明这条形状与该字已有样本差异过大，
     * 可能是 ML Kit 标错、也可能是切框偏移，宁可不入库。
     */
    private const val SAME_TEXT_MIN_AGREEMENT = 0.80

    /**
     * 跨字"抢标签"阈值：本条与另一个字的样本相似度 ≥ 此值，
     * 判定为 ML Kit 把该字错标成了当前字（如把「模」标成「摸」），直接丢弃。
     */
    private const val CROSS_TEXT_STEAL_AGREEMENT = 0.93

    /** 校验尺寸容差（同一字在不同界面渲染尺寸会差 1~2 px）。 */
    private const val VALIDATE_SIZE_TOLERANCE = 2

    /** 被校验丢弃的条数（汇总输出，避免每条都弹提示刷屏）。 */
    private var rejectedCount = 0

    /** 被丢弃条目的人类可读原因（最多留 8 条样例）。 */
    private val rejectionSamples = ArrayList<String>()

    /**
     * 入库前校验：返回 null 表示通过，否则返回丢弃原因。
     *
     * ML Kit 自动标注会偶发把形近字标错（实测 模→摸、账→顶、并→井、免费→免轰），
     * 这类错误是"像素对、标签错"，单条看不出来，但字库里有同一字的其它样本时就能比对出来。
     */
    private fun inspectGlyph(glyph: Glyph): String? {
        var bestSame: Glyph? = null
        var bestSameScore = 0.0
        var bestOther: Glyph? = null
        var bestOtherScore = 0.0
        var bestOtherText = ""

        for (other in glyphs + runtimeGlyphs) {
            if (abs(other.width - glyph.width) > VALIDATE_SIZE_TOLERANCE) continue
            if (abs(other.height - glyph.height) > VALIDATE_SIZE_TOLERANCE) continue
            val score = glyphAgreement(glyph, other)
            if (other.text == glyph.text) {
                if (score > bestSameScore) {
                    bestSameScore = score
                    bestSame = other
                }
            } else if (score > bestOtherScore) {
                bestOtherScore = score
                bestOtherText = other.text
                bestOther = other
            }
        }

        // 1) 更像另一个字 → 判定 ML Kit 误标，丢弃
        if (bestOther != null && bestOtherScore >= CROSS_TEXT_STEAL_AGREEMENT &&
            bestOtherScore > bestSameScore
        ) {
            return "更像「$bestOtherText」(%.2f)，判定 ML Kit 误标".format(bestOtherScore)
        }
        // 2) 已有同字样本但形状差太多 → 可疑，丢弃
        if (bestSame != null && bestSameScore < SAME_TEXT_MIN_AGREEMENT) {
            return "与已有「${glyph.text}」样本仅 %.2f 一致，判为可疑".format(bestSameScore)
        }
        // 3) 库里还没有可比对样本（首个样本）→ 放行
        return null
    }

    /** 两个字模之间的对齐一致率（与识别时 [paddedAgreement] 同口径）。 */
    private fun glyphAgreement(a: Glyph, b: Glyph): Double {
        val rows = maxOf(a.height, b.height)
        val cols = maxOf(a.width, b.width)
        var matched = 0
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val bitA = y < a.height && x < a.width && a.bits[y * a.width + x]
                val bitB = y < b.height && x < b.width && b.bits[y * b.width + x]
                if (bitA == bitB) matched++
            }
        }
        return matched.toDouble() / (rows * cols).toDouble()
    }

    /**
     * 采集入库：先做 [inspectGlyph] 一致性校验，通过后才写入，避免误标污染字库。
     * @return 是否新入库
     */
    private fun addGlyph(glyph: Glyph): Boolean {
        val key = glyph.text + "_" + glyph.width + "x" + glyph.height
        if (harvestedKeys.contains(key)) return false
        val reason = inspectGlyph(glyph)
        if (reason != null) {
            rejectedCount++
            if (rejectionSamples.size < 8) rejectionSamples.add("「${glyph.text}」" + reason)
            return false
        }
        harvestedKeys.add(key)
        runtimeGlyphs.add(glyph)
        return true
    }

    /** 输出并清空校验丢弃汇总（每轮采集/合并结束时调用）。 */
    private fun flushRejectionLog() {
        if (rejectedCount == 0) return
        val detail = rejectionSamples.joinToString("；")
        ShowMessage("字模校验丢弃 $rejectedCount 条：$detail")
        rejectedCount = 0
        rejectionSamples.clear()
    }

    // ---------------------------------------------------------------- 识别管线

    private interface Maskable {
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        val width: Int
        val mask: BooleanArray
    }

    private class Component(
        override val left: Int, override val top: Int, override val right: Int, override val bottom: Int,
        override val mask: BooleanArray, override val width: Int, val height: Int
    ) : Maskable

    /**
     * 把多个相邻的笔画/偏旁合并成一个"字格"。
     * 中文字符（如"明=日+月"、"性=忄+生"）经二值化后往往不是单一连通域，
     * 直接按连通域匹配会导致拆字；合并后再与字库中完整的单字字形比对。
     */
    private class Cell(
        override val left: Int, override val top: Int, override val right: Int, override val bottom: Int,
        override val mask: BooleanArray, override val width: Int, val height: Int
    ) : Maskable

    private fun binarize(bitmap: Bitmap, grayMin: Int, grayMax: Int, maxSaturation: Int): BooleanArray {
        val width = bitmap.width
        val height = bitmap.height
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
        return ink
    }

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

    private fun mergeComponentsIntoCells(components: List<Component>): List<Cell> {
        if (components.isEmpty()) return emptyList()
        val sorted = components.sortedBy { it.left }
        val cells = ArrayList<Cell>()
        for (comp in sorted) {
            val last = cells.lastOrNull()
            val merged = if (last != null && shouldMergeCells(last, comp)) {
                val newLeft = minOf(last.left, comp.left)
                val newTop = minOf(last.top, comp.top)
                val newRight = maxOf(last.right, comp.right)
                val newBottom = maxOf(last.bottom, comp.bottom)
                val newW = newRight - newLeft + 1
                val newH = newBottom - newTop + 1
                val newMask = BooleanArray(newW * newH)
                copyMask(last, newMask, newLeft, newTop, newW)
                copyMask(comp, newMask, newLeft, newTop, newW)
                cells.removeLast()
                Cell(newLeft, newTop, newRight, newBottom, newMask, newW, newH)
            } else {
                Cell(comp.left, comp.top, comp.right, comp.bottom, comp.mask, comp.width, comp.height)
            }
            cells.add(merged)
        }
        return cells
    }

    private fun shouldMergeCells(cell: Cell, comp: Component): Boolean {
        val newLeft = minOf(cell.left, comp.left)
        val newTop = minOf(cell.top, comp.top)
        val newRight = maxOf(cell.right, comp.right)
        val newBottom = maxOf(cell.bottom, comp.bottom)
        val newWidth = newRight - newLeft + 1
        val newHeight = newBottom - newTop + 1
        // 合并后整体应近似方形；太宽说明是两个独立字符。
        if (newWidth > newHeight * 1.5 + 4) return false
        val gap = comp.left - cell.right
        if (gap < 0) return true // 有重叠直接合并
        val refHeight = maxOf(cell.height, comp.height)
        val maxGap = maxOf((refHeight * 0.25).toInt(), 2)
        if (gap > maxGap) return false
        val overlapTop = maxOf(cell.top, comp.top)
        val overlapBottom = minOf(cell.bottom, comp.bottom)
        val overlapHeight = overlapBottom - overlapTop
        val minHeight = minOf(cell.height, comp.height)
        return overlapHeight >= minHeight * 0.4
    }

    private fun copyMask(source: Maskable, into: BooleanArray, baseLeft: Int, baseTop: Int, stride: Int) {
        val left = source.left
        val top = source.top
        val right = source.right
        val bottom = source.bottom
        val mask = source.mask
        val width = source.width
        for (y in top..bottom) {
            for (x in left..right) {
                if (mask[(y - top) * width + (x - left)]) {
                    into[(y - baseTop) * stride + (x - baseLeft)] = true
                }
            }
        }
    }

    private fun matchGlyph(cell: Cell, minSimilarity: Double, sizeTolerance: Int): String? {
        var bestText: String? = null
        var bestScore = 0.0
        for (glyph in glyphs + runtimeGlyphs) {
            if (abs(glyph.width - cell.width) > sizeTolerance) continue
            if (abs(glyph.height - cell.height) > sizeTolerance) continue
            val score = paddedAgreement(glyph, cell)
            if (score > bestScore) {
                bestScore = score
                bestText = glyph.text
            }
        }
        return bestText?.takeIf { bestScore >= minSimilarity }
    }

    private fun paddedAgreement(glyph: Glyph, cell: Cell): Double {
        val rows = maxOf(glyph.height, cell.height)
        val cols = maxOf(glyph.width, cell.width)
        var matched = 0
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val glyphBit = y < glyph.height && x < glyph.width && glyph.bits[y * glyph.width + x]
                val inkBit = y < cell.height && x < cell.width && cell.mask[y * cell.width + x]
                if (glyphBit == inkBit) matched++
            }
        }
        return matched.toDouble() / (rows * cols).toDouble()
    }
}
