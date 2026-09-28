package com.coc.zkqcode.jar.code.mainbase.clangames

import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase.ClanGamesRewardIcons
import com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase.ClanGamesTaskIcons
import com.coc.zkqcode.jar.code.universal.GameScene
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsAll
import com.coc.zkqcode.jar.code.universal.detectCurrentScene
import com.coc.zkqcode.jar.code.universal.ensureZoomedOutMainBase
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.checkMemoryFile
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.ui.schema.Schema

/**
 * 部落竞赛（Clan Games）主流程，移植自源脚本 `awcocx_main.lua`：
 *  - L42649~L42683 主循环调度：开关 + 节流（3 小时一次）→ 滑动打开竞赛页 → 函数233a
 *  - 函数233a L33513 竞赛主流程：找竞赛屋 → 进界面 → 判已接/蓝徽章/积分满 → 领奖励 → 接任务
 *  - 函数230a L33267 接任务：点任务图标 → 点绿色「接受」按钮 → 复核是否还在（还在=接取失败）
 *  - 函数231a L33301 找可接任务图标（按任务图标模板挑夜世界任务）
 *  - 函数232a L33559 领奖励：在指定行区域找奖励图标 → 点 → 绿色确认
 *
 * 坐标同项目其他模块：源脚本竖屏 720x1280 → 本项目横屏 1280x720 的 90° 映射。
 *
 * 与源脚本的差异（有意为之）：
 *  1. 源用 `函数67a/函数194a` 做进界面与清弹窗（本项目对应 [detectCurrentScene] 的载入/弹窗处理），
 *     这里只在确认处于主村庄时才动手，失败直接跳过，不做额外的清弹窗点击。
 *  2. 源用 OCR 判断已接任务是"主世界"还是"夜世界"（建筑大师）；本项目尚未建中文字库，
 *     改为"只接夜世界任务"这一侧的语义：靠任务图标模板挑夜世界任务，已接任务时不做额外判断。
 *  3. 源用 `已接主竞赛/已接夜竞赛/已做满` 全局 Lua 变量保存状态；本项目用 [ClanGamesState]
 *     内存态（每次进面板都会重新识别，无需持久化），只有"检查节流"落账号记忆。
 *
 * TODO(真机标定)：所有坐标/颜色特征均来自 2023 年版源脚本，首轮真机必须逐个复核。
 */

/** 竞赛检查节流：源 L42650「主检查竞赛」= 3 小时检查一次。 */
private const val CHECK_INTERVAL_MINUTES = 180

/** 没找到竞赛屋时的重试间隔：源 L42660 = 600 秒。 */
private const val NOT_FOUND_INTERVAL_MINUTES = 10

/** 进竞赛面板后的等待上限：源 L33522 循环 30 次 × 200ms。 */
private const val PANEL_WAIT_MS = 6_000L

/** 面板状态轮询次数：源 L33531 循环 15 次 × 100ms。 */
private const val PANEL_STATE_LOOPS = 15

/** 竞赛面板里的三种稳定状态（源 L33531~L33550）。 */
private enum class PanelState {
    /** 已经有已接任务（蓝标）。 */
    TASK_ACCEPTED,

    /** 任务已完成待领奖（蓝徽章 ≥3）。 */
    BLUE_BADGE,

    /** 积分已做满。 */
    POINTS_FULL,

    /** 三种都不是（可以继续接任务 / 领奖励）。 */
    NONE
}

/**
 * 竞赛的内存态。每轮进面板都会重新识别，"已接任务"之外的状态不持久化；
 * "是否已接任务"落账号记忆（[StorageKeys.CLAN_GAMES_ACCEPTED]），因为
 * 夜世界对战的切号局数（源 L42672「已接夜竞赛 → 夜打鱼」）在竞赛检查**之前**执行，
 * 必须能读到上一轮接任务的结果。
 */
object ClanGamesState {
    private var lastAccountNumber: Int = -1

    /** 本轮是否已接取竞赛任务。 */
    var hasAcceptedTask: Boolean = false
        private set

    /** 已接任务是否属于夜世界（建筑大师）。主世界任务时该字段为 false。 */
    var isNightTask: Boolean = false
        private set

