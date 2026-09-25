package com.coc.zkqcode.jar.code.builderbase.attack

import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.colorpackage.builderbase.BuilderBaseAttackColors
import com.coc.zkqcode.jar.code.universal.colors.countColorsInRegionSource
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsAll
import com.coc.zkqcode.jar.code.universal.colors.parseBgrToleranceList
import kotlinx.coroutines.delay
import java.util.Random

/**
 * 源项目（awcocx_main.lua）夜世界下兵方案的忠实移植（保真版）。
 *
 * 对应关系：
 *   - 函数123a  → [deployTroopsSource]：选方向、算四向下兵几何（含动态边界扫描）、英雄下放、兵种识别并逐一下兵
 *   - 函数323a  → [dualFingerDeploy]：双指滑屏放兵
 *   - 函数128a  → [builderBaseAttackSource] 内的战斗监控（技能释放复用现有 [releaseSkillsLoop]）
 *   - 函数121a/122a/129a/130a → [scanUpper]/[scanLower]/[scan129a]/[scan130a]：走行边界扫描
 *   - 函数131a/132a → [cameraSwipeUp]/[cameraSwipeDown]：相机上下滑动
 *
 * 颜色注意事项：源 lua 的色串为 BGR 顺序，本项目 ColorSchema 同样按 BGR 解析，
 * 因此 [BuilderBaseAttackColors] 中已有的色串**原样复用，切勿再做 RGB↔BGR 转换**。
 * 坐标注意事项：源为竖屏 720×1280，本项目为横屏 1280×720，
 * 映射为 横屏X=源Y，横屏Y=720-源X（仅坐标/偏移旋转，颜色不变）。
 */

// 源 XY差值（≈1.334，与 DeployGeometry.XY_RATIO 一致）
private const val XY_RATIO = 1.334

// 双指滑屏循环次数（源 cishu = 2）
private const val CISHU = 2

// 英雄（第一区域）单英雄下放的最大重试轮次：每轮换方向重建几何换落点
private const val HERO_MAX_ROUNDS = 3

// ───────────────── 夜世界下兵方案3 配置 ─────────────────
// 方案3：卡位式从左到右下兵，点卡位后检测选中白框，无白框=已下完；
//       每个卡位选中后在落点点4次，最多6轮；落点无效时换下一个候选落点。

// 第一区域（第一场）：1英雄 + 6兵种 = 7个卡位
// 第二区域（第二场）：1英雄 + 8兵种 = 9个卡位
// 注意：以下 x 坐标为横屏 1280×720 下卡位中心近似值，需根据实机截图校准。
private val AREA1_SLOT_XS = intArrayOf(80, 200, 320, 440, 560, 680, 800)
private val AREA2_SLOT_XS = intArrayOf(70, 165, 260, 355, 450, 545, 640, 735, 830)
private const val SLOT_CENTER_Y = 640

// 白框检测：点击卡位后，在 [x±45, y±45] 区域内统计高亮像素；
// 亮度阈值 0xE0，数量阈值 80（选中时出现白框，未选中时极少）。
private const val WHITE_BRIGHT_THRESHOLD = 0xE0
private const val WHITE_BORDER_MIN_COUNT = 80
private const val WHITE_CHECK_HALF_SIZE = 45

// 每个落点点击次数、最大轮次
private const val SOURCE3_TAPS_PER_POINT = 4
private const val SOURCE3_MAX_ROUNDS = 6

// 方案3 区域计数：每进程第1次调用=第一区域（6兵），第2次=第二区域（8兵）。
// （与 realAttack 中“已进入第二区域”对应，每场夜世界对战会触发两次 normalBattle。）
private var source3AreaCallCount = 0

// 源竖屏宽（= 横屏高）
private const val SRC_W = 720

// 第二村庄下兵：源脚本按当前进攻的第几个基地决定；本项目固定走带扫描的主路径。
// 若需切到"第二村庄"（用硬编码几何、不扫描），把此处改为 true 即可。
private const val SECOND_VILLAGE = false

// 边界扫描色系开关：false=用 函数121a/122a（4053AE，与源 函数123a 实际调用一致）；
// true=改用 函数129a/130a（2973C2，doc 描述的边界扫描）。两套都已移植，默认走前者。
private var altBoundaryScan = false

// 边界扫描用的两套蓝色边界色串（BGR，原样照搬源 lua）
private const val BLUE_2973 =
    "2973C2-090909|1E5E9E-090909|15419B-101010|1C53B9-101010|256AC6-101010|307FD8-090909|2777BA-060606|328AC9-030303|3084AD-070707|2B6BE2-101010|1A338D-101010"
private const val BLUE_4053 =
    "4053AE-101010|3D4BAB-090909|4D5998-050505|434B81-060606|404882-060606|313D96-090909|3F528A-090909|4E6294-090909|0F1C2C-070707|6654A7-090909|352171-090909|3F428D-070707|5A4293-090909|383B7B-090909|4A3CB1-090909|554BB7-090909|2A1A87-090909"

// 模块级状态
private var direction = "左上"           // 蛮王位置
private var walkLT = 0                   // 左上走行
private var walkRT = 0                   // 右上走行
private var walkLB = 0                   // 左下走行
private var walkRB = 0                   // 右下走行

