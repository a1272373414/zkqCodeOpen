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
 * 入口：先按源 函数123a 下兵，再调用现有 [releaseSkillsLoop] 释放英雄技能并等待战斗结束。
 * 回营（点退出对战）由 normalBattle 的共享逻辑处理。
 */
suspend fun builderBaseAttackSource(): Boolean {
    ShowMessage.run("夜世界源方案：进入 函数123a 下兵流程")
    if (!deployTroopsSource()) {
        ShowMessage.run("夜世界源方案：下兵失败，终止本场")
        return false
    }
    ShowMessage.run("夜世界源方案：下兵完成，进入 函数128a 技能释放与战斗监控")
    // 函数128a 的战斗监控：技能释放 + 等待放弃按钮出现。
    // 现有 releaseSkillsLoop 已包含"战斗中持续释放英雄技能直到战斗结束"的逻辑，直接复用。
    releaseSkillsLoop()
    ShowMessage.run("夜世界源方案：技能轮询结束，交回 normalBattle 继续回营")
    return true
}

/**
 * 函数123a：选方向、算几何（含动态边界扫描）、英雄下放、逐兵种下兵。
 */
private suspend fun deployTroopsSource(): Boolean {
    // 1. 随机选一个方向作为蛮王位置（左上/右上/左下/右下）
    direction = when (Random().nextInt(4)) {
        0 -> "左上"
        1 -> "右上"
        2 -> "左下"
        else -> "右下"
    }
    ShowMessage.run("夜世界源方案：随机蛮王位置 = $direction")

    // 2. 默认四向 开始/结束（源硬编码，竖屏坐标）
    var ltStartX = 446; var ltStartY = 535
    var rtStartX = 453; var rtStartY = 723
    var lbStartX = 449; var lbStartY = 268
    var rbStartX = 460; var rbStartY = 1035
    var ltEndX = 244; var ltEndY = 266
    var rtEndX = 232; var rtEndY = 1026
    var lbEndX = 236; var lbEndY = 549
    var rbEndX = 233; var rbEndY = 729

    if (SECOND_VILLAGE) {
        // 第二村庄：不扫描，直接用上面硬编码几何
        ShowMessage.run("夜世界源方案：第二村庄模式，跳过边界扫描，使用硬编码几何")
    } else {
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
        // 用走行重算 开始/结束（源 15886~15901）
        ltStartX = 518 - walkRT
        ltStartY = (640 - walkRT * XY_RATIO).toInt()
        rtStartX = 518 - walkLT
        rtStartY = (640 + walkLT * XY_RATIO).toInt()
        lbStartX = 526 - walkLT
        lbStartY = (155 + walkLT * XY_RATIO).toInt()
        rbStartX = 526 - walkRT
        rbStartY = (1128 - walkRT * XY_RATIO).toInt()
        ltEndX = 157 + walkLB
        ltEndY = (155 + walkLB * XY_RATIO).toInt()
        rtEndX = 157 + walkRB
        rtEndY = (1128 - walkRB * XY_RATIO).toInt()
        lbEndX = 165 + walkRB
        lbEndY = (640 - walkRB * XY_RATIO).toInt()
        rbEndX = 165 + walkLT
        rbEndY = (640 + walkLT * XY_RATIO).toInt()
        // +21 基础偏移（源 15902~15909）
        ltStartX += 21; rtStartX += 21; lbStartX += 21; rbStartX += 21
        ltEndX += 21; rtEndX += 21; lbEndX += 21; rbEndX += 21
        // 方向相关 + 走行*XY*0.5 修正（源 15910~15917）
        when (direction) {
            "左上" -> ltStartY += (walkLT * XY_RATIO * 0.5).toInt()
            "右上" -> rtStartY += (walkRT * XY_RATIO * 0.5).toInt()
            "左下" -> lbStartY += (walkLB * XY_RATIO * 0.5).toInt()
            "右下" -> rbStartY += (walkRB * XY_RATIO * 0.5).toInt()
        }
        ltEndY += (walkLT * XY_RATIO * 0.5).toInt()
        rtEndY += (walkRT * XY_RATIO * 0.5).toInt()
        lbEndY += (walkLB * XY_RATIO * 0.5).toInt()
        rbEndY += (walkRB * XY_RATIO * 0.5).toInt()
    }

    ShowMessage.run("夜世界源方案几何：走行LT=$walkLT RT=$walkRT LB=$walkLB RB=$walkRB | 左上($ltStartX,$ltStartY)-($ltEndX,$ltEndY) 右上($rtStartX,$rtStartY)-($rtEndX,$rtEndY) 左下($lbStartX,$lbStartY)-($lbEndX,$lbEndY) 右下($rbStartX,$rbStartY)-($rbEndX,$rbEndY)")

    // 3. 由 开始/结束 派生 中间 与 外围（女巫专用，源 15860~15879）
    ltQuad = Quad(ltStartX, ltStartY, ltEndX, ltEndY)
    rtQuad = Quad(rtStartX, rtStartY, rtEndX, rtEndY)
    lbQuad = Quad(lbStartX, lbStartY, lbEndX, lbEndY)
    rbQuad = Quad(rbStartX, rbStartY, rbEndX, rbEndY)
    // 外围：复制开始/结束
    ltOuter = Quad(ltStartX, ltStartY, ltEndX, ltEndY)
    rtOuter = Quad(rtStartX, rtStartY, rtEndX, rtEndY)
    lbOuter = Quad(lbStartX, lbStartY, lbEndX, lbEndY)
    rbOuter = Quad(rbStartX, rbStartY, rbEndX, rbEndY)

    // 4. 英雄下放（函数123a：先检测王/夜飞机卡槽，点下）
    dropHero(BuilderBaseAttackColors.BuilderBaseMachine, "王")
    dropHero(BuilderBaseAttackColors.BuilderBaseBattleCopter, "夜飞机")

    // 5. 兵种识别并逐一下兵（源 15997~16071，顺序与源一致）
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
    for (spec in troops) {
        deployTroop(spec)
        delay(150L)
    }
    ShowMessage.run("夜世界源方案：全部兵种识别与下兵流程结束")
    return true
}

private data class TroopSpec(
    val name: String,     // 兵种名（仅日志用）
    val schema: ColorSchema,
    val batch: Boolean,   // 是否用 findMultiColorAll（源批量兵种）
    val witch: Boolean    // 是否女巫（用外围点）
)

/** 检测并点下英雄卡槽。 */
private suspend fun dropHero(schema: ColorSchema, label: String) {
    val p = findMultiColors(schema) ?: run {
        ShowMessage.run("夜世界源方案：未检测到英雄[$label]卡槽")
        return
    }
    ShowMessage.run("夜世界源方案：检测到英雄[$label]卡槽(${p.x},${p.y})，点下放")
    TouchActions.tap(p.x, p.y)
    delay(300L)
}

/** 检测兵种卡槽并按源 函数323a 下兵。 */
private suspend fun deployTroop(spec: TroopSpec) {
    val card = if (spec.batch) {
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
    ShowMessage.run("夜世界源方案：部署兵种[${spec.name}] 方向=$direction 批量=${spec.batch} 女巫=${spec.witch} 命中卡槽(${card.x},${card.y})")
    dualFingerDeploy(quad, outer, spec.witch, card)
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