    /** 本轮是否已检测到"积分/任务已做满"。 */
    var pointsFull: Boolean = false
        private set

    private fun acceptedKey(account: Int): String =
        StorageKeys.withAccountNumber(StorageKeys.CLAN_GAMES_ACCEPTED, account)

    /** 记忆里存的实际值："" / "0" = 无，"main" = 已接主世界任务，"night" = 已接夜世界任务。 */
    private suspend fun acceptedValue(account: Int): String = readMemory(acceptedKey(account))

    /** 切号时复位，并从账号记忆恢复"上一轮是否已接竞赛任务"及任务类型。 */
    suspend fun resetForAccount(accountNumber: Int) {
        if (lastAccountNumber == accountNumber) return
        lastAccountNumber = accountNumber
        pointsFull = false
        val v = acceptedValue(accountNumber)
        hasAcceptedTask = v == "main" || v == "night"
        isNightTask = v == "night"
    }

    suspend fun markAccepted(isNight: Boolean) {
        hasAcceptedTask = true
        isNightTask = isNight
        writeMemory(acceptedKey(InGamesVars.currentAccountNumber), if (isNight) "night" else "main")
    }

    /** 手上已没有任务（任务失败 / 已放弃）。 */
    suspend fun markNoTask() {
        hasAcceptedTask = false
        isNightTask = false
        writeMemory(acceptedKey(InGamesVars.currentAccountNumber), "0")
    }

    /** 积分/任务已做满（源 L42634：已做满时把「接竞赛」一并关掉）。 */
    suspend fun markPointsFull() {
        pointsFull = true
        hasAcceptedTask = false
        isNightTask = false
        writeMemory(acceptedKey(InGamesVars.currentAccountNumber), "0")
    }
}

/**
 * 部落竞赛阶段入口（供主循环 [com.coc.zkqcode.jar.code.runMainScript] 调用）。
 *
 * 竞赛是**附加功能**：开关全关时直接返回；流程中任何一步拿不准都记日志并跳过，
 * 不抛错、不中断主循环（因此返回值恒为 true，保留 Boolean 只为与主循环其他阶段签名一致）。
 */
suspend fun playClanGames(): Boolean {
    val account = InGamesVars.currentAccountNumber
    ClanGamesState.resetForAccount(account)
    val acceptEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.DO_CLAN_GAMES.key)
    val claimEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.CLAIM_CLAN_GAME_REWARDS.key)
    if (!acceptEnabled && !claimEnabled) return true

    // 节流：源 L42650（3 小时一次）与 L42660（没找到竞赛屋时 10 分钟后再找）
    val notFoundKey = StorageKeys.withAccountNumber(StorageKeys.CLAN_GAMES_NOT_FOUND, account)
    if (!checkMemoryFile(notFoundKey, NOT_FOUND_INTERVAL_MINUTES)) {
        ShowMessage("账号$account，竞赛：${NOT_FOUND_INTERVAL_MINUTES}分钟内刚找过且没找到竞赛屋，跳过")
        return true
    }
    val checkKey = StorageKeys.withAccountNumber(StorageKeys.CLAN_GAMES_CHECK, account)
    if (!checkMemoryFile(checkKey, CHECK_INTERVAL_MINUTES)) {
        ShowMessage("账号$account，竞赛：${CHECK_INTERVAL_MINUTES}分钟内已检查过竞赛，跳过")
        return true
    }

    SceneState.setFlowNode("竞赛")
    if (detectCurrentScene() != GameScene.MAIN_VILLAGE) {
        ShowMessage("账号$account，竞赛：当前不在主村庄，跳过")
        return true
    }

    val hut = findClanGamesHut()
    if (hut == null) {
        // 源 L42654：先斜向滑一下（竞赛屋可能在屏幕外），再找一次
        TouchActions.swipe(336, 188, 691, 602, delayTime = 300)
        delayWithMultiplier(500)
        val hut2 = findClanGamesHut()
        if (hut2 == null) {
            writeMemory(notFoundKey, (System.currentTimeMillis() / 60_000).toString())
            ShowMessage("账号$account，竞赛：没找到竞赛屋，${NOT_FOUND_INTERVAL_MINUTES}分钟后再试")
            return true
        }
        return openAndHandlePanel(hut2, acceptEnabled, claimEnabled, account, checkKey)
    }
    return openAndHandlePanel(hut, acceptEnabled, claimEnabled, account, checkKey)
}