// 四向几何（源竖屏坐标）：开始/结束/中间（中间由开始+结束算得）
private data class Quad(var startX: Int, var startY: Int, var endX: Int, var endY: Int) {
    val midX: Int get() = (startX + endX) / 2
    val midY: Int get() = (startY + endY) / 2
}

// 各向几何与"外围"（女巫专用）几何
private lateinit var ltQuad: Quad
private lateinit var rtQuad: Quad
private lateinit var lbQuad: Quad
private lateinit var rbQuad: Quad
private lateinit var ltOuter: Quad
private lateinit var rtOuter: Quad
private lateinit var lbOuter: Quad
private lateinit var rbOuter: Quad

/** 源竖屏坐标 → 横屏坐标：横屏X=源Y，横屏Y=720-源X。 */
private fun toRealX(sx: Int, sy: Int) = sy.toFloat()
private fun toRealY(sx: Int, sy: Int) = (SRC_W - sx).toFloat()

private fun rnd(n: Int) = Random().nextInt(n)

/**
 * 入口：夜世界下兵方案2（源项目函数123a/323a/128a 保真移植）。
 * 先下兵，再调用现有 [releaseSkillsLoop] 释放英雄技能并等待战斗结束。
 * 回营（点退出对战）由 normalBattle 的共享逻辑处理。
 */
suspend fun builderBaseAttackSource(): Boolean {
    ShowMessage.run("夜世界方案2：进入 函数123a 下兵流程")
    if (!deployTroopsSource()) {
        ShowMessage.run("夜世界方案2：下兵失败，终止本场")
        return false
    }
    ShowMessage.run("夜世界方案2：下兵完成，进入 函数128a 技能释放与战斗监控")
    // 函数128a 的战斗监控：技能释放 + 等待放弃按钮出现。
    // 现有 releaseSkillsLoop 已包含"战斗中持续释放英雄技能直到战斗结束"的逻辑，直接复用。
    releaseSkillsLoop()
    ShowMessage.run("夜世界方案2：技能轮询结束，交回 normalBattle 继续回营")
    return true
}

/**
 * 入口：夜世界下兵方案3（卡位式从左到右、点卡位+点落点4次、白框识别判已下、最多6轮、落点无效换点）。
 * 复用方案2的方向/边界扫描/几何；下兵逻辑改为固定卡位轮询。
 */
suspend fun builderBaseAttackSource3(): Boolean {
    ShowMessage.run("夜世界方案3：进入卡位式下兵流程")
    if (!prepareSourceGeometry()) {
        ShowMessage.run("夜世界方案3：几何准备失败，终止本场")
        return false
    }

    source3AreaCallCount++
    val slotXs = if (source3AreaCallCount % 2 == 1) AREA1_SLOT_XS else AREA2_SLOT_XS
    val areaName = if (source3AreaCallCount % 2 == 1) "第一区域(6兵)" else "第二区域(8兵)"
    ShowMessage.run("夜世界方案3：当前为 $areaName，共 ${slotXs.size} 个卡位")

    val slots = slotXs.mapIndexed { index, x ->
        SlotState(index, x, SLOT_CENTER_Y)
    }

    val deployCandidates = buildDeployCandidates()
    ShowMessage.run("夜世界方案3：候选落点 ${deployCandidates.size} 个")

    var round = 0
    while (round < SOURCE3_MAX_ROUNDS) {
        round++
        ShowMessage.run("夜世界方案3：第 ${round}/${SOURCE3_MAX_ROUNDS} 轮下兵开始")

        var anyDeployedThisRound = false
        var allDone = true

        for (slot in slots) {
            if (slot.deployed) continue
            allDone = false

            // 点击卡位并检测是否出现选中白框：无白框=该卡位已下完
            val hasWhiteBorder = tapAndCheckWhiteBorder(slot.x, slot.y)
            if (!hasWhiteBorder) {
                slot.deployed = true
                ShowMessage.run("夜世界方案3：卡位${slot.index}(${slot.x},${slot.y}) 无选中白框，判定已下完")
                continue
            }

            // 有白框 → 选中成功，取该卡位下一个候选落点
            val point = deployCandidates[slot.nextCandidateIndex % deployCandidates.size]
            slot.nextCandidateIndex++
            ShowMessage.run("夜世界方案3：卡位${slot.index} 选中，使用落点 #${slot.nextCandidateIndex} (${point.x},${point.y})")

            // 在该落点连续点击 4 次
            repeat(SOURCE3_TAPS_PER_POINT) {
                TouchActions.tap(point.x, point.y)
                delay(120L)
            }
            anyDeployedThisRound = true
            delay(200L)
        }

        if (allDone) {
            ShowMessage.run("夜世界方案3：全部卡位已下完，结束下兵")
            break
        }
        if (!anyDeployedThisRound) {
            ShowMessage.run("夜世界方案3：本轮无卡位可下（均判定已下完），结束下兵")
            break
        }
        // 给游戏一点动画时间，再进入下一轮
        delay(400L)
    }

    ShowMessage.run("夜世界方案3：下兵完成，进入技能轮询")
    releaseSkillsLoop()
    ShowMessage.run("夜世界方案3：技能轮询结束，交回 normalBattle 继续回营")
    return true
}

/** 方案3 单个卡位的状态。 */
private data class SlotState(
    val index: Int,
    val x: Int,
    val y: Int,
    var deployed: Boolean = false,
    var nextCandidateIndex: Int = 0
)

