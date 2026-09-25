package com.coc.zkqcode.jar.code.builderbase.attack

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchIn
import com.coc.zkqcode.core.util.touchactions.TouchActions.swipe
import com.coc.zkqcode.jar.code.builderbase.resources.collectBuilderBaseResources
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.buildings.BaseType
import com.coc.zkqcode.jar.code.universal.buildings.walls.calculateResourcesPercentage
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.clickRightBottom
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.deploy.DeployGeometry
import com.coc.zkqcode.jar.code.universal.deploy.DeploySide
import com.coc.zkqcode.jar.code.universal.deploy.DeployType
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.checkMemoryFile
import com.coc.zkqcode.jar.code.universal.smalltools.checkReconnections
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.getConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.jar.code.universal.smalltools.enterBuilderBase
import com.coc.zkqcode.jar.ui.schema.Schema
import kotlin.random.Random

// Shared helper to prepend account number to all log messages
private fun accountLog(msg: String) = ShowMessage.run("账号${InGamesVars.currentAccountNumber}，$msg")

/** 单轮下兵上限（个）：旧逻辑"每个兵种各连点 160 次"会把整局拖得很长。 */
private const val MAX_TROOPS_PER_ROUND = 30

/**
 * 一轮里连点多少个落点。
 *
 * 一张女巫卡就有 20 个兵，如果一轮只点一个落点，整局会被拖得极长
 * （实测每轮约 1.25 秒：日志 03:26:15.660 / 17.015 / 18.367 / 19.499）。
 * 选中一次之后连续点落点即可连续下兵，所以这里成批点。
 * 至于"选中状态是否会在下兵后保持"，由下一轮的白框复查判断：
 *   · 白框消失（选中被清空）→ 下一轮重新点卡；
 *   · 白框仍在（选中保持）  → 下一轮直接继续连点落点，不再点卡（避免"点第二次=放技能"）。
 */
private const val DEPLOY_TAPS_PER_ROUND = 8

/**
 * 探测兵卡是否处于"选中"状态：**选中卡的左缘有一条贯穿整卡高度的纯白竖线**，未选中卡同一位置
 * 是纯黑/暗色 —— 黑白对比，比"白边/紫边"那种模糊判据干净得多。
 *
 * 由差分法实测（用户标注图 夜世界-已死亡-已放技能-未放技能-选中-未选中.jpg 里，
 * 卡4=选中 与 卡5=未选中 是同款女巫卡、兵数和技能状态一致、卡面完全相同，直接相减后）：
 *   选中卡该列 = (255,255,255) 贯穿 y≈606~683；未选中卡对应列 = (0,0,0)。
 * 之前识别不出来，是因为**凭缩放截图估算边框坐标**，位置取偏了，正好取到编号块与技能条上。
 *
 * 这里在匹配点左侧一小段范围内找"近白像素最多的一列"，返回标定信息 ——
 * 先用实机日志把"白线相对匹配点的偏移"量准，再据此下判断（临时诊断，标定完可删）。
 */
/** 兵卡卡位中心：夜世界最多 8 个卡位（打第二区域会多 2 个），间距约 101.6，首位中心约 222。 */
private const val SLOT_FIRST_CENTER = 222f
private const val SLOT_SPACING = 101.6f

/** 把识别到的卡坐标归到固定卡位（识别坐标会抖几十像素，归位后再用固定白框坐标判定）。 */
private fun slotCenterX(cardX: Int): Int =
    Math.round(SLOT_FIRST_CENTER + SLOT_SPACING * Math.round((cardX - SLOT_FIRST_CENTER) / SLOT_SPACING))

/** 把任意横坐标换算成卡位号（1~8，落在卡栏外返回 0）。 */
private fun slotIndex(x: Int): Int {
    val i = Math.round((x - SLOT_FIRST_CENTER) / SLOT_SPACING) + 1
    return if (i in 1..8) i else 0
}

/**
 * 本局各卡位被点击的次数（1~8，索引 0 不用）。
 *
 * 用户实测现象："每次都是第三张兵卡的位置点了两次"。兵卡槽被点击的来源有三处：
 *   ① [deployTroops] 里的下兵选卡；
 *   ② 部队技能（也是点在兵卡槽上）；
 *   ③ 英雄技能（点在英雄卡槽上）。
 * 把它统一记到卡位号上，就能一眼看出"哪个卡位被点了几次、分别是谁点的"。
 */
private val slotTapCount = IntArray(9)

/** 技能轮询的最长时长（毫秒）：兜底，避免战斗异常时卡在这里不出。 */
/**
 * 技能轮询的单次时长上限。
 *
 * 原来设 120s，实机表现是**每次下兵后都死守 120 秒**才回到主循环，
 * 于是"进第二区域""继续下兵"都要干等两分钟（日志 05:40:34 进 → 05:42:34 才退）。
 * 英雄充能约 15 秒 1 格，守 30 秒足够覆盖 1~2 次释放；到点就交回主循环，
 * 主循环下一轮又会触发 normalBattle → 下兵 → 再进技能轮询，节奏更合理。
 */
private const val SKILL_LOOP_MAX_MS = 30_000L

/**
 * 技能轮询 —— **只管放技能**：英雄技能 + 各兵种部队技能。
 *
 * 由用户要求与下兵轮询拆开：
 *   · 下兵轮询（[deployTroops]）只管下兵，**绝不碰技能**；
 *   · 下兵完成之后才进入本函数，专门放技能。
 * 这样技能点击永远不会和下兵选卡交错，也就不会再出现"同一张卡被点两次 → 放技能 →
 * 选卡丢失 → 下兵失败"。
 *
 * 退出条件：检测到回营按钮（战斗结束）或超过 [SKILL_LOOP_MAX_MS]。
 * **不是"几轮没技能就退出"**：英雄技能会持续充能（约 15 秒 1 格），要一直守着放。
 */
suspend fun releaseSkillsLoop() {
    val start = System.currentTimeMillis()
    var released = 0
    while (System.currentTimeMillis() - start < SKILL_LOOP_MAX_MS) {
        val screen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult

        // 战斗结束（回营按钮出现）→ 交回主循环处理
        if (findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBackToCamp) != null) {
            accountLog("技能轮询：检测到回营按钮，战斗结束（本局共放出 $released 次技能）")
            return
        }

        // 英雄技能：充能条第 1 格亮起就点英雄卡槽。放掉后充能归零，天然起到节流作用，
        // 所以不做"只放一次"的去重 —— 后续充能满了要继续放。
        if (findMultiColors(byteBuffer = screen, schema = MyColors.HeroChargeReady) != null) {
            val slot = findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseMachine) ?: machineSlot
            val tx = slot?.x ?: 106
            val ty = slot?.y ?: 634
            logSlotTap("英雄技能", tx, ty)
            TouchActions.tap(tx, ty, delayTime = 200)
            released++
        }

        // 部队技能：逐卡位检测，每个卡位只放一次（单个兵种的技能没有冷却，放一次即可）
        val skillSlot = findTroopSkillSlot(screen)
        if (skillSlot > 0 && troopSkillTappedSlot.add(skillSlot)) {
            val center = (SLOT_FIRST_CENTER + SLOT_SPACING * (skillSlot - 1)).toInt()
            logSlotTap("部队技能", center, 620)
            TouchActions.tap(center, 620, delayTime = 150)
            released++
        }

        delayWithMultiplier(250)
    }
    accountLog("技能轮询：达到时长上限（${SKILL_LOOP_MAX_MS / 1000}s），退出（本局共放出 $released 次技能）")
}