/** 点竞赛屋 → 等面板出现 → 处理 → 关闭面板，并写入节流时间戳。 */
private suspend fun openAndHandlePanel(
    hut: Point, acceptEnabled: Boolean, claimEnabled: Boolean, account: Int, checkKey: String
): Boolean {
    ShowMessage("账号$account，竞赛：点击竞赛屋 @(${hut.x},${hut.y})")
    TouchActions.tap(hut.x, hut.y, delayTime = 700)
    if (!waitForClanGamesPanel()) {
        ShowMessage("账号$account，竞赛：点竞赛屋后没进到竞赛界面，跳过")
        return true
    }
    writeMemory(checkKey, (System.currentTimeMillis() / 60_000).toString())
    handleClanGamesPanel(acceptEnabled, claimEnabled)
    closeClanGamesPanel()
    return true
}

/** 在主村庄里找竞赛屋（源 L33514 的 findImage「竞赛屋.png」+ L33517 的颜色兜底）。 */
private suspend fun findClanGamesHut(): Point? {
    ensureZoomedOutMainBase()
    return findMultiColors(schema = MyColors.ClanGamesHut, increment = 1)
}

/** 等待竞赛面板出现（源 L33522：最多 30 × 200ms）。 */
private suspend fun waitForClanGamesPanel(): Boolean {
    val deadline = System.currentTimeMillis() + PANEL_WAIT_MS
    while (System.currentTimeMillis() < deadline) {
        if (isOnClanGamesPanel()) return true
        delayWithMultiplier(200)
    }
    return false
}

/**
 * 是否在竞赛面板上。源只用 `竞赛界面` 一个特征；本项目该特征是从"友谊战配置页"标定的，
 * 为稳妥起见再并上右上角「我的任务 / 全部任务」切换按钮（源 L33616）作为佐证。
 */
private suspend fun isOnClanGamesPanel(): Boolean {
    if (findMultiColors(schema = MyColors.ClanGamesEntry, increment = 1) != null) return true
    return findMultiColors(schema = MyColors.ClanGamesTasksTab, increment = 1) != null
}

/** 竞赛面板处理主流程（源 函数233a L33529~L34052）。 */
private suspend fun handleClanGamesPanel(acceptEnabled: Boolean, claimEnabled: Boolean) {
    val account = InGamesVars.currentAccountNumber
    val state = detectPanelState()
    when (state) {
        PanelState.TASK_ACCEPTED -> {
            // 源 L33941~L33962：点开已接任务 → OCR 描述 → 判「建筑大师」决定是夜世界还是主世界
            detectAcceptedTaskType()
            return
        }

        PanelState.BLUE_BADGE -> ShowMessage("账号$account，竞赛：任务已完成（蓝徽章），准备领奖励")
        PanelState.POINTS_FULL -> ShowMessage("账号$account，竞赛：积分已做满")
        PanelState.NONE -> ShowMessage("账号$account，竞赛：未检测到已接任务")
    }

    // 源 L33551：竞赛结束「可领奖励」标识出现且开了领奖 → 领奖励
    if (claimEnabled && findMultiColors(schema = MyColors.ClanGamesRewardsReady, increment = 1) != null) {
        ShowMessage("账号$account，竞赛结束领奖励")
        claimClanGameRewards()
        return
    }
    if (!acceptEnabled) return

    // 源 L33657：已接任务分支下的各种状态处理
    if (findMultiColors(schema = MyColors.ClanGamesTaskCompleted, increment = 1) != null) {
        ShowMessage("账号$account，竞赛：任务完成")
    }
    if (findMultiColors(schema = MyColors.ClanGamesPointsFull, increment = 1) != null) {
        ClanGamesState.markPointsFull()
        ShowMessage("账号$account，竞赛：积分已做满，关闭竞赛面板")
        return
    }
    if (findMultiColors(schema = MyColors.ClanGamesNoClan, increment = 1) != null) {
        ShowMessage("账号$account，竞赛：未加入部落，无法参加竞赛")
        return
    }
    val countdown = findMultiColors(schema = MyColors.ClanGamesCountdownPopup, increment = 1)
    if (countdown != null) {
        ShowMessage("账号$account，竞赛：竞赛开始倒计时窗口，点掉")
        TouchActions.tap(countdown.x, countdown.y, delayTime = 600)
        return
    }
    if (findMultiColors(schema = MyColors.ClanGamesCooldown1, increment = 1) != null ||
        findMultiColors(schema = MyColors.ClanGamesCooldown2, increment = 1) != null
    ) {
        ShowMessage("账号$account，竞赛：放弃任务冷却中，跳过")
        return
    }
    if (findMultiColors(schema = MyColors.ClanGamesTaskFailed, increment = 1) != null) {
        ShowMessage("账号$account，竞赛：任务失败，放弃当前任务")
        abandonCurrentTask()
        return
    }
    if (state == PanelState.TASK_ACCEPTED) return

    // 源 L33964：没接任务 → 先找简单任务，滑屏后再找一遍；再找"简单+困难"
    if (!acceptNightTask()) {
        // 源 L33998：一个可接的夜世界任务都没有 → 放弃第一张任务卡刷新任务列表
        ShowMessage("账号$account，竞赛：没有可接的夜世界任务，放弃第一个任务刷新任务列表")
        abandonFirstTask()
    }
}