/**
 * 点击卡位后检测是否出现白色选中边框。
 * 返回 true = 白框出现（卡位仍有兵/英雄，可下）；false = 无白框（已下完或该卡位为空）。
 */
private suspend fun tapAndCheckWhiteBorder(x: Int, y: Int): Boolean {
    TouchActions.tap(x, y)
    delay(250L)  // 等选中动画/白框稳定
    val cap = ScreenCaptureManager.capture(false) as? ScreenCaptureManager.CaptureResult
        ?: return false
    val count = countBrightPixels(
        cap,
        x - WHITE_CHECK_HALF_SIZE, y - WHITE_CHECK_HALF_SIZE,
        x + WHITE_CHECK_HALF_SIZE, y + WHITE_CHECK_HALF_SIZE,
        WHITE_BRIGHT_THRESHOLD
    )
    ShowMessage.run("夜世界方案3：卡位(${x},${y}) 白框亮度像素数=$count")
    return count >= WHITE_BORDER_MIN_COUNT
}

/**
 * 统计横屏 capture 中指定矩形内，R/G/B 均 >= brightThreshold 的像素数量。
 * 坐标直接使用横屏坐标，不做源→横屏旋转。
 */
private fun countBrightPixels(
    cap: ScreenCaptureManager.CaptureResult,
    x1: Int, y1: Int, x2: Int, y2: Int,
    brightThreshold: Int
): Int {
    val w = cap.width
    val h = cap.height
    val buf = cap.buffer
    val stride = cap.rowStride
    val xStart = x1.coerceIn(0, w - 1)
    val xEnd = x2.coerceIn(0, w - 1)
    val yStart = y1.coerceIn(0, h - 1)
    val yEnd = y2.coerceIn(0, h - 1)
    val thr = brightThreshold.coerceIn(0, 255)
    var count = 0
    for (y in yStart..yEnd) {
        for (x in xStart..xEnd) {
            val o = y * stride + x * 4
            val b = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val r = buf.get(o + 2).toInt() and 0xFF
            if (r >= thr && g >= thr && b >= thr) count++
        }
    }
    return count
}

/**
 * 由当前四向几何构建候选落点列表（实屏坐标）：四象限中点 + 女巫外围中点。
 * 每个卡位按顺序循环使用，落点无效时自动换下一个。
 */
private fun buildDeployCandidates(): List<Point> {
    val list = mutableListOf<Point>()
    fun addQuad(q: Quad) {
        list.add(Point(toRealX(q.midX, q.midY).toInt(), toRealY(q.midX, q.midY).toInt()))
    }
    addQuad(ltQuad)
    addQuad(rtQuad)
    addQuad(lbQuad)
    addQuad(rbQuad)
    addQuad(ltOuter)
    addQuad(rtOuter)
    addQuad(lbOuter)
    addQuad(rbOuter)
    return list
}

/**
 * 复用部分：随机选方向 + 动态边界扫描 + 构建四向几何（含夹取）。
 * 方案2（deployTroopsSource）与方案3（builderBaseAttackSource3）共用。
 */
private suspend fun prepareSourceGeometry(): Boolean {
    // 随机选一个方向作为蛮王位置（左上/右上/左下/右下）
    direction = when (Random().nextInt(4)) {
        0 -> "左上"
        1 -> "右上"
        2 -> "左下"
        else -> "右下"
    }
    ShowMessage.run("夜世界源方案：随机蛮王位置 = $direction")

    // 动态边界扫描（第二村庄用硬编码几何），得到走行；具体四向几何 + 方向修正 + 夹取 + 构建 quads 由 applyDirectionGeometry 完成（便于重试时换方向）
    if (!SECOND_VILLAGE) {
        ShowMessage.run("夜世界源方案：开始动态边界扫描，色系=${if (altBoundaryScan) "2973C2" else "4053AE"}")
        if (altBoundaryScan) {
            // 函数132a → 函数129a（左上/右上）→ 函数130a（左下/右下）→ 函数131a
            cameraSwipeDown()
            scan129a()
            scan130a()
            cameraSwipeUp()
        } else {
            // 函数132a → 函数122a（左下/右下）→ 函数131a → 函数121a（左上/右上）
            // 与源 函数123a 实际调用顺序一致
            cameraSwipeDown()
            scanLower()
            cameraSwipeUp()
            scanUpper()
        }
    } else {
        ShowMessage.run("夜世界源方案：第二村庄模式，跳过边界扫描，使用硬编码几何")
    }
    applyDirectionGeometry(direction)
    return true
}

/**
 * 函数123a：英雄下放、逐兵种下兵。此即“夜世界下兵方案2”（源项目保真移植）。
 */