/** 记录一次"点卡槽"，带上卡位号与本局累计次数。 */
private fun logSlotTap(what: String, x: Int, y: Int) {
    val idx = slotIndex(x)
    if (idx in 1..8) slotTapCount[idx]++
    val prefix = if (idx in 1..8) "点卡诊断：$what @($x,$y) → 卡位#$idx 本局第 ${slotTapCount[idx]} 次"
    else "点卡诊断：$what @($x,$y) → 卡栏外"
    accountLog(prefix)
}

/**
 * 判定该卡是否处于"选中"状态：**选中卡的左缘有一条贯穿整卡高度的纯白竖线**。
 *
 * 由用户标注图（夜世界-已死亡-已放技能-未放技能-选中-未选中.jpg）逐列统计纯白像素得出：
 *   选中卡（卡位中心 527）：左竖线 x≈497、右竖线 x≈592（宽 95），线上纯白像素 112~118；
 *   未选中卡：同一列只有 9~24 个白像素（都是数字/图标的白）。
 * 所以判据 = 该卡位左竖线附近任一列的白像素数 ≥ 60。
 *
 * 用户确认：**卡位固定、选中框位置也固定**，因此这里按固定卡位坐标取窗，不用动态搜索。
 */
private fun isCardSelected(screen: ScreenCaptureManager.CaptureResult?, cardX: Int): Pair<Boolean, Int> {
    val s = screen ?: return false to 0
    val center = slotCenterX(cardX)
    val leftN = whiteLineCount(s, center - 27)   // 实测：中心 527 → 左竖线 ≈500
    val rightN = whiteLineCount(s, center + 66)  // 实测：中心 527 → 右竖线 ≈592
    // **左右竖线必须同时存在**：相邻卡位的两条线只隔 6px（卡位 N 的左线 vs 卡位 N-1 的右线），
    // 只看一条会把隔壁卡位的边框算成自己的（实机日志已复现：连续 4 轮在不同卡位都报 112）。
    return (leftN >= 60 && rightN >= 60) to minOf(leftN, rightN)
}

/** 统计 x0 附近（±3 列）某一列上贯穿卡片高度的纯白像素数，取最大值。 */
private fun whiteLineCount(s: ScreenCaptureManager.CaptureResult, x0: Int): Int {
    val buf = s.buffer
    var best = 0
    for (x in (x0 - 3)..(x0 + 3)) {
        if (x < 0 || x >= 1280) continue
        var n = 0
        for (y in 582..693) {
            val o = y * s.rowStride + x * s.pixelStride
            val b = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val r = buf.get(o + 2).toInt() and 0xFF
            if (minOf(b, g, r) > 225) n++
        }
        if (n > best) best = n
    }
    return best
}

// ---------- 兵卡"是否还有兵可下"判据：顶部紫色技能条的位置 ----------
// 用户实机确认：**已下兵与未下兵，卡顶部那条紫色技能条的位置（高度）不一样**。
// 由 I:\coc\游戏截图\夜世界-英雄充能 全目录 10 张截图逐行统计紫色像素标定：
//   · 兵还没下 → 技能条在较低位置，顶部 y ≈ **588**（选中时略上移到 582）；
//   · 兵已下场 → 技能条整体上移约 20px，顶部 y ≈ **568**；
//   · 技能已放 / 已阵亡 → 整条消失（无紫色条）。
// 所以判据 = 找到紫色条的顶部 y：无条 或 顶部 y < [SKILL_BAR_TOP_DEPLOYED] → 已下放（不点）；
// 否则（顶部 y ≥ 578）→ 还没下，可以点。
// 跨图一致性验证：「请选择其他兵种」6 个卡位全部 588（未下）；
// 「已经派出所有兵力」6 个卡位全部 568（已下完）；进度 0~3 全部 568。
// 颜色判据（品红/紫粉，样本 EE5AFF / C534FD / C73CFE / C946FF）：B>200 且 R>150 且 G<150。
// 注意：这个判据对卡位坐标的偏差不敏感（条横跨整卡宽 ~90px，窗口取 ±45 偏移二三十像素仍能命中），
// 所以不需要像白框判据那样依赖精确的卡位中心。
private const val SKILL_BAR_Y1 = 556
private const val SKILL_BAR_Y2 = 620
/** 紫色条顶部 y 小于该值 = 兵已下场（实测已下场 568、未下 582~588）。 */
private const val SKILL_BAR_TOP_DEPLOYED = 578
/** 一行的紫色像素数达到该值才认为"条在这一行"（滤掉零星噪点）。 */
private const val SKILL_BAR_ROW_MIN_PIXELS = 3

/**
 * 测量该卡顶部紫色技能条的**顶部 y**（自上而下扫，第一行紫色像素即条顶）。
 *
 * @return 顶部 y；**-1 = 没有紫色条**（技能已放或已阵亡）。
 * 拆出这个函数是为了把实测值打进日志（实机与离线截图有偏差时能一眼看出）。
 */
private fun cardSkillBarTop(screen: ScreenCaptureManager.CaptureResult?, cardX: Int): Int {
    val s = screen ?: return -1
    val buf = s.buffer
    val cx = slotCenterX(cardX)
    for (y in SKILL_BAR_Y1..SKILL_BAR_Y2) {
        var n = 0
        var x = cx - 45
        while (x <= cx + 45) {
            if (x >= 0 && x < 1280) {
                val o = y * s.rowStride + x * s.pixelStride
                val b = buf.get(o).toInt() and 0xFF
                val g = buf.get(o + 1).toInt() and 0xFF
                val r = buf.get(o + 2).toInt() and 0xFF
                if (b > 200 && r > 150 && g < 150) n++
            }
            x += 2
        }
        if (n >= SKILL_BAR_ROW_MIN_PIXELS) return y
    }
    return -1
}