/** 面板状态轮询：源 L33531 循环 15 次 × 100ms，命中即返回。 */
private suspend fun detectPanelState(): PanelState {
    repeat(PANEL_STATE_LOOPS) {
        if (findMultiColors(schema = MyColors.ClanGamesTaskAccepted, increment = 1) != null) {
            return PanelState.TASK_ACCEPTED
        }
        // 蓝徽章要"同一行出现 ≥3 个"才算数（源 L33539）
        if (findMultiColorsAll(schema = MyColors.ClanGamesBlueBadge, maxMatches = 4).size >= 3) {
            return PanelState.BLUE_BADGE
        }
        if (findMultiColors(schema = MyColors.ClanGamesPointsFull, increment = 1) != null) {
            return PanelState.POINTS_FULL
        }
        delayWithMultiplier(100)
    }
    return PanelState.NONE
}

/**
 * 判断已接任务是「夜世界(建筑大师)」还是「主世界」任务（源 L33941~L33962）。
 *
 * 源脚本流程：点开已接任务卡 → OCR 任务描述区域 → 出现"建筑大师"即夜世界任务，
 * 否则是主世界任务 → 关掉任务详情。OCR 区域竖屏 (477,661,552,799) 按 90° 映射成横屏 (661,167,799,242)。
 *
 * 注意：本项目此前"无中文字库"，所以竞赛一直只接夜世界任务、且已接任务不做类型判断（见 T11）。
 * 现在有了 [ChineseTextReader]，这里补上类型识别。若 OCR 不可用 / 读不到中文，保守当作主世界任务
 * （只是夜世界对战不会切到"接取竞赛后"那档局数，不影响主循环）。
 */
private suspend fun detectAcceptedTaskType() {
    val account = InGamesVars.currentAccountNumber
    val accepted = findMultiColors(schema = MyColors.ClanGamesTaskAccepted, increment = 1) ?: run {
        ShowMessage("账号$account，竞赛：检测到已接任务，但没定位到任务卡，按主世界任务处理")
        ClanGamesState.markAccepted(isNight = false)
        return
    }
    // 点开任务详情（源 L33948 taps(512,548) → 横屏 (548,207)）
    TouchActions.tap(accepted.x, accepted.y, delayTime = 1200)
    val isNight = ChineseTextReader.containsAny(
        startX = 661, startY = 167, endX = 799, endY = 242,
        keywords = listOf("建筑大师", "夜世界", "夜营", "夜间")
    )
    ClanGamesState.markAccepted(isNight = isNight)
    ShowMessage("账号$account，竞赛：已接任务，类型=${if (isNight) "夜世界(建筑大师)" else "主世界"}")
    // 关掉任务详情回到竞赛面板；外层 closeClanGamesPanel 再点一次退出面板
    TouchActions.tap(1120, 68, delayTime = 600)
}