private suspend fun deployTroopsSource(): Boolean {
    if (!prepareSourceGeometry()) return false

    // ───────────────── 第一区域(英雄)与第二区域(兵)合并轮次：识别 → 第1轮顺带下英雄 → 下本轮回溯到的新兵种 ─────────────────
    // 英雄在第 1 轮“识别之后”下放（与源方案一致：识别→下英雄→下兵，英雄与兵间隔最短），
    // 每下完一个英雄立即回扫卡槽判断是否真的下场，失败则换方向重试，并记录其成功落点作保底。
    val troops = listOf(
        TroopSpec("皮卡", BuilderBaseAttackColors.BuilderBasePekka, false, false),
        TroopSpec("巨人", BuilderBaseAttackColors.BuilderBaseGiant, false, false),
        TroopSpec("弓箭", BuilderBaseAttackColors.BuilderBaseArcher, false, false),
        TroopSpec("野蛮", BuilderBaseAttackColors.BuilderBaseBarbarian, false, false),
        TroopSpec("炮车", BuilderBaseAttackColors.BuilderBaseCannonCart, false, false),
        TroopSpec("炸弹", BuilderBaseAttackColors.BuilderBaseBomber, true, false),
        TroopSpec("野猪", BuilderBaseAttackColors.BuilderBaseHog, false, false),
        TroopSpec("气球", BuilderBaseAttackColors.BuilderBaseBalloon, false, false),
        TroopSpec("龙宝", BuilderBaseAttackColors.BuilderBaseBabyDragon, true, false),
        TroopSpec("亡灵", BuilderBaseAttackColors.BuilderBaseMinion, false, false),
        TroopSpec("法师", BuilderBaseAttackColors.BuilderBaseWizard, false, false),
        TroopSpec("女巫", BuilderBaseAttackColors.NightWitch, true, true),
    )

    // 已成功下发过的兵种名集合：女巫等兵种下场后卡槽常不消失（残留），若每轮都重新识别到就会
    // 被误判为“未下成功”而重复下/乱换方向。一旦下发过即视为已下场，后续轮次不再重复下。
    val deployedNames = mutableSetOf<String>()
    var heroFallbackPoint: Point? = null
    var prevNames: List<String>? = null
    val maxRounds = 3
    var round = 0
    while (true) {
        round++
        // 识别兵种（扫描全部卡槽，记录命中的兵种）
        val available = recognizeTroops(troops)
        val names = available.map { it.first.name }.sorted()
        ShowMessage.run("夜世界源方案：第${round}轮 识别兵种完成，命中 ${available.size} 个可下兵种")

        // 本轮新出现且从未下发过的兵种（需要真正去下）
        val newOnes = available.filter { it.first.name !in deployedNames }
        // 识别集合与上一轮完全相同且没有任何新兵种要下 → 这些卡槽是“已下场但残留”，停止重试避免重复下/乱换方向
        if (prevNames != null && names == prevNames && newOnes.isEmpty()) {
            ShowMessage.run("夜世界源方案：第${round}轮 识别结果与上一轮完全相同且均已下场（卡槽残留），不再重复下兵")
            break
        }
        prevNames = names

        // 第一区域：第 1 轮顺带下英雄（识别之后），带成功检测与落点记录
        if (round == 1) {
            heroFallbackPoint = deployHeroSource()
        }

        if (available.isEmpty()) {
            ShowMessage.run("夜世界源方案：无可下兵种，结束下兵流程")
            break
        }

        // 第二区域：只下“本轮新识别且尚未下发过”的兵种（已下发过但卡槽残留的跳过，避免重复下）
        for ((spec, c) in available) {
            if (spec.name in deployedNames) continue
            deployTroop(spec, c)
            deployedNames.add(spec.name)
            delay(150L)
        }

        if (round >= maxRounds) {
            ShowMessage.run("夜世界源方案：达到最大下兵轮次 $maxRounds，结束常规下兵流程")
            break
        }
        // 下一轮重新识别，用于确认卡槽是否真消失（已下发过的不会被重复下）
    }

    // ───────────────── 保底：常规轮次后“从未下发过”的残留兵种，用英雄成功落点兜底 ─────────────────
    // 已下发过但卡槽仍残留的（女巫等已下场但卡槽不消失）视为已下，不再重复下。
    val leftAfterRounds = recognizeTroops(troops)
    val genuineLeft = leftAfterRounds.filter { it.first.name !in deployedNames }
    if (genuineLeft.isEmpty()) {
        ShowMessage.run("夜世界源方案：常规下兵已全部消耗（或已下场卡槽残留），无需保底")
    } else if (heroFallbackPoint != null) {
        ShowMessage.run("夜世界源方案：常规 ${maxRounds} 轮后仍有 ${genuineLeft.size} 个兵种从未下，启用了英雄成功落点保底（复用英雄“点卡槽+单指拖到落点”手势）")
        for ((spec, c) in genuineLeft) {
            dragDeployTroop(spec, c, heroFallbackPoint)
            delay(150L)
        }
        val leftFinal = recognizeTroops(troops).filter { it.first.name !in deployedNames }
        if (leftFinal.isEmpty()) {
            ShowMessage.run("夜世界源方案：保底落点下兵成功，全部兵种已下场")
        } else {
            ShowMessage.run("夜世界源方案：保底落点仍未下成功 ${leftFinal.size} 个兵种（该落点区域被游戏判定不可下）")
        }
    } else {
        ShowMessage.run("夜世界源方案：常规 ${maxRounds} 轮后仍有 ${genuineLeft.size} 个兵种从未下，且无英雄成功落点可保底")
    }

    ShowMessage.run("夜世界源方案：全部兵种识别与下兵流程结束")
    return true
}