/**
 * 该卡是否还有兵可下（紫色技能条处于"未下"的位置）。
 *
 * @param cardX 兵卡特征匹配到的横坐标，内部归位到卡位中心后按固定窗口取样。
 * @return true 表示可以点这张卡；拿不到截图时返回 true（宁可多试一次）。
 */
private fun isCardDeployable(screen: ScreenCaptureManager.CaptureResult?, cardX: Int): Boolean {
    if (screen == null) return true
    val top = cardSkillBarTop(screen, cardX)
    return top in SKILL_BAR_TOP_DEPLOYED..SKILL_BAR_Y2
}

/** 夜世界可下兵的兵种卡（顺序即优先级：英雄与夜飞机在前，其余按识别顺序）。 */
private val TROOP_CARDS = listOf(
    MyColors.BuilderBaseMachine, MyColors.BuilderBaseBattleCopter,
    MyColors.NightWitch, MyColors.BuilderBaseBarbarian,
    MyColors.BuilderBasePekka, MyColors.BuilderBaseGiant, MyColors.BuilderBaseGiantAlt,
    MyColors.BuilderBaseArcher, MyColors.BuilderBaseCannonCart, MyColors.BuilderBaseBomber,
    MyColors.BuilderBaseHog, MyColors.BuilderBaseBalloon, MyColors.BuilderBaseBabyDragon,
    MyColors.BuilderBaseBabyDragonAlt, MyColors.BuilderBaseMinion, MyColors.BuilderBaseWizard
)

/**
 * 检测屏幕中央偏上的下兵提示红字。**这块红字有两种语义，位置和颜色完全一样**：
 *   · 「请选择其他兵种」    —— 当前兵种已放完，**该换兵**；
 *   · 「已经派出所有兵力」  —— 全部放完，**该收工**。
 *
 * 由用户提供的实机截图 (I:\coc\游戏截图\夜世界-英雄充能\夜世界-请选择其他兵种.jpg) 逐像素采样：
 * 红字集中在 y 195~215，仅原窗口 (562-724, 197-215) 内就有 798 个红色像素，远超原阈值 90。
 *
 * 旧实现直接把它当成「已经派出所有兵力」→ 提前收工，于是**第一个兵种放完就再也不下兵**
 * （实机表现：只下了英雄 / 第一个兵就没动静）。因此这里只做"有没有下兵提示"的判断，
 * 一律按**换兵**处理；换兵之后若确实已无兵可下，`card == null` 分支会自然收工。
 */
/**
 * 统计屏幕中央"下兵提示红字"的红像素数。
 *
 * 返回**真实数量**而不是布尔值：实机日志里提示一次都没触发过，需要靠这个数判断到底是
 * "提示根本没出现"还是"出现了但阈值卡住"（实测「请选择其他兵种」825、「已经派出所有兵力」819）。
 * 窗口按实测红字覆盖区放宽到 (430~920, 175~235)。
 */
private fun deployHintRedCount(screen: ScreenCaptureManager.CaptureResult?): Int {
    val s = screen ?: return 0
    val buf = s.buffer
    var red = 0
    for (y in 175..235) {
        for (x in 430..920) {
            val o = y * s.rowStride + x * s.pixelStride
            val b = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val r = buf.get(o + 2).toInt() and 0xFF
            if (r > 170 && g < 95 && b < 95) red++
        }
    }
    return red
}

/** 是否出现下兵提示红字（阈值 60：实测提示有 800+ 个红像素，战场零散红字远低于此）。 */
private fun hasDeployHint(screen: ScreenCaptureManager.CaptureResult?): Boolean =
    deployHintRedCount(screen) >= 60

/**
 * 区分「已经派出所有兵力」与「请选择其他兵种」。
 *
 * 两条提示的**主红字完全重合**（都在 y195~215），像素数也几乎相同——
 * 由用户截图实测：请选择其他兵种 825 个、已经派出所有兵力 819 个，靠主区域根本分不开。
 * 差异在**上方多出的一行**：「已经派出所有兵力」在 y145~160 还有一行红字（实测约 65 个红像素），
 * 而「请选择其他兵种」在 y140~162 基本没有（约 3 个）。
 *
 * 所以判据 = 主提示命中 **且** 上方那一行也命中。
 */
private fun isAllTroopsDeployedHint(screen: ScreenCaptureManager.CaptureResult?): Boolean {
    val s = screen ?: return false
    if (!hasDeployHint(s)) return false
    val buf = s.buffer
    var red = 0
    for (y in 140..162) {
        for (x in 560..1060) {
            val o = y * s.rowStride + x * s.pixelStride
            val b = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val r = buf.get(o + 2).toInt() and 0xFF
            if (r > 170 && g < 95 && b < 95) red++
        }
    }
    return red >= 30
}

// T16：记录夜飞机(空中机器)卡槽位置，供 realAttack 循环释放技能（每局 normalBattle 重置）
private var battleCopterSlot: Point? = null

// T16：记录战争机器(夜世界王)卡槽位置。英雄技能充能就绪时点的是"英雄卡槽"本身，
// 英雄放下后卡槽会变灰导致色特征匹配不到，所以必须记住坐标（找不到特征时用它兜底）。
private var machineSlot: Point? = null

/**
 * 已释放过技能的**卡位号**（每局清空）。
 *
 * 用户确认：**单个兵种的技能只能释放一次**（不存在冷却），放过一次后再点没有意义。
 * 用卡位号（1~8）而不是横坐标去重：识别坐标会抖，且同一个卡位会被反复匹配到。
 */
private val troopSkillTappedSlot = mutableSetOf<Int>()

/**
 * 逐卡位检测"部队技能就绪"，返回命中的卡位号（1~8），都没命中返回 0。
 *
 * 为什么必须逐卡位：TroopSkills 的搜索区横跨整个卡栏（x 189~1241），
 * findMultiColors 只返回第一个匹配位置，实机恒为 x≈425（卡位3），
 * 于是整局只能放出卡位3 的技能（用户实测"仅第三个兵释放技能"）。
 * 这里把特征 rescope 到每个卡位上方的小区域，逐个判。
 */
private suspend fun findTroopSkillSlot(screen: ScreenCaptureManager.CaptureResult?): Int {
    if (screen == null) return 0
    for (i in 0..7) {
        val center = (SLOT_FIRST_CENTER + SLOT_SPACING * i).toInt()
        val hit = findMultiColors(
            byteBuffer = screen,
            schema = ColorSchema.rescope(MyColors.TroopSkills, center - 42, 566, center + 42, 600, 0)
        )
        if (hit != null) return i + 1
    }
    return 0
}