/**
 * 找可接的夜世界任务并接取（源 L33964~L33997：两轮，先"简单"，再"简单+困难"，中间滑屏）。
 * @return true=已成功接取任务。
 */
private suspend fun acceptNightTask(): Boolean {
    for (round in 1..2) {
        val templates = if (round == 1) {
            ClanGamesTaskIcons.SIMPLE_TEMPLATES
        } else {
            ClanGamesTaskIcons.ALL_TEMPLATES
        }
        val icon = findTaskIcon(templates)
        if (icon != null && acceptTaskAt(icon)) return true

        // 源 L33984：向上滑一屏再找（横屏 x 固定 1106，y 497 → 149）
        TouchActions.swipe(1106, 497, 1106, 149, delayTime = 300)
        delayWithMultiplier(1000)
        if (!isOnClanGamesPanel()) break
        val icon2 = findTaskIcon(templates)
        if (icon2 != null && acceptTaskAt(icon2)) return true

        // 源 L33992：滑回列表顶部（横屏 x 固定 1126，y 134 → 534），为下一轮换方向做准备
        TouchActions.swipe(1126, 134, 1126, 534, delayTime = 200)
        delayWithMultiplier(2000)
    }
    return false
}

/** 在任务列表里按图标模板找一张可接任务（源 函数231a）。 */
private suspend fun findTaskIcon(templates: List<ColorSchema>): Point? {
    // 模板有几十组，先截一张图再逐个扫描，避免每组各截一次图（省掉几十次截图开销）
    val capture = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
    for (template in templates) {
        val hit = findMultiColors(schema = template, byteBuffer = capture, increment = 1)
        if (hit != null) return hit
    }
    return null
}

/**
 * 点任务图标 → 点绿色「接受」按钮 → 复核（源 函数230a）。
 * @return true=接取成功；false=没找到接受按钮，或点完后按钮还在（源判为"接竞赛失败"）。
 */
private suspend fun acceptTaskAt(icon: Point): Boolean {
    val account = InGamesVars.currentAccountNumber
    TouchActions.tap(icon.x, icon.y, delayTime = 1000)
    val accept = findMultiColors(schema = MyColors.ClanGamesAcceptButton, increment = 1)
    if (accept == null) {
        ShowMessage("账号$account，竞赛：没找到绿色「接受任务」按钮")
        return false
    }
    TouchActions.tap(accept.x, accept.y, delayTime = 2500)
    // 源 L33276：再找一次，按钮还在说明没接上
    if (findMultiColors(schema = MyColors.ClanGamesAcceptButton, increment = 1) != null) {
        ShowMessage("账号$account，竞赛：接竞赛失败")
        ClanGamesState.markPointsFull()
        return false
    }
    ClanGamesState.markAccepted(isNight = true)
    ShowMessage("账号$account，竞赛：已接取夜世界任务")
    return true
}

/**
 * 领竞赛奖励（源 L33551~L33656 + 函数232a L33559~L33631）：
 * 切到任务页 → 逐行找绿色勾 → 在每行里找奖励图标并点领取 → 下滑到底处理最后一行。
 */
private suspend fun claimClanGameRewards() {
    val account = InGamesVars.currentAccountNumber
    // 源 L33616：先点右上角切换按钮，确保停在奖励列表页
    findMultiColors(schema = MyColors.ClanGamesTasksTab, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 700)
    }
    val rows = findMultiColorsAll(schema = MyColors.ClanGamesRewardRow, maxMatches = 10)
    for (row in rows) {
        claimRewardInRow(row.x)
    }
    // 源 L33640：看不到那列绿色操作按钮说明列表没到底，下滑后再处理最后一行
    if (findMultiColors(schema = MyColors.ClanGamesGreenActionButton, increment = 1) == null) {
        TouchActions.swipe(900, 361, 500, 361, delayTime = 500)
        delayWithMultiplier(1000)
        val lastRow = findMultiColors(schema = MyColors.ClanGamesRewardRowBottom, increment = 1)
        if (lastRow != null) claimRewardInRow(lastRow.x)
    }
    // 源 L33650：点那列绿色操作按钮（继续 / 领取），并把检查时间推后 3 小时
    findMultiColors(schema = MyColors.ClanGamesGreenActionButton, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 1000)
        ShowMessage("账号$account，竞赛：已领取竞赛奖励")
    }
}