/** 由走行（或第二村庄硬编码）计算方向无关的四向基础几何，套用方向修正、夹取到有效屏内，并构建 quads（含女巫外围）。 */
private fun applyDirectionGeometry(dir: String) {
    var ltStartX: Int; var ltStartY: Int; var rtStartX: Int; var rtStartY: Int
    var lbStartX: Int; var lbStartY: Int; var rbStartX: Int; var rbStartY: Int
    var ltEndX: Int; var ltEndY: Int; var rtEndX: Int; var rtEndY: Int
    var lbEndX: Int; var lbEndY: Int; var rbEndX: Int; var rbEndY: Int
    if (SECOND_VILLAGE) {
        // 第二村庄：硬编码几何（源竖屏坐标）
        ltStartX = 446; ltStartY = 535; rtStartX = 453; rtStartY = 723
        lbStartX = 449; lbStartY = 268; rbStartX = 460; rbStartY = 1035
        ltEndX = 244; ltEndY = 266; rtEndX = 232; rtEndY = 1026
        lbEndX = 236; lbEndY = 549; rbEndX = 233; rbEndY = 729
    } else {
        // 用走行重算 开始/结束（源 15886~15901，含 +21 基础偏移）
        ltStartX = 518 - walkRT + 21; ltStartY = (640 - walkRT * XY_RATIO).toInt()
        rtStartX = 518 - walkLT + 21; rtStartY = (640 + walkLT * XY_RATIO).toInt()
        lbStartX = 526 - walkLT + 21; lbStartY = (155 + walkLT * XY_RATIO).toInt()
        rbStartX = 526 - walkRT + 21; rbStartY = (1128 - walkRT * XY_RATIO).toInt()
        ltEndX = 157 + walkLB + 21; ltEndY = (155 + walkLB * XY_RATIO).toInt()
        rtEndX = 157 + walkRB + 21; rtEndY = (1128 - walkRB * XY_RATIO).toInt()
        lbEndX = 165 + walkRB + 21; lbEndY = (640 - walkRB * XY_RATIO).toInt()
        rbEndX = 165 + walkLT + 21; rbEndY = (640 + walkLT * XY_RATIO).toInt()
    }
    // 方向相关 + 走行*XY*0.5 修正（源 15910~15917）
    when (dir) {
        "左上" -> ltStartY += (walkLT * XY_RATIO * 0.5).toInt()
        "右上" -> rtStartY += (walkRT * XY_RATIO * 0.5).toInt()
        "左下" -> lbStartY += (walkLB * XY_RATIO * 0.5).toInt()
        "右下" -> rbStartY += (walkRB * XY_RATIO * 0.5).toInt()
    }
    ltEndY += (walkLT * XY_RATIO * 0.5).toInt()
    rtEndY += (walkRT * XY_RATIO * 0.5).toInt()
    lbEndY += (walkLB * XY_RATIO * 0.5).toInt()
    rbEndY += (walkRB * XY_RATIO * 0.5).toInt()

    // 夹取到有效屏内（源坐标 X∈[0,720]、Y∈[0,1280]；旋转后 横屏X=源Y、横屏Y=720-源X）。
    // 动态边界扫描在个别基地/方向会给出越界值（实测 右上 出现过 开始(47,1296)，旋转后 横屏X=1296 越界），
    // 落点跑到屏幕外 → 双指滑屏无效 → 兵种实际未下场。夹取后保证落点必在屏内（mid 由 start/end 派生，自动同步）。
    fun cx(v: Int) = v.coerceIn(8, 712)
    fun cy(v: Int) = v.coerceIn(8, 1272)
    ltStartX = cx(ltStartX); ltStartY = cy(ltStartY)
    rtStartX = cx(rtStartX); rtStartY = cy(rtStartY)
    lbStartX = cx(lbStartX); lbStartY = cy(lbStartY)
    rbStartX = cx(rbStartX); rbStartY = cy(rbStartY)
    ltEndX = cx(ltEndX); ltEndY = cy(ltEndY)
    rtEndX = cx(rtEndX); rtEndY = cy(rtEndY)
    lbEndX = cx(lbEndX); lbEndY = cy(lbEndY)
    rbEndX = cx(rbEndX); rbEndY = cy(rbEndY)

    ltQuad = Quad(ltStartX, ltStartY, ltEndX, ltEndY)
    rtQuad = Quad(rtStartX, rtStartY, rtEndX, rtEndY)
    lbQuad = Quad(lbStartX, lbStartY, lbEndX, lbEndY)
    rbQuad = Quad(rbStartX, rbStartY, rbEndX, rbEndY)
    // 外围：复制开始/结束（女巫专用，源 15860~15879）
    ltOuter = Quad(ltStartX, ltStartY, ltEndX, ltEndY)
    rtOuter = Quad(rtStartX, rtStartY, rtEndX, rtEndY)
    lbOuter = Quad(lbStartX, lbStartY, lbEndX, lbEndY)
    rbOuter = Quad(rbStartX, rbStartY, rbEndX, rbEndY)
    ShowMessage.run("夜世界源方案几何：方向=$dir 走行LT=$walkLT RT=$walkRT LB=$walkLB RB=$walkRB | 左上($ltStartX,$ltStartY)-($ltEndX,$ltEndY) 右上($rtStartX,$rtStartY)-($rtEndX,$rtEndY) 左下($lbStartX,$lbStartY)-($lbEndX,$lbEndY) 右下($rbStartX,$rbStartY)-($rbEndX,$rbEndY)")
}

/** 在四个方向中随机选一个与 cur 不同的方向，用于重试时更换落点区域。 */
private fun randomDirectionOtherThan(cur: String): String {
    val others = listOf("左上", "右上", "左下", "右下") - cur
    return others[Random().nextInt(others.size)]
}