/**
 * 本局"已经下完"的兵卡序号（挑卡时直接跳过，只有还有兵可下的卡才允许被点击）。
 *
 * 用户指出的机制：某个兵种放完后游戏会弹「请选择其他兵种」。而**放空的卡位色特征仍然匹配得到**
 * （实机日志里英雄卡下完后 `#0` 依旧反复出现在卡扫描结果里），若只看"特征是否匹配"，就会不断
 * 去点一张已经没有兵的卡，那一次落点必然白费。所以见到下兵提示就把"上一次点的那张卡"记进来。
 */
private val exhaustedCards = mutableSetOf<Int>()

/**
 * 本局"已经下完"的**卡位号**（1~8），挑卡时直接跳过。
 *
 * 必须按卡位去重、不能按兵种序号：[TROOP_CARDS] 的下标是**兵种**序号，而同一种兵会占多个
 * 卡位（实机一局有 6 张女巫卡）。若按兵种序号记录，第一张女巫下完就会把其余 5 张一起跳过，
 * 兵根本下不完。英雄卡 x<180 不在卡位网格内（[slotIndex] 返回 0），仍由 [exhaustedCards] 处理。
 */
private val exhaustedSlots = mutableSetOf<Int>()

/**
 * 已点过一次选卡的**非网格卡**（卡栏最左、x < 180：英雄 / 夜飞机），按兵种序号记录。
 *
 * 这些卡不在 8 个兵卡卡位网格内，白框判定对它们无效，所以只能靠"点过一次"来避免重复点 ——
 * 而**点同一张卡第二次 = 放技能**，会白白把技能交掉（实机日志已确认）。
 *
 * 必须**逐兵种**记录而不是用一个布尔：英雄(idx 0)和夜飞机(idx 1)都在 x<180，
 * 之前共用一个 heroCardTapped，第一区域点了英雄后，第二区域扫到夜飞机也被当成
 * "英雄已点过"而只点落点、不点卡，夜飞机整局下不去（2026-09-24 实机）。
 */
private val offGridTapped = mutableSetOf<Int>()

/**
 * 本局战斗开始时间（[realAttack] 入口记录）。
 * 用于给"是否允许释放技能"加超时兜底：正常情况下必须等首次下兵**真正完成**才放技能，
 * 但若落点全部无效、始终下不出兵，不能让技能永远不释放。
 */
private var battleStartedAt = 0L

/**
 * 主用下兵点：源四象限部署线各取样 6 个点（[DeployGeometry.spreadTap]），保持源顺序、不做排序。
 * 常规布局（基地尺寸正常）下这套点可用，是历史验证过的主力方案，所以优先用它。
 */
private fun buildDeployPoints(): List<Point> {
    val points = ArrayList<Point>()
    for (side in DeployGeometry.topSides + DeployGeometry.bottomSides) {
        for (i in 0 until 6) {
            points.add(DeployGeometry.spreadTap(side, i, 6, DeployType.TROOP))
        }
    }
    return points
}

/**
 * 保底下兵点：仅当"主用点整轮都没把兵下出去"时才启用。
 *
 * 触发场景（实机截图已确认，属少数情况）：对方基地建筑群铺满画面中央、四周全是森林悬崖，
 * 部署线整段压在基地里，只有最外围一圈还能下兵。这里取靠画面边缘的一圈网格，
 * 并按"离基地中心越远越先试"排序。
 */
private fun buildFallbackDeployPoints(): List<Point> {
    val cx = DeployGeometry.SCREEN_W / 2.0
    val cy = DeployGeometry.SCREEN_H / 2.0
    // 贴着画面四边铺一条密集环带 —— 用户确认这种"基地铺满画面"的布局**只有最边缘能下兵**，
    // 稀疏网格（上一版 9×5）会大面积落在基地建筑区里，实机表现为 30 次尝试一个兵都下不去。
    // 只剔除会误触界面的热区：
    //   · 左侧「结束战斗」按钮 (36~136, 474~521) —— 点到会直接结束对局
    //   · 底部兵卡栏（y 超过 560 就是卡槽区）
    val ring = ArrayList<Point>()
    for (x in 55..1225 step 60) {
        ring.add(Point(x, 55))
        ring.add(Point(x, 560))
    }
    // 纵向从 115 起，避开与上面横向边重复的两个角点 (55,55)/(1225,55)
    for (y in 115..560 step 60) {
        ring.add(Point(55, y))
        ring.add(Point(1225, y))
    }
    val points = ring.filterNot { it.x < 160 && it.y in 470..525 }
    return points.sortedByDescending {
        val dx = it.x - cx
        val dy = it.y - cy
        dx * dx + dy * dy
    }
}

// Elevated from local nested function to private top-level for reusability
private suspend fun tapRepeat(x: Int, y: Int, times: Int = 12) {
    repeat(times) {
        TouchActions.tap(x, y, delayTime = 80)
    }
}

suspend fun builderBaseAttack(): Boolean {
    accountLog("开始夜世界对战流程")
    // 1. Check if Builder Base farming is enabled
    val isEnabled = getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.BUILDER_BASE_FARMING.key)
    if (!isEnabled) {
        accountLog("未开启打夜世界")
        return true
    }

    // 2. Calculate resource percentages using shared utility
    val resourcePercentage = calculateResourcesPercentage(BaseType.Builder)
    accountLog("金币百分比: ${resourcePercentage.gold}%, 圣水百分比: ${resourcePercentage.elixir}%")
    // Consider resource full if percentage >= 96%
    val isGoldFull = resourcePercentage.gold >= 96
    val isExileFull = resourcePercentage.elixir >= 96
    val stopIfFull = getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.STOP_WHEN_RESOURCE_FULL.key)

    // 3. Determine action based on resource state and settings
    if (isGoldFull && isExileFull && stopIfFull) {
        accountLog("资源已满，停止对战")
    } else {
        // Evaluate attack strategy
        val attackType = when {
            getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.TROPHY_PUSHING_MODE.key) -> {
                accountLog("已勾选上分模式")
                "gold"
            }

            getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.ELIXIR_CART_FARMING.key) -> {
                accountLog("已勾选刷圣水车模式")
                "exile"
            }
            // If gold is not full (< 96%), prioritize gold; otherwise, default to exile
            resourcePercentage.gold < 96 -> "gold"
            else -> "exile"
        }
        val battleTimes =
            getConfigRuntime(Schema.BUILDER_BASE_SETTINGS.SWITCH_ACCOUNT_AFTER_BATTLES.key).toIntOrNull() ?: logAndRestart("${Schema.BUILDER_BASE_SETTINGS.SWITCH_ACCOUNT_AFTER_BATTLES.displayName} 必须是数字，请检查配置")
        repeat(battleTimes) { index ->
            if (!realAttack(attackType, index + 1, battleTimes)) return false
            if ((index + 1) % 5 == 0) {
                if (!enterMainScreen()) return false
                collectBuilderBaseResources()
            }
        }
    }
    // 4. Return to main screen
    return enterMainScreen()
}