/**
 * 在指定奖励行里找奖励图标并领取（源 函数232a）。
 * @param rowX 该行绿色勾的横坐标；行区域由源 `函数232a(230, y-44, 520, y+52)` 换算而来。
 */
private suspend fun claimRewardInRow(rowX: Int) {
    val x1 = (rowX - 44).coerceAtLeast(0)
    val x2 = (rowX + 52).coerceAtMost(1279)
    // 同一行里要试 16 组图标，复用一张截图
    val capture = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
    for (icon in ClanGamesRewardIcons.ALL) {
        val scoped = ColorSchema.rescope(icon, x1, 199, x2, 489)
        val hit = findMultiColors(schema = scoped, byteBuffer = capture, increment = 1) ?: continue
        TouchActions.tap(hit.x, hit.y, delayTime = 700)
        findMultiColors(schema = MyColors.ClanGamesClaimConfirm, increment = 1)?.let {
            TouchActions.tap(it.x, it.y, delayTime = 700)
        }
        return
    }
    // 源 L33618：一组图标都没命中 → 在这一行里随机点一张橙色奖励卡
    val cards = findMultiColorsAll(
        schema = ColorSchema.rescope(MyColors.ClanGamesTaskCardOrange, x1, 199, x2, 489),
        maxMatches = 10
    )
    if (cards.isEmpty()) return
    val card = cards.random()
    // 源是 taps(x-30, y-30)（竖屏偏移），按 90° 映射换算为横屏 (-30, +30)
    TouchActions.tap((card.x - 30).coerceAtLeast(0), (card.y + 30).coerceAtMost(719), delayTime = 700)
    findMultiColors(schema = MyColors.ClanGamesClaimConfirm, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 700)
    }
}

/** 放弃当前已接任务（源 L33722~L33743：任务失败后的处理）。 */
private suspend fun abandonCurrentTask() {
    ClanGamesState.markNoTask()
    // 源 L33730 taps(512,548)：点任务卡片
    TouchActions.tap(548, 207, delayTime = 500)
    if (!isOnClanGamesPanel()) return
    // 源 L33734 taps(377,1030)：点放弃
    TouchActions.tap(1030, 342, delayTime = 3500)
    closeClanGamesPanel()
}

/** 没有可接任务时放弃第一张任务卡刷新列表（源 L33998~L34033）。 */
private suspend fun abandonFirstTask() {
    val account = InGamesVars.currentAccountNumber
    val card = findMultiColors(schema = MyColors.ClanGamesFirstTaskCard, increment = 1)
    if (card == null) {
        ShowMessage("账号$account，竞赛：没找到可放弃的第一张任务卡")
        return
    }
    TouchActions.tap(card.x, card.y, delayTime = 800)
    val giveUp = findMultiColors(schema = MyColors.ClanGamesGiveUpButton, increment = 1)
    if (giveUp == null) {
        ShowMessage("账号$account，竞赛：没找到放弃按钮")
        return
    }
    TouchActions.tap(giveUp.x, giveUp.y, delayTime = 3200)
    if (findMultiColors(schema = MyColors.ClanGamesAcceptButton, increment = 1) != null) {
        ShowMessage("账号$account，竞赛：放弃后仍无法接取（源判为已做满）")
        ClanGamesState.markPointsFull()
    }
    val confirm = findMultiColors(schema = MyColors.ClanGamesAbandonConfirm, increment = 1)
    if (confirm == null) return
    TouchActions.tap(confirm.x, confirm.y, delayTime = 1200)
    // 源这里点的是固定坐标 (247,774)；本项目点命中点（源固定点对应旧版 UI）
    findMultiColors(schema = MyColors.ClanGamesAbandonGreenConfirm, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 1000)
        ShowMessage("账号$account，竞赛：已放弃任务，刷新任务列表")
    }
}

/** 关闭竞赛面板（源 L34050 taps(651,1120)，竖屏 → 横屏 (1120,68)）。 */
private suspend fun closeClanGamesPanel() {
    TouchActions.tap(1120, 68, delayTime = 600)
}