private data class TroopSpec(
    val name: String,     // 兵种名（仅日志用）
    val schema: ColorSchema,
    val batch: Boolean,   // 是否用 findMultiColorAll（源批量兵种）
    val witch: Boolean    // 是否女巫（用外围点）
)

/** 英雄下场结果：无该英雄 / 下场成功(附带源坐标落点) / 下场失败。 */
private sealed class HeroResult {
    object Absent : HeroResult()                       // 本账号无此英雄
    data class Success(val point: Point) : HeroResult() // point 为英雄成功落点的实屏坐标（heroDeployPoint 已旋转）
    object Fail : HeroResult()                         // 有该英雄但下场失败（卡槽仍在）
}

/** 依次下放两个英雄（王、夜飞机），各自独立重试；任一英雄下场成功即记录其源坐标落点作为兵种保底落点。 */
private suspend fun deployHeroSource(): Point? {
    var fallback: Point? = null
    val heroes = listOf(
        BuilderBaseAttackColors.BuilderBaseMachine to "王",
        BuilderBaseAttackColors.BuilderBaseBattleCopter to "夜飞机"
    )
    for ((schema, label) in heroes) {
        var res = deploySingleHero(schema, label)
        var attempts = 1
        while (res is HeroResult.Fail && attempts < HERO_MAX_ROUNDS) {
            attempts++
            // 换方向 → 重建几何 → 换落点重试
            direction = randomDirectionOtherThan(direction)
            applyDirectionGeometry(direction)
            res = deploySingleHero(schema, label)
        }
        when (res) {
            is HeroResult.Success -> {
                // 优先用王的成功落点（王通常落点更靠中、更通用）；无王则退而用夜飞机
                if (label == "王" || fallback == null) fallback = res.point
                ShowMessage.run("夜世界源方案：英雄[$label] 下场成功，记录成功落点(实屏) @(${res.point.x},${res.point.y})")
            }
            is HeroResult.Fail -> ShowMessage.run("夜世界源方案：英雄[$label] 重试 $HERO_MAX_ROUNDS 轮仍未下场")
            is HeroResult.Absent -> { /* 本账号无此英雄，跳过 */ }
        }
    }
    return fallback
}

/** 下放单个英雄并判断下场是否成功：先点卡槽选中、再点落点才真正下场；下完回扫卡槽，消失即成功。 */
private suspend fun deploySingleHero(schema: ColorSchema, label: String): HeroResult {
    val card = findMultiColors(schema) ?: run {
        ShowMessage.run("夜世界源方案：英雄[$label] 卡槽未识别（本账号无此英雄），跳过")
        return HeroResult.Absent
    }
    ShowMessage.run("夜世界源方案：检测到英雄[$label]卡槽(${card.x},${card.y})，点下放")
    // 第一步：点英雄卡 → 选中（与源 taps(夜世界王X, 夜世界王Y) 一致）
    TouchActions.tap(card.x, card.y)
    delay(300L)
    // 第二步：再点一次落点（当前方向象限中点），否则英雄只被选中、永远不下场。
    // 源 函数123a 在选完卡后紧接着 taps(左上/右上/左下/右下中间X, 中间Y) 才是真正下放，
    // 之前漏掉这一步，导致整局战争机器都留在手里没下出去（实机日志已确认）。
    val (dx, dy) = heroDeployPoint()
    ShowMessage.run("夜世界源方案：英雄[$label]点落点 @(${dx.toInt()},${dy.toInt()})")
    TouchActions.tap(dx.toInt(), dy.toInt())
    delay(900L)  // 等英雄卡消耗 / 下场动画稳定后再回扫判断
    // 判断成功：卡槽消失即视为下场成功
    val stillThere = findMultiColors(schema) != null
    return if (!stillThere) {
        ShowMessage.run("夜世界源方案：英雄[$label] 下场成功")
        // 直接记录英雄真实落点（heroDeployPoint 已是旋转后的实屏坐标），保底时复用同一“点卡槽+点落点”手势
        HeroResult.Success(Point(dx.toInt(), dy.toInt()))
    } else {
        ShowMessage.run("夜世界源方案：英雄[$label] 下场疑似失败（卡槽仍在），将更换落点重试")
        HeroResult.Fail
    }
}

/** 识别全部命中的兵种卡槽（扫描全部卡槽，返回 兵种规格+命中坐标）。 */
private suspend fun recognizeTroops(troops: List<TroopSpec>): List<Pair<TroopSpec, Point>> {
    return troops.mapNotNull { spec ->
        val c = if (spec.batch) findMultiColorsAll(spec.schema).firstOrNull()
                else findMultiColors(spec.schema)
        if (c == null) null else spec to c
    }
}

/** 保底下兵：单指从卡槽拖到英雄成功落点（COC 标准放兵手势，比双指滑屏/双击更稳），在已验证有效的落点处放兵。 */
private suspend fun dragDeployTroop(spec: TroopSpec, card: Point, realPoint: Point) {
    ShowMessage.run("夜世界源方案：保底拖放下兵种[${spec.name}] 卡槽(${card.x},${card.y}) → 落点(${realPoint.x},${realPoint.y})")
    // 点卡选中 → 单指从卡位拖到落点 → 松手即下场
    TouchActions.tap(card.x, card.y)
    delay(300L)
    TouchActions.touchDown(card.x.toFloat(), card.y.toFloat(), 1)
    delay(200L)
    TouchActions.touchMove(realPoint.x.toFloat(), realPoint.y.toFloat(), 1)
    delay(200L)
    TouchActions.touchUp(1)
    delay(400L)
}