private suspend fun realAttack(mode: String, battleNumber: Int = 1, battleTimes: Int): Boolean {
    val startTime = System.currentTimeMillis()
    // Track first detection of SwitchTroopButton for shorter initial delay
    var isFirstSwitchTroop = true
    // 每局重置卡槽记忆：防守局不经过 normalBattle，若不重置会沿用上一局的卡槽坐标，
    // 在"本来就没有充能条"的防守画面上误做诊断（实机已复现该误报）。
    machineSlot = null
    battleCopterSlot = null
    troopSkillTappedSlot.clear()
    offGridTapped.clear()
    exhaustedCards.clear()
    exhaustedSlots.clear()
    battleStartedAt = System.currentTimeMillis()
    slotTapCount.fill(0)
    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 8 * 60 * 1000L) {
            accountLog("战斗超过8分钟，强制退出")
            break
        }
        val remainingMin = (8 * 60 * 1000L - elapsed) / 60000.0
        accountLog("对战中，第${battleNumber}/${battleTimes}局\n若${"%.1f".format(remainingMin)}分钟内未完成对战，则强制重启")

        // Capture a single screenshot and reuse it for all state checks in this iteration
        val capturedScreen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult

        // 技能释放已拆到独立轮询 [releaseSkillsLoop]（由 normalBattle 在下兵完成后调用）——
        // 主循环这里**不再碰技能**，避免技能点击与下兵选卡交错。
        // 主循环只负责界面流转：开始进攻弹窗、搜索对手、训练提示、回营按钮等。

        val trainTroopButton = findMultiColors(byteBuffer = capturedScreen, schema = MyColors.TrainTroops)
        if (trainTroopButton != null) {
            TouchActions.tap(85, 640, delayTime = 800)
            continue // State matched, skip remaining checks
        }
        // 夜世界「开始进攻」确认弹窗优先处理：该界面静止等待玩家点「立即寻找！」，若先走
        // checkReconnections() 会先绕一圈通用检测，界面出现后不能第一时间被点掉（实机看到的就是
        // "十几秒才点、甚至被判成网络卡死重启"）。
        val attackNow = findMultiColors(byteBuffer = capturedScreen, schema = MyColors.AttackNow)
        if (attackNow != null) {
            TouchActions.tap(attackNow.x, attackNow.y, delayTime = 200)
            val warning = findMultiColorsUntil(schemas = listOf(MyColors.TrainTroopsWarning), duration = 500)
            if (warning != null) {
                clickRightBottom(2)
                builderBaseTrainTroops()
            }
            continue
        }
        if (!checkReconnections()) return false
        val builderBaseStarBonus = findMultiColors(byteBuffer = capturedScreen, schema = MyColors.BuilderBaseStarBonus)
        if (builderBaseStarBonus != null) {
            TouchActions.tap(builderBaseStarBonus.x + 10, builderBaseStarBonus.y + 10, delayTime = 200)
            continue
        }
        val search = findMultiColors(byteBuffer = capturedScreen, schema = MyColors.CancelAttackSearch)
        if (search != null) {
            waitLoop()
            continue
        }
        val builderBaseEndBattle = findMultiColors(byteBuffer = capturedScreen, schema = MyColors.BuilderBackToCamp)
        if (builderBaseEndBattle != null) {
            TouchActions.tap(builderBaseEndBattle.x, builderBaseEndBattle.y, delayTime = 200)
            break
        }
        val switchTroopButton = findMultiColorsUntil(schemas = listOf(MyColors.TroopsWithSkills, MyColors.TroopsWithOutSkills), duration = 200, byteBuffer = capturedScreen)
        if (switchTroopButton != null) {
            // Use shorter delay on first detection, normal delay afterward
            if (isFirstSwitchTroop) {
                delayWithMultiplier(500)
                isFirstSwitchTroop = false
            } else {
                accountLog("已进入第二区域")
                delayWithMultiplier(2000)
            }
            // 每次触发都独立走一轮下兵：下兵次数按"区域"各自计算，不共用全局轮数上限。
            // （之前用全局上限时，第 N 次之后的区域会被整体拦掉，实机表现为"进了第二区域却不
            //   下兵"。单轮上限由 deployTroops 自己控制，不会无节制点击。）
            when (mode) {
                "gold" -> normalBattle()
                "exile" -> deployAndExit()
            }
            continue
        }

        // T16：夜飞机（空中机器）技能自动释放（源 战斗监控 16652~16670）。
        // 夜飞机技能就绪时卡槽旁出现粉光(FFB2FF/FE3AC7)，按卡槽位置 rescope 检测后点槽释放。
        if (battleCopterSlot != null) {
            val slot = battleCopterSlot!!
            val copterSkill = findMultiColors(
                byteBuffer = capturedScreen,
                schema = ColorSchema.rescope(MyColors.BattleCopterSkills, slot.x - 70, 555, slot.x + 40, 575, 0)
            ) ?: findMultiColors(
                byteBuffer = capturedScreen,
                schema = ColorSchema.rescope(MyColors.BattleCopterSkillsAlt, slot.x - 70, 555, slot.x + 40, 575, 0)
            )
            if (copterSkill != null) {
                TouchActions.tap(slot.x, slot.y, delayTime = 200)
            }
        }
        delayWithMultiplier(1000)
    }
    if (!enterMainScreen()) return false
    return enterBuilderBase(false)
}

private suspend fun deployAndExit() {
    normalBattle(false)
    val exitButton = findMultiColorsUntil(schemas = listOf(MyColors.ExitBattleButton), duration = 2000)
    if (exitButton != null) {
        TouchActions.tap(exitButton.x, exitButton.y, delayTime = 200)
        TouchActions.tap(775, 470, delayTime = 400)//Confirm exit
    }
}

private suspend fun normalBattle(isNormal: Boolean = true) {
    pinchIn(141, 423, 1052, 352, 638, 365, duration = 200)

    // 夜世界下兵方案提前读取：方案3 要求“不拖动地图、缩到最小”的固定镜头
    // （地图边缘固定落点坐标才成立），因此方案3 跳过这里的随机侧滑。
    val plan = getConfigRuntime(Schema.BUILDER_BASE_SETTINGS.NIGHT_WORLD_DEPLOY_PLAN.key)

    // 源四象限下兵几何：随机选一个象限用于滑屏视角。
    // 落点不再用"象限中点单点"——实机发现该点会压在基地建筑区(不可下兵区域)，
    // 改为四象限部署线上的多点候选，选卡后依次尝试（详见 [buildDeployPoints]）。
    if (plan != "3") {
        val side = (DeployGeometry.topSides + DeployGeometry.bottomSides).random()
        if (side.isTop) {
            swipe(981, 485, 0, 0, delayTime = 120)
        } else {
            swipe(100, 117, 1280, 720, delayTime = 120)
        }
        delayWithMultiplier(100)
    }
    val deployPoints = buildDeployPoints()

    // 战争机器（夜世界王，单体英雄）：这里只用色特征记录卡槽坐标（释放技能时要靠它兜底，
    // 英雄放下后卡槽会变灰、特征就匹配不到了）。真正的"选卡 + 下兵"统一交给 deployTroops，
    // 由它每轮先判断状态（是否已派完所有兵力 / 充能是否就绪）再动手。
    machineSlot = findMultiColors(schema = MyColors.BuilderBaseMachine)?.let { Point(it.x, it.y) }
    if (machineSlot == null) {
        accountLog("未找到战争机器特征，退回固定坐标 (125,610)")
        machineSlot = Point(125, 610)
    }

    // T14：夜飞机（空中机器）——同样只记录卡槽位置供技能释放
    battleCopterSlot = findMultiColors(schema = MyColors.BuilderBaseBattleCopter)?.let { Point(it.x, it.y) }
    if (battleCopterSlot == null) {
        accountLog("未找到夜飞机特征，跳过（本账号可能未解锁战斗直升机）")
    }

    if (!isNormal) return
    // 先用主用下兵点（源四象限部署线，常规布局够用）；只有在整轮都没把兵下出去时
    // （例如对方基地铺满画面、只有边缘能下兵的少数布局），才启用保底边缘点再试一轮。
    // 夜世界下兵方案分流（默认方案3）：
    //   0/缺省/3 → 方案3（卡位式：全卡位轮询 + 固定边缘落点 + 白框识别）
    //   1        → 当前方案（主世界几何+点选落点）
    //   2        → 方案2（源项目保真：函数123a/323a/128a）
    accountLog("夜世界：选用下兵方案 = ${when (plan) {
        "1" -> "当前方案"
        "2" -> "方案2(源保真)"
        else -> "方案3(卡位式,默认)"
    }}")
    when (plan) {
        "1" -> {
            val mainDeployDone = deployTroops(deployPoints)
            if (!mainDeployDone) {
                accountLog("夜世界：主用落点未把兵下出去，改用保底边缘点重试")
            }
            val deployDone = mainDeployDone || deployTroops(buildFallbackDeployPoints())
            // 下兵轮询到此结束。按用户要求把轮询拆成两个：
            //   · 下兵轮询 [deployTroops] —— 只管下兵，绝不碰技能；
            //   · 技能轮询 [releaseSkillsLoop] —— 下兵完成后进入，只管放技能（英雄 + 各兵种部队技能）。
            // 这样两者不会交错，也就不会出现"同一张卡被点两次 → 放技能 → 选卡丢失 → 下兵失败"。
            if (deployDone) {
                accountLog("夜世界：本轮下兵完成 → 进入技能轮询")
                releaseSkillsLoop()
            } else {
                accountLog("夜世界：本轮未能下兵（可能存在无效落点），跳过技能轮询")
            }
        }
        "2" -> {
            // 方案2（源项目保真方案）：内部完成下兵与技能轮询
            builderBaseAttackSource()
        }
        else -> {
            // 0/3/缺省 都走方案3（默认）
            builderBaseAttackSource3()
        }
    }
}

/**
 * 夜世界下兵主循环（单轮最多 30 个、每轮先判状态再动手）。
 *
 * **这里只下兵，绝不点技能**：技能点的是兵卡槽，会打乱选卡状态（用户实机确认），
 * 技能统一交给独立的技能轮询 [releaseSkillsLoop]，由 [normalBattle] 在下兵完成后调用。
 *
 * 每轮流程：
 *   ① 「已经派出所有兵力」（上方多一行红字）→ 收工；
 *   ② 「请选择其他兵种」→ 刚那张卡已放完，标记跳过、换兵继续；
 *   ③ 否则挑一张"还有兵可下"的卡（已变灰的卡跳过），点一次卡 + 点一个候选落点。
 *
 * 注意：**点一次卡 = 选中，点第二次同一张卡 = 放该兵种技能**（不是取消选中），
 * 所以每轮只点一次卡，否则会顺手把技能放掉并清空选中，紧跟的落点就白费。
 */
/**
 * 落点是否安全（落在可下兵的区域、而不是界面 UI 上）。
 *
 * 已知禁区：
 *   · **左上角**：不可选区域，点到不落兵（用户实测"下兵落点有时会点到左上角的不可选位置"）；
 *   · **左侧「结束战斗」按钮** (36~136, 474~521)：点到会直接结束对局；
 *   · 贴屏幕边缘太近的点：多半落在界面外框上。
 * 无论主用落点还是保底环带，都先过这一层过滤。
 */
private fun isSafeDeployPoint(p: Point): Boolean {
    if (p.x < 70 || p.y < 70 || p.x > 1210 || p.y > 650) return false    // 太贴边
    if (p.x < 230 && p.y < 240) return false                             // 左上角不可选区
    if (p.x < 170 && p.y in 460..530) return false                       // 「结束战斗」按钮
    return true
}