/** 当前方向象限中点（已转横屏真实坐标），作为英雄下放落点（与源 函数123a 的 中间X/中间Y 对应）。 */
private fun heroDeployPoint(): Pair<Float, Float> {
    val q = when (direction) {
        "左上" -> ltQuad
        "右上" -> rtQuad
        "左下" -> lbQuad
        else -> rbQuad
    }
    return toRealX(q.midX, q.midY) to toRealY(q.midX, q.midY)
}

/** 检测兵种卡槽并按源 函数323a 双指滑屏下兵。 */
private suspend fun deployTroop(spec: TroopSpec, card: Point? = null) {
    // card 由调用方在“统一识别”阶段预先扫描得到；为空时再现场扫描（兼容旧路径）
    val c = card ?: if (spec.batch) {
        // 源：findMultiColorAll 取 [1].y（Lua 1-based = 第一个匹配），这里取首个命中点
        findMultiColorsAll(spec.schema).firstOrNull()
    } else {
        findMultiColors(spec.schema)
    } ?: run {
        ShowMessage.run("夜世界源方案：兵种[${spec.name}] 卡槽未识别，跳过")
        return
    }
    val quad = when (direction) {
        "左上" -> ltQuad
        "右上" -> rtQuad
        "左下" -> lbQuad
        else -> rbQuad
    }
    val outer = when (direction) {
        "左上" -> ltOuter
        "右上" -> rtOuter
        "左下" -> lbOuter
        else -> rbOuter
    }
    ShowMessage.run("夜世界源方案：部署兵种[${spec.name}] 方向=$direction 批量=${spec.batch} 女巫=${spec.witch} 命中卡槽(${c.x},${c.y})")
    dualFingerDeploy(quad, outer, spec.witch, c)
}

/**
 * 函数323a：双指滑屏放兵。
 * 源逻辑：taps(72, y) 点卡 → 双指按住中间点 → 循环 cishu 次
 * （指1滑到开始点、指2滑到结束点、两指再回到中间点）→ 松手。
 * 坐标均已按 横屏X=源Y、横屏Y=720-源X 旋转。
 */
private suspend fun dualFingerDeploy(quad: Quad, outer: Quad, isWitch: Boolean, card: Point) {
    ShowMessage.run("夜世界源方案：双指滑屏 卡(${card.x},${card.y}) 中(${quad.midX},${quad.midY}) 开始(${quad.startX},${quad.startY}) 结束(${quad.endX},${quad.endY}) 女巫=$isWitch")
    // 点兵种卡（源 taps(72, y)；当前坐标系下直接点匹配到的卡槽点）
    TouchActions.tap(card.x, card.y)
    delay(400L)

    val m = if (isWitch) outer else quad
    val (f1x, f1y) = toRealX(m.midX + 5 + rnd(5), m.midY + 5 + rnd(5)) to
        toRealY(m.midX + 5 + rnd(5), m.midY + 5 + rnd(5))
    val (f2x, f2y) = toRealX(m.midX + rnd(5), m.midY + rnd(5)) to
        toRealY(m.midX + rnd(5), m.midY + rnd(5))
    TouchActions.touchDown(f1x, f1y, 1)
    TouchActions.touchDown(f2x, f2y, 2)
    delay(432L)

    repeat(CISHU) {
        // 女巫（isWitch）使用外围点 outer 作为起始/结束轨迹线，与源 16077~16099 一致
        val (sX, sY) = toRealX(m.startX + rnd(8), m.startY + rnd(8)) to
            toRealY(m.startX + rnd(8), m.startY + rnd(8))
        val (eX, eY) = toRealX(m.endX + rnd(8), m.endY + rnd(8)) to
            toRealY(m.endX + rnd(8), m.endY + rnd(8))
        val (mX, mY) = toRealX(m.midX + rnd(8), m.midY + rnd(8)) to
            toRealY(m.midX + rnd(8), m.midY + rnd(8))
        TouchActions.touchMove(sX, sY, 1, isJitter = false)
        TouchActions.touchMove(eX, eY, 2, isJitter = false)
        TouchActions.touchMove(mX, mY, 1, isJitter = false)
        TouchActions.touchMove(mX, mY, 2, isJitter = false)
        delay((300 + rnd(50)).toLong())
    }
    TouchActions.touchUp(1)
    TouchActions.touchUp(2)
    ShowMessage.run("夜世界源方案：双指滑屏结束（兵种[${if (isWitch) "女巫" else ""}]已投放）")
    delay(400L)
}

/**
 * 通用边界扫描：从起始小窗沿 (stepLen, stepYDir*stepLen*XY) 方向逐次外扩，
 * 统计窗口内蓝色边界像素数（函数129a/130a 还额外探一个偏置窗），命中后由 assign 写回。
 * 单方向超过 maxCoord 步则整体偏移并累加走行；走行超过 680 放弃（重置为 0）。
 */
private suspend fun scanBoundary(
    colorsStr: String,
    startX1: Int, startY1: Int, startX2: Int, startY2: Int,
    stepLen: Int, stepYDir: Int, probeYDir: Int, threshold: Int,
    maxCoord: Int,
    assign: (x: Int, y: Int) -> Unit,
    getWalk: () -> Int, setWalk: (Int) -> Unit
) {
    val cap = ScreenCaptureManager.capture(false) as ScreenCaptureManager.CaptureResult
    val colors = parseBgrToleranceList(colorsStr)
    var coord = 0
    var x1 = startX1; var y1 = startY1; var x2 = startX2; var y2 = startY2
    var hhx1 = 1.0; var hhy1 = XY_RATIO; var hhx2 = 1.0; var hhy2 = XY_RATIO
    var w = getWalk()
    while (true) {
        val n1 = countColorsInRegionSource(cap, x1, y1, x2, y2, colors)
        val n3 = if (probeYDir != 0) countColorsInRegionSource(
            cap,
            x1 - 26, (y1 + probeYDir * 26 * XY_RATIO).toInt(),
            x2 - 26, (y2 + probeYDir * 26 * XY_RATIO).toInt(),
            colors
        ) else 0
        val hit = if (probeYDir == 0) (n1 > threshold) else (n1 + n3 > threshold)
        if (hit) {
            assign(x1 + coord, (y1 + coord * stepYDir * XY_RATIO).toInt())
            return
        }
        coord += stepLen
        x1 -= stepLen; x2 -= stepLen
        y1 += (stepYDir * stepLen * XY_RATIO).toInt()
        y2 += (stepYDir * stepLen * XY_RATIO).toInt()
        if (maxCoord < coord) {
            coord = 0
            x1 = startX1; y1 = startY1; x2 = startX2; y2 = startY2
            hhx1 += stepLen; hhy1 += stepLen * XY_RATIO
            hhx2 += stepLen; hhy2 += stepLen * XY_RATIO
            x1 -= hhx1.toInt(); y1 += hhy1.toInt(); x2 -= hhx2.toInt(); y2 += hhy2.toInt()
            w += stepLen; setWalk(w)
        }
        if (w > 680) {
            setWalk(0)
            ShowMessage.run("夜世界源方案：边界扫描放弃(走行>680)，重置为0")
            return
        }
    }
}

/** 函数121a：扫描左上/右上（4053AE 色系），写 walkLT/walkRT。 */
private suspend fun scanUpper() {
    scanBoundary(BLUE_4053, 463, 548, 479, 564, 3, -1, 0, 3, 190,
        { _, _ -> }, { walkLT }, { walkLT = it })
    scanBoundary(BLUE_4053, 462, 706, 478, 722, 3, 1, 0, 3, 190,
        { _, _ -> }, { walkRT }, { walkRT = it })
    ShowMessage.run("夜世界源方案扫描：左上走行=$walkLT 右上走行=$walkRT")
}

/** 函数122a：扫描左下/右下（4053AE 色系），写 walkLB/walkRB。 */
private suspend fun scanLower() {
    scanBoundary(BLUE_4053, 466, 221, 482, 237, 3, 1, 0, 3, 220,
        { _, _ -> }, { walkLB }, { walkLB = it })
    scanBoundary(BLUE_4053, 463, 1061, 479, 1077, 3, -1, 0, 3, 220,
        { _, _ -> }, { walkRB }, { walkRB = it })
    ShowMessage.run("夜世界源方案扫描：左下走行=$walkLB 右下走行=$walkRB")
}

/** 函数129a：扫描左上/右上（2973C2 色系）。与 scanUpper 同作用，作为备选扫描。 */
private suspend fun scan129a() {
    scanBoundary(BLUE_2973, 572, 521, 588, 537, 2, -1, -1, 1, 200,
        { _, _ -> }, { walkLT }, { walkLT = it })
    scanBoundary(BLUE_2973, 577, 742, 593, 758, 2, 1, 1, 1, 200,
        { _, _ -> }, { walkRT }, { walkRT = it })
    ShowMessage.run("夜世界源方案扫描(2973)：左上走行=$walkLT 右上走行=$walkRT")
}

/** 函数130a：扫描左下/右下（2973C2 色系）。与 scanLower 同作用，作为备选扫描。 */
private suspend fun scan130a() {
    scanBoundary(BLUE_2973, 413, 239, 429, 255, 2, 1, 1, 1, 200,
        { _, _ -> }, { walkLB }, { walkLB = it })
    scanBoundary(BLUE_2973, 419, 1028, 435, 1044, 2, -1, -1, 1, 200,
        { _, _ -> }, { walkRB }, { walkRB = it })
    ShowMessage.run("夜世界源方案扫描(2973)：左下走行=$walkLB 右下走行=$walkRB")
}

/** 函数132a：相机向下滑动（源 swipes(1,360,640,1080,640,300) 旋转到横屏）。 */
private suspend fun cameraSwipeDown() {
    ShowMessage.run("夜世界源方案：相机下滑")
    delay(100L)
    TouchActions.touchDown(640f, 360f, 1)
    delay(50L)
    TouchActions.touchMove(640f, 0f, 1, isJitter = false)
    delay(280L)
    TouchActions.touchUp(1)
    delay(100L)
}

/** 函数131a：相机向上滑动（源 swipes(1,360,640,-360,640,300) 旋转到横屏）。 */
private suspend fun cameraSwipeUp() {
    ShowMessage.run("夜世界源方案：相机上滑")
    delay(100L)
    TouchActions.touchDown(640f, 360f, 1)
    delay(50L)
    TouchActions.touchMove(640f, 720f, 1, isJitter = false)
    delay(280L)
    TouchActions.touchUp(1)
    delay(100L)
}