private suspend fun deployTroops(deployPoints: List<Point>): Boolean {
    // 先剔掉不安全落点（贴边 / 左上角不可选区 / 结束战斗按钮）
    val points = deployPoints.filter { isSafeDeployPoint(it) }
    val dropped = deployPoints.size - points.size
    if (dropped > 0) accountLog("夜世界：落点过滤，剔除 $dropped 个不安全点（剩 ${points.size} 个）")
    if (points.isEmpty()) return true
    var deployed = 0
    var guard = 0
    var lastIdx = -1
    var lastSlot = 0
    // 上一轮点完落点的那张卡：下一轮用新截图复查"选中是否被清空"，据此判断落点有没有真的下出兵
    // （下兵成功 → 游戏清空选中 → 白框消失）。这样不必为复查再单独截一次图，省一次截图耗时。
    var pendingCard: Point? = null
    // 连续多少个回合复查都显示"落点无效"（没有兵下去） —— 用于兜底收工
    var noProgressRounds = 0
    while (deployed < MAX_TROOPS_PER_ROUND && guard++ < MAX_TROOPS_PER_ROUND * 4) {
        val screen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult

        // ⓪ 复查上一轮落点（用本轮这张截图，不额外截图）
        pendingCard?.let { pc ->
            if (pc.x >= 180) { // 英雄卡没有白框判定，跳过
                val (still, _) = isCardSelected(screen, pc.x)
                accountLog(
                    if (still) "下兵诊断：③ 上一轮落点无效（白框仍在=选中未清空）"
                    else "下兵诊断：③ 上一轮落点生效（白框消失=兵已下去）"
                )
                noProgressRounds = if (still) noProgressRounds + 1 else 0
                // 这一轮的落点一个兵都没下去 → 这张卡（或这批落点）不可用，换下一张，
                // 否则会一直卡在同一张卡上反复空点。按**卡位号**记录（同兵种有 6 张卡，
                // 记兵种序号会把其余同款卡一起跳过）。
                if (still && lastSlot > 0) {
                    accountLog("下兵诊断：卡位#$lastSlot 这批落点无效 → 标记跳过，换下一张")
                    exhaustedSlots.add(lastSlot)
                }
            }
            pendingCard = null
        }

        // ① 出现下兵提示红字 → 切换到下一个兵种。
        //    用户实测：某个兵种下完时游戏会提示「请选择其他兵种」，此时应当换兵继续下；
        //    而旧代码把这条红字当成「已经派出所有兵力」直接收工，导致第一个兵种放完就停止下兵。
        //    两种提示位置/颜色相同、无法区分，所以统一按换兵处理（真的没兵可下时由 card==null 收工）。
        //    用户要求：**只有"还有兵可下"的卡才允许被点击**。放空的卡位色特征仍会匹配到，
        //    所以见到提示就把"上一次点的那张卡"标记为已放完，之后挑卡时直接跳过它。
        //
        //    先区分两种提示（主红字重合，靠上方多出的一行判断）：
        //      · 「已经派出所有兵力」→ 全部下完，收工；
        //      · 「请选择其他兵种」  → 刚那张卡放完，换兵继续。
        if (deployed % 5 == 0) {
            accountLog("下兵诊断：下兵提示红字数=${deployHintRedCount(screen)}（阈值 60）")
        }
        if (isAllTroopsDeployedHint(screen)) {
            accountLog("夜世界：检测到「已经派出所有兵力」，下兵结束（已下 $deployed 个）")
            return true
        }
        // 兜底收工：连续这么多轮复查都显示"落点无效"，说明已经没有能下的地方了，
        // 避免像实机那样反复跑满 30 次上限还停不下来。
        if (noProgressRounds >= 6) {
            accountLog("夜世界：连续 $noProgressRounds 轮落点无效，判定无法继续下兵（已下 $deployed 个）")
            return true
        }
        if (hasDeployHint(screen) && (lastSlot > 0 || lastIdx >= 0)) {
            // 按卡位号记录（同兵种 6 张卡，记兵种序号会把其余同款卡一起跳过）
            val newly = if (lastSlot > 0) exhaustedSlots.add(lastSlot) else exhaustedCards.add(lastIdx)
            accountLog(
                "夜世界：下兵提示 → ${if (lastSlot > 0) "卡位#$lastSlot" else "卡#$lastIdx"} 已放完" +
                    "${if (newly) "，标记跳过" else "（重复标记）"}（本轮尝试 $deployed 次）"
            )
            // 诊断：就在"某兵种刚下完"的这个时刻，看技能条是什么状态 ——
            // 用户反馈的现象是"一个兵种下完就放技能"，这里把两条技能条的存在与否记下来，
            // 用来判断技能到底是不是在这个时刻被点掉的（下一段主循环日志会给出点击来源）。
            val heroBar = findMultiColors(byteBuffer = screen, schema = MyColors.HeroChargeReady)
            val troopBar = findMultiColors(byteBuffer = screen, schema = MyColors.TroopSkills)
            accountLog(
                "下完间隙诊断：英雄充能条=${heroBar?.let { "(${it.x},${it.y})" } ?: "无"}" +
                    " 部队技能条=${troopBar?.let { "(${it.x},${it.y})" } ?: "无"}"
            )
            lastIdx = -1
            lastSlot = 0
            delayWithMultiplier(300) // 等提示消失再继续，避免同一帧反复命中
            continue
        }

        // ② 按优先级顺序扫卡，**碰到第一张"还有兵可下"的卡就停**。
        //
        // 性能注意：findMultiColors 是全屏模板匹配，之前每轮把 16 个特征全扫一遍，
        // 一轮要 1.25 秒左右（实机日志 03:26:15.660 / 17.015 / 18.367 / 19.499），下兵间隔太长。
        // 顺序扫、命中即停通常 1~3 次匹配就够（英雄/夜飞机/女巫排在前，多数局第一张就中）。
        var card: Point? = null
        var cardIdx = -1
        var cardSlot = 0
        // 判据失效兜底用：记住"按特征匹配到的第一张卡"，万一紫色条判据在实机上把全部卡
        // 都判成已下放（2026-09-24 实机翻过一次：整局一个兵都没下），就退回按特征下兵。
        var fallbackCard: Point? = null
        var fallbackIdx = -1
        var fallbackSlot = 0
        for ((idx, schema) in TROOP_CARDS.withIndex()) {
            if (idx in exhaustedCards) continue          // 英雄等不在卡位网格内的卡
            // 同一兵种占多个卡位，而 findMultiColors 只返回第一个匹配位置（最左边那张）。
            // 那张若已下完，把搜索起点右移继续找同兵种的下一个卡位，否则同款兵永远轮不到。
            var fromX = 0
            while (fromX < 1279) {
                val found = findMultiColors(
                    byteBuffer = screen,
                    schema = ColorSchema.rescope(schema, fromX, 596, 1279, 700, 0)
                ) ?: break
                val slot = slotIndex(found.x)
                if (slot in exhaustedSlots) {
                    fromX = found.x + 40                 // 该卡位已下完 → 找同兵种的下一个卡位
                    continue
                }
                if (fallbackCard == null) {
                    fallbackCard = Point(found.x, found.y)
                    fallbackIdx = idx
                    fallbackSlot = slot
                }
                // 英雄卡（含夜飞机这类"卡栏最左、x<180"的卡）**另走一套，不套紫色条判据**：
                // 它们上方那条粉色的是英雄**充能条**（y559~567、D36AFF），同样满足紫色判据，
                // 且位置偏上（<578），会被误判成"兵已下场"—— 实机表现就是战斗刚开始英雄就被
                // 判已下放，整局一个英雄都不下（2026-09-24 实机日志已确认）。
                // 它们各自是否点过由 [offGridTapped] 按兵种序号分别记录。
                if (found.x >= 180) {
                    val barTop = cardSkillBarTop(screen, found.x)
                    if (!isCardDeployable(screen, found.x)) {
                        accountLog(
                            "下兵诊断：卡#$idx @(${found.x},${found.y}) 卡位#$slot " +
                                "紫色条顶部=${if (barTop < 0) "无条" else barTop} → 兵已下放，不点"
                        )
                        if (slot > 0) exhaustedSlots.add(slot)
                        fromX = found.x + 40
                        continue
                    }
                    accountLog("下兵诊断：卡#$idx @(${found.x},${found.y}) 卡位#$slot 紫色条顶部=$barTop → 可点")
                } else {
                    accountLog("下兵诊断：卡#$idx @(${found.x},${found.y}) 英雄/非网格卡 → 不套紫色条判据，按选卡逻辑处理")
                }
                card = Point(found.x, found.y)
                cardIdx = idx
                cardSlot = slot
                break
            }
            if (card != null) break
        }
        // 整体兜底：**一个兵都还没下**却被判"全部已下放" → 判据在实机上整体失效，
        // 直接按特征匹配下兵，绝不能出现整局一个兵都不下的情况。
        if (card == null && deployed == 0 && fallbackCard != null) {
            accountLog("下兵诊断：紫色条判据把全部卡判为已下放，但本轮还没下过兵 → 判据失效兜底，按特征下兵")
            card = fallbackCard
            cardIdx = fallbackIdx
            cardSlot = fallbackSlot
        }
        if (card == null) {
            accountLog("夜世界：已无可下兵种卡（全部已下放），停止下兵（本轮尝试 $deployed 次）")
            return true
        }
        lastIdx = cardIdx
        lastSlot = cardSlot
        val cardName = "${TROOP_CARDS[cardIdx].name ?: "未命名"}@#$cardIdx@卡位#$cardSlot"

        // ③ 选卡 + 下兵（用户实机确认的机制）：
        //   · 点一次卡         → **选中**该兵种；
        //   · 再点一次同一张卡 → **释放该兵种的技能**（不是取消选中！），并清空选中。
        //   所以每轮只点一次卡；非网格卡（卡栏最左、x<180：英雄 / 夜飞机）白框判定无效，
        //   改成"每张只点一次"，按兵种序号分别记在 [offGridTapped]。
        val p = points[deployed % points.size]             // 轮转候选落点
        val isOffGrid = card.x < 180
        val (selected, whiteN) = if (isOffGrid) false to -1 else isCardSelected(screen, card.x)
        val needTap = if (isOffGrid) cardIdx !in offGridTapped else !selected
        if (needTap) {
            accountLog(
                "下兵诊断：卡#$cardName " +
                    (if (isOffGrid) "非网格卡(只点一次选卡)" else "未选中(左右白线=$whiteN)") +
                    " → 点卡 @(${card.x},${card.y}) 再点落点 @(${p.x},${p.y})"
            )
            logSlotTap("下兵选卡", card.x, card.y)
            TouchActions.tap(card.x, card.y, delayTime = 100)
            if (isOffGrid) offGridTapped.add(cardIdx)
        } else {
            accountLog(
                "下兵诊断：卡#$cardName " +
                    (if (isOffGrid) "非网格卡已点过" else "已选中(左右白线=$whiteN)") +
                    " → 直接点落点 @(${p.x},${p.y})"
            )
        }
        // 连点多个落点（一张女巫卡 20 个兵，一轮一个太慢）。落点按顺序轮转，
        // 这样即使中间有几个落在不可部署区，后面的点还能继续下。
        var taps = 0
        for (i in 0 until DEPLOY_TAPS_PER_ROUND) {
            if (deployed >= MAX_TROOPS_PER_ROUND) break
            val pp = points[deployed % points.size]
            TouchActions.tap(pp.x, pp.y, delayTime = 120)
            deployed++
            taps++
        }
        accountLog("下兵诊断：卡#$cardName 连点 $taps 个落点（本轮累计 $deployed 次）")
        // 英雄/夜飞机是单体：一轮（1 次选卡 + 连点 8 个落点）就够。用完**立刻标记跳过** ——
        // 否则英雄排在 TROOP_CARDS[0] 优先级最高、又永远不会"变灰"，就会一直被挑中，
        // 兵种卡轮不到，表现为"下完英雄要等很久才开始下兵"。
        if (isOffGrid) exhaustedCards.add(cardIdx)
        // 落点结果留到下一轮用那张截图复查（省掉一次额外截图，见循环开头 pendingCard）
        pendingCard = card
    }
    accountLog("夜世界：本轮下兵达到上限 $MAX_TROOPS_PER_ROUND 个，仍未下完（可能存在无效落点）")
    return false
}

private suspend fun waitLoop() {
    val totalDuration = 15_000L
    val startTime = System.currentTimeMillis()
    // Guaranteed non-null at use site due to early return guard below
    var lastPosition: Point?

    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        val remainingMs = totalDuration - elapsed

        val pos = findMultiColors(schema = MyColors.CancelAttackSearch)
        if (pos != null) lastPosition = pos
        else return
        if (remainingMs <= 0) break

        val remainingSeconds = remainingMs / 1000.0
        accountLog("搜索中，剩余 ${"%.1f".format(remainingSeconds)} 秒")
        delayWithMultiplier(1000)
    }
    TouchActions.tap(lastPosition.x, lastPosition.y, delayTime = 200)
}

private suspend fun builderBaseTrainTroops() {
    // Attempt to locate the initial training button
    val trainingButton = findMultiColorsUntil(schemas = listOf(MyColors.TrainTroops), duration = 1500)

    if (trainingButton == null) {
        accountLog("夜世界练兵失败")
        return
    }

    // Enter training menu
    TouchActions.tap(trainingButton.x, trainingButton.y, delayTime = 400)

    // Clear existing troops if the clear button is present
    val cleanTroops = findMultiColorsUntil(schemas = listOf(MyColors.RedCleanButton), duration = 1000)
    if (cleanTroops != null) {
        TouchActions.tap(cleanTroops.x, cleanTroops.y, delayTime = 500)
    }

    // Identify troop type and train
    val trainNightWitch = findMultiColors(schema = MyColors.TrainNightWitch)
    if (trainNightWitch != null) {
        // Train Night Witches based on detected location
        accountLog("练暗夜女巫")
        tapRepeat(trainNightWitch.x, trainNightWitch.y)
    } else {
        // Fallback to Barbarians using original hardcoded coordinates
        accountLog("未检测到暗夜女巫，练野蛮人\n（有暗夜女巫后会练暗夜女巫）")
        tapRepeat(278, 491)
    }

    // Close the training interface
    TouchActions.tap(1152, 102, delayTime = 300)
}

suspend fun builderBaseTrainWithConditions(): Boolean {
    accountLog("开始练兵检测")
    val storageKey = StorageKeys.withAccountNumber(StorageKeys.BUILDER_BASE_TRAIN_TROOPS, InGamesVars.currentAccountNumber)

    if (checkMemoryFile(storageKey, 1440)) {
        accountLog("24小时内未练兵，开始练兵")
        builderBaseTrainTroops()
        writeMemory(storageKey, (System.currentTimeMillis() / 60_000).toString())
        return enterMainScreen()
    }

    // Training was already completed within 24 hours
    accountLog("24小时内已练兵，跳过")
    return true
}