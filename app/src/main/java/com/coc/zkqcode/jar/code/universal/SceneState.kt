package com.coc.zkqcode.jar.code.universal

import android.graphics.Bitmap
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.fileactions.LogHelper
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.TemplateMatcher
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.smalltools.detectStuckNetworkSpinner
import com.coc.zkqcode.jar.code.universal.smalltools.isGameAtFront
import com.coc.zkqcode.jar.code.universal.smalltools.runGame
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reused from the legacy freescript (awcocx_main.lua) scene-detection architecture.
 *
 * The legacy script keeps two things the current project was missing:
 *   1. A single "what screen am I on right now" question, answered by a prioritized
 *      list of feature schemas (函数275a dispatch table + 是否主村庄界面/是否夜村庄界面 predicates).
 *   2. A run-control state machine (游戏运行控制: 运行/切号/界面超时/网络异常/...) that every
 *      wait-loop writes to, so the flow node is always observable instead of silently
 *      killing the game on timeout.
 *
 * This file is the Kotlin port of those two ideas. It does NOT hard-code navigation
 * coordinates; it only observes the screen so the caller can log/branch on the result.
 */

/**
 * The primary game screen the device is currently on.
 * [displayName] is the Chinese label shown to the user (matches the project's UI convention).
 */
enum class GameScene(val displayName: String) {
    /** Main village / home base (训练部队 button visible). */
    MAIN_VILLAGE("主村庄"),
    /** Training troops screen (left bar / 进攻 button visible). */
    TRAINING_PAGE("训练部队页面"),
    /** Battle / attack screen (进攻！ launch button visible). */
    BATTLE("战斗页面"),
    /** Builder base / night village. */
    BUILDER_BASE("夜世界"),
    /** Clan capital. */
    CLAN_CAPITAL("都城"),
    /**
     * 游戏启动/载入/过渡界面（合规告示黑屏、"搜索对手"乌云等）。
     * 这是**合法且短暂**的状态：调用方应继续等待载入完成，
     * 绝不能对它按返回键（在游戏里按返回会弹出"确定退出游戏？"确认框）。
     */
    LOADING("载入中"),
    /** None of the known gameplay screens could be identified. */
    UNKNOWN("未知页面")
}

/**
 * Run-control state machine ported from the legacy freescript `游戏运行控制` variable.
 * Every wait-loop / recovery entry writes one of these so the current flow node is explicit.
 */
enum class GameRunControl(val displayName: String) {
    /** Normal operation. */
    RUNNING("运行"),
    /** Switch account (login conflict / kicked / account switch option). */
    SWITCH_ACCOUNT("切号"),
    /** A wait-loop hit its countdown without reaching the target screen. */
    INTERFACE_TIMEOUT("界面超时"),
    /** Network dropped and did not recover. */
    NETWORK_ERROR("网络异常"),
    /** Game process must be restarted (not responding / error code). */
    RESTART_GAME("重启游戏"),
    /** Account banned. */
    BANNED("封号"),
    /** Logged in on another device. */
    ANOTHER_DEVICE("另一个设备登录"),
    /** Server maintenance. */
    MAINTENANCE("系统维护中"),
    /** Forced offline (cool-down). */
    FORCED_OFFLINE("被强制下线"),
    /** Failed to enter the main base home screen. */
    ENTER_HOME_FAILED("进入主页失败"),
    /** Failed to identify the Town Hall level. */
    IDENTIFY_TH_FAILED("判断大本失败"),
    /** Stopped the whole script after repeatedly failing to recognize the current page. */
    STOPPED("已停止（重复未识别）")
}

/**
 * Holds the live scene / run-control / flow-node so any part of the script can both
 * read "where am I" and the user can always see the current node.
 */
object SceneState {
    @Volatile
    var currentScene: GameScene = GameScene.UNKNOWN

    @Volatile
    var currentRunControl: GameRunControl = GameRunControl.RUNNING

    /** Free-form label for the current business step, e.g. "练兵-圣水兵". */
    @Volatile
    var currentFlowNode: String = "未开始"

    fun setScene(scene: GameScene) {
        if (scene != currentScene) {
            currentScene = scene
            ShowMessage("当前页面：${scene.displayName}")
        }
    }

    fun setRunControl(control: GameRunControl) {
        if (control != currentRunControl) {
            currentRunControl = control
            ShowMessage("运行状态：${control.displayName}")
        }
    }

    fun setFlowNode(node: String) {
        currentFlowNode = node
        ShowMessage("流程节点：$node")
    }

    /** One-line status used for HUD / logging. */
    fun snapshot(): String =
        "页面=${currentScene.displayName} | 节点=${currentFlowNode} | 状态=${currentRunControl.displayName}"
}

/**
 * Which village the camera is currently on, decided ONLY by village-specific markers so that the
 * answer stays correct on every "village-like" screen (主世界 / 夜世界 / 都城).
 *
 * Mirrors the legacy freescript 是否主村庄界面() / 是否夜村庄界面() predicates (awcocx_main.lua
 * L1548-1561), which likewise ask "which village is this" before doing anything else.
 *
 * Why the 训练部队 button cannot be used here (verified against real 1280x720 screenshots):
 *   `MyColors.TrainTroops` matches in the MAIN village, the NIGHT village AND the clan capital
 *   alike (it is only the common element of a village HUD), so it can never answer "which village".
 *   That is exactly why the night village used to be reported as 主村庄.
 * The discriminative markers are the builder-icon rows at the top of the screen:
 *   主世界 -> MainBaseWorker / MainBaseWorker2 / MainBaseWorker3 / GoblinWorker / GoblinResearcher
 *   夜世界 -> BuilderBaseWorker / BuilderBaseWorker2
 * They were verified to be mutually exclusive (the main-village icons miss on night-village and
 * capital screenshots, and the builder-base icons miss on main-village screenshots).
 */
enum class Village(val displayName: String) {
    /** Main village / home village (主世界). */
    MAIN("主世界"),

    /** Builder base / night village (夜世界). */
    NIGHT("夜世界"),

    /** Neither village marker is visible (menu / battle / capital / popup / ...). */
    UNKNOWN("未知村庄")
}

/**
 * Detects which village the camera is on using village-specific builder markers only, so it is
 * safe to call on any screen (training page, battle, capital, ...) — it simply returns
 * [Village.UNKNOWN] there.
 *
 * @param byteBuffer optional pre-captured screen; a fresh capture is taken when null.
 */
suspend fun detectVillage(byteBuffer: ScreenCaptureManager.CaptureResult? = null): Village {
    val screen = byteBuffer
        ?: ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return Village.UNKNOWN

    // 1. Night village (夜世界): the master-builder icon row only exists in the builder base.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseWorker, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseWorker2, increment = 1) != null
    ) {
        return Village.NIGHT
    }

    // 2. Main village (主世界): the home-village builder icons (incl. the event goblin
    //    builder / researcher) only appear on the home village.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.MainBaseWorker, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.MainBaseWorker2, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.MainBaseWorker3, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.GoblinWorker, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.GoblinResearcher, increment = 1) != null
    ) {
        return Village.MAIN
    }

    return Village.UNKNOWN
}

/**
 * Detects the primary screen from the current framebuffer using a prioritized list of
 * feature schemas. Mirrors the legacy `函数275a` dispatch + 是否主村庄界面/是否夜村庄界面 predicates.
 *
 * The village question is asked FIRST, through the village-specific builder markers (see
 * [detectVillage]), because the 训练部队 button that used to be the main-village marker also
 * exists in the night village and in the clan capital — using it first made the night village be
 * reported as 主村庄 (and `waitForScene(MAIN_VILLAGE)` succeed while standing in the night village).
 *
 * Then the known non-village pages are classified (练兵页 / 战斗页), and only afterwards — when the
 * page is still undecided — are dialogs closed and the village re-detected, because a popup can
 * hide the builder-icon row. The 练兵页 must be classified BEFORE that step: its own top-right X
 * matches `MyColors.RedX`, so a "close dialogs" pass on that page would close the page itself.
 *
 * As a last resort, an overlay that [sweepBlockingPopups] cannot recognize (e.g. the 选择英雄
 * dialog) is dismissed with BACK ([dismissUnknownOverlayWithBack]).
 *
 * @param byteBuffer optional pre-captured screen; a fresh capture is taken when null.
 * @return the detected [GameScene].
 */
suspend fun detectCurrentScene(byteBuffer: ScreenCaptureManager.CaptureResult? = null): GameScene {
    var scene = detectCurrentSceneInternal(byteBuffer)

    // 1. 载入中：持续等待，最长 [LOADING_MAX_WAIT_MS]（常规约 15 秒即完成；版本更新时可能很久）。
    //    只有超过上限仍停在载入页，才当作"卡死"转入未识别处理。
    if (scene == GameScene.LOADING) {
        val now = System.currentTimeMillis()
        if (loadingSince == 0L) loadingSince = now
        val waited = now - loadingSince
        if (waited < LOADING_MAX_WAIT_MS) {
            if (waited in LOADING_MIN_WAIT_MS until LOADING_MIN_WAIT_MS + 1_001) {
                ShowMessage("载入时间较长（已等待 ${waited / 1000} 秒），继续等待载入完成…")
            }
            return GameScene.LOADING
        }
        loadingSince = 0L
        scene = GameScene.UNKNOWN
    } else {
        loadingSince = 0L
    }

    // 2. 其他未识别：先等 [UNKNOWN_RETRY_DELAY_MS] 重新检测，共重复 [UNKNOWN_RETRY_TIMES] 次；
    //    只有重试后仍识别不出，才真正"记入未识别"。
    var attempt = 0
    while (scene == GameScene.UNKNOWN && attempt < UNKNOWN_RETRY_TIMES) {
        attempt++
        delay(UNKNOWN_RETRY_DELAY_MS)
        scene = detectCurrentSceneInternal(null)
    }

    if (scene != GameScene.UNKNOWN) return scene

    // 3. 重试后仍未识别 → 记入未识别（截图 + 滑动窗口计数），并对认不出的弹窗按返回兜底。
    noteUnrecognizedPage()
    dismissUnknownOverlayWithBack()
    return GameScene.UNKNOWN
}

private suspend fun detectCurrentSceneInternal(byteBuffer: ScreenCaptureManager.CaptureResult? = null): GameScene {
    var screen = byteBuffer
        ?: ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return GameScene.UNKNOWN

    // 1. Village screen: decide 主世界 / 夜世界 by village-specific markers only.
    when (detectVillage(screen)) {
        Village.NIGHT -> return GameScene.BUILDER_BASE
        Village.MAIN -> return GameScene.MAIN_VILLAGE
        Village.UNKNOWN -> Unit
    }

    // 2. Training page (练兵页 / 我的军队): left bar or the in-training 进攻 button.
    if (isTrainingPageOnScreen(screen)) return GameScene.TRAINING_PAGE

    // 3. Battle: the full 进攻！ launch button (distinct from the training-page 进攻 button).
    if (findMultiColors(byteBuffer = screen, schema = MyColors.AttackButton, increment = 1) != null) {
        return GameScene.BATTLE
    }

    // 3.2 已开战的战斗画面：主世界"结束战斗/放弃"、夜世界"退出对战"、搜索中的"取消搜索"。
    //     截图证据：主世界"进攻中"的多张只命中 EndBattle，被旧逻辑判为未知页面并会误按返回键。
    if (isInBattle(screen)) return GameScene.BATTLE

    // 3.5 Clan capital (都城): the bottom-left 回营 / 都城 entry button. It is asked only AFTER the
    //     village / training / battle questions, because the very same button also exists in a normal
    //     village (there the village markers above already answered, so it is never ambiguous here).
    // TODO(待采集)：ClanCapitalEntry 在本部落都城页实测不命中（主色命中但偏移点失配），
    // 识别都城需要按 A 页重新采集特征；此处先保留，等新特征到位后替换。
    if (findMultiColors(byteBuffer = screen, schema = MyColors.ClanCapitalEntry, increment = 1) != null) {
        return GameScene.CLAN_CAPITAL
    }

    // 3.6 夜世界布局"编辑模式"：右侧一列亮绿色按钮。它属于夜世界场景，
    //     旧逻辑识别不出会当未知页面（并在游戏里按返回），这里直接归为夜世界。
    if (findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseEditMode, increment = 1) != null) {
        return GameScene.BUILDER_BASE
    }

    // 4. Still undecided: a popup covering the builder-icon row is the most likely cause, and it
    //    would also let the ambiguous 训练部队 button mislabel a night village as 主村庄. The legacy
    //    script closes dialogs (函数58a/47a) BEFORE asking 是否主村庄界面 / 是否夜村庄界面, so do the
    //    same: close every dismissible popup (a no-op when there is none) and ask again.
    if (sweepBlockingPopups()) {
        (ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult)?.let {
            screen = it
            when (detectVillage(it)) {
                Village.NIGHT -> return GameScene.BUILDER_BASE
                Village.MAIN -> return GameScene.MAIN_VILLAGE
                Village.UNKNOWN -> Unit
            }
        }
    }

    // 5. Last-resort village HUD fallback: the bottom-left 训练部队 button only tells us "this is
    //    some village HUD" (it also exists in the night village / clan capital), so it is checked
    //    only after the village-specific markers above were ruled out — e.g. when the builder row
    //    is hidden by a panel. 都城 intentionally falls through to here.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.TrainTroops, increment = 1) != null) {
        return GameScene.MAIN_VILLAGE
    }

    // 5.5 游戏启动/载入的黑屏合规告示页（"健康游戏忠告" + SUPERCELL logo）：
    //     这是"还没有任何游戏 UI"的合法中间态，识别为 LOADING 并让调用方继续等待。
    //     这一步必须在第 6 步（按返回兜底）之前，否则在载入页按返回会弹出游戏的退出确认框。
    //     注：搜索对手的"乌云"过渡界面颜色与夜世界夜空过近（实测互相误命中），不做特征识别，
    //         由第 6 步的"未识别先等待"兜底覆盖即可。
    if (findMultiColors(byteBuffer = screen, schema = MyColors.GameLoadingNotice, increment = 1) != null) {
        return GameScene.LOADING
    }

    // 6.5 模板兜底（T02 接线）：以上特征都认不出时，尝试已登记的 NCC 模板规则
    //     （模板匹配对"颜色会变/被遮挡"的按钮更稳；规则默认留空，不产生额外开销）。
    for (rule in TEMPLATE_RULES) {
        if (TemplateMatcher.find(rule.name, rule.x1, rule.y1, rule.x2, rule.y2, rule.threshold) != null) {
            return rule.scene
        }
    }

    // 6. 未识别：这里只返回 UNKNOWN。"先等 1.5 秒重试 2 次 → 仍未知才记入未识别（截图/计数）
    //    并按返回兜底"统一交给外层 [detectCurrentScene] 处理，避免在过渡/动画帧上就截图或按返回。
    return GameScene.UNKNOWN
}

/**
 * True when the 训练部队页面 (练兵页 / 我的军队) is on screen — the left bar or the in-training
 * 进攻 button. Shared by [detectCurrentScene] and [sweepBlockingPopups]: the page has its own
 * top-right X that also matches `MyColors.RedX`, so it must never be treated as a dialog to close.
 */
private suspend fun isTrainingPageOnScreen(screen: ScreenCaptureManager.CaptureResult): Boolean =
    findMultiColors(byteBuffer = screen, schema = MyColors.TrainingPage, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage2, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage3, increment = 1) != null

/**
 * True when a battle is in progress (主世界放弃按钮 / 夜世界退出对战 / 取消搜索), used to keep the
 * BACK fallback of [detectCurrentScene] from interfering with a fight.
 */
private suspend fun isInBattle(screen: ScreenCaptureManager.CaptureResult): Boolean =
    findMultiColors(byteBuffer = screen, schema = MyColors.EndBattle, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.GiveUpButton, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.ExitBattleButton, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.CancelAttackSearch, increment = 1) != null

/**
 * NCC 模板兜底规则（task.md T02 接线）：[name] 对应 `assets/templates/<name>.png`，
 * 由 `tools/make_template.py` 裁剪生成。命中 [x1,y1,x2,y2] 区域内的模板即认为处于 [scene]。
 *
 * 默认**留空**（避免每帧产生 NCC 开销）；需要时按下面示例登记：
 * ```
 * private val TEMPLATE_RULES = listOf(
 *     TemplateRule("btn_bb_edit", GameScene.BUILDER_BASE, 1050, 38, 1245, 78),
 * )
 * ```
 */
private data class TemplateRule(
    val name: String,
    val scene: GameScene,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    // 区域要**贴紧按钮所在的小范围**：NCC 成本 ≈ 窗口数 × 模板像素，区域越大越慢。
    // 阈值 0.75 是实测值（精确模板正样本 score≈1.0，能压掉 0.7~0.72 的近似误命中）。
    val threshold: Double = 0.75
)

private val TEMPLATE_RULES = listOf<TemplateRule>()

/** Timestamp of the last "unknown overlay" BACK press, so the fallback cannot cascade. */
private var lastUnknownOverlayBackAt = 0L
private const val UNKNOWN_OVERLAY_BACK_INTERVAL_MS = 8_000L

/** 首次识别到"载入中"的时间戳；载入结束或超过等待上限后清零。 */
private var loadingSince = 0L

/**
 * 载入中（LOADING）的等待区间：常规约 15 秒即可完成载入，
 * 但游戏版本更新/资源下载时可能很久，因此允许一直等到 [LOADING_MAX_WAIT_MS]（90 秒）；
 * 只有超过该上限仍停在载入页，才当作"卡死"转入未识别处理。
 */
private const val LOADING_MIN_WAIT_MS = 15_000L
private const val LOADING_MAX_WAIT_MS = 90_000L

/**
 * 非载入类的"未识别"处理：出现时先等 [UNKNOWN_RETRY_DELAY_MS] 重新检测，共重复
 * [UNKNOWN_RETRY_TIMES] 次；只有重试后仍识别不出，才真正记入未识别（截图 + 计数 + 按返回兜底）。
 */
private const val UNKNOWN_RETRY_TIMES = 2
private const val UNKNOWN_RETRY_DELAY_MS = 1_500L

/**
 * Last-resort recovery for an overlay that [sweepBlockingPopups] cannot recognize (no red-X and no
 * common dialog) — e.g. the 选择英雄 dialog that deadlocked the training flow (see 11.17): press
 * BACK to dismiss it.
 *
 * Guards:
 *  - skipped while a battle is in progress ([isInBattle]), because BACK there interferes with the
 *    fight;
 *  - rate-limited to once per [UNKNOWN_OVERLAY_BACK_INTERVAL_MS], so a page the script simply does
 *    not understand cannot be "BACK-ed" through in a cascade.
 *
 * @return true when BACK was actually sent (the caller should let the next tick re-detect).
 */
private suspend fun dismissUnknownOverlayWithBack(): Boolean {
    val screen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return false
    if (isInBattle(screen)) return false

    // 未识别已由 [detectCurrentScene] 做过"等 1.5 秒 ×2 重试"，这里直接按返回兜底
    //（仅针对真正认不出的弹窗；战斗中不按，且做时间限流防止级联）。
    val now = System.currentTimeMillis()
    if (now - lastUnknownOverlayBackAt < UNKNOWN_OVERLAY_BACK_INTERVAL_MS) return false
    lastUnknownOverlayBackAt = now
    ShowMessage("当前页面无法识别，按返回键尝试关闭未知弹窗")
    // root shell keyevent; 失败时静默忽略（与 SearchOpponents.pressBack 一致）
    runCatching { Shell.cmd("input keyevent 4").exec() }
    return true
}

/**
 * Sliding-window guard for repeated "unrecognized page" episodes. The legacy freescript would
 * loop forever (or restart) on a page it could not parse; here we STOP the whole script once
 * [MAX_UNKNOWN_IN_WINDOW] such episodes occur within [UNKNOWN_WINDOW_MS], because blindly
 * retrying a state we cannot parse usually means a layout/schema drift that needs human attention
 * (and the snapshot is already saved by [captureDebugSnapshot] for later inspection).
 *
 * @return true if the script has been stopped (caller may also rely on [GameRunControl.STOPPED]).
 */
private const val MAX_UNKNOWN_IN_WINDOW = 10
private const val UNKNOWN_WINDOW_MS = 5 * 60_000L
private val unknownEventTimes = mutableListOf<Long>()

suspend fun noteUnrecognizedPage(): Boolean {
    val now = System.currentTimeMillis()
    // 1. Always snapshot this episode (transition-guarded by the caller, so once per episode).
    captureDebugSnapshot("无法识别当前页面（所有已知场景特征均未命中）")
    // 2. Record the event and drop anything outside the sliding window.
    unknownEventTimes.add(now)
    unknownEventTimes.removeAll { now - it > UNKNOWN_WINDOW_MS }
    // 3. If the threshold is exceeded, stop the script instead of looping on an unknown state.
    return if (unknownEventTimes.size >= MAX_UNKNOWN_IN_WINDOW) {
        SceneState.setRunControl(GameRunControl.STOPPED)
        ShowMessage("短时间内多次（${unknownEventTimes.size}次/${(UNKNOWN_WINDOW_MS / 60_000)}分钟内）无法识别当前页面，已停止脚本")
        currentCoroutineContext()[Job]?.cancel()
        true
    } else {
        false
    }
}

/**
 * Captures the current screen to a private debug file and records the reason, so an
 * "unrecognized" state (UNKNOWN scene / unexpected popup / fatal dialog with no schema) can be
 * inspected later and turned into a new schema. Mirrors the legacy freescript "异常截图留痕" idea.
 *
 * The PNG is written under the app's private storage (filesDir/debug_snapshots), which does NOT
 * require the WebSocket file route (only non-private paths do). The reason is also logged via
 * [LogHelper] so it lands in the on-device log file alongside the screenshot path.
 *
 * @param reason human-readable description of what could not be recognized.
 */
suspend fun captureDebugSnapshot(reason: String) {
    val ctx = ScreenCaptureManager.getContext() ?: run {
        ShowMessage("无法获取上下文，跳过异常截图：$reason")
        return
    }
    val bitmap = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap
    if (bitmap == null) {
        ShowMessage("异常截图失败（截屏为空）：$reason")
        return
    }
    val dir = File(ctx.filesDir, "debug_snapshots")
    if (!dir.exists()) dir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(dir, "unrecognized_$stamp.png")
    try {
        FileOutputStream(file).use { fos -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        val info = "异常截图已保存：${file.absolutePath}\n原因：$reason\n账号：${InGamesVars.currentAccountNumber}\n${SceneState.snapshot()}"
        LogHelper.showDebugInfo(info)
        ShowMessage("无法识别当前页面，已截图留档：\n$reason")
    } catch (e: Exception) {
        ShowMessage("异常截图保存失败：${e.message}")
    } finally {
        bitmap.recycle()
    }
}

/**
 * Manual "grab current game screen" entry (the C-step of schema capture): on click it brings the
 * game back to the foreground, then screenshots and saves to the same private [debug_snapshots]
 * folder as [captureDebugSnapshot] but with a `manual_` prefix so deliberate grabs stay separate
 * from auto-captured anomalies. Used to collect dialog/popup screenshots for building new schemas.
 *
 * @param note optional human note (e.g. which dialog this screenshot shows).
 */
suspend fun captureManualSnapshot(note: String = "") {
    // 1. Bring the game to the foreground so we screenshot the game, not the script UI.
    if (!isGameAtFront()) runGame()
    delay(500)
    // 2. Capture + save (same private storage path as captureDebugSnapshot).
    val ctx = ScreenCaptureManager.getContext() ?: run {
        ShowMessage("无法获取上下文，跳过截图")
        return
    }
    val bitmap = ScreenCaptureManager.capture(asBitmap = true) as? Bitmap
    if (bitmap == null) {
        ShowMessage("截图失败（截屏为空）")
        return
    }
    val dir = File(ctx.filesDir, "debug_snapshots")
    if (!dir.exists()) dir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val file = File(dir, "manual_$stamp.png")
    try {
        FileOutputStream(file).use { fos -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        val info = "手动截图已保存：${file.absolutePath}${if (note.isNotEmpty()) "\n备注：$note" else ""}"
        LogHelper.showDebugInfo(info)
        ShowMessage("已抓取游戏画面到debug目录：\n${file.name}${if (note.isNotEmpty()) "\n$note" else ""}")
    } catch (e: Exception) {
        ShowMessage("截图保存失败：${e.message}")
    } finally {
        bitmap.recycle()
    }
}

/**
 * Returns true when a dismissible popup/dialog is covering the screen (generic red-X close
 * button or the common wood-panel dialog). Ported from the legacy 函数47a/58a "close dialog" idea.
 */
suspend fun hasBlockingPopup(byteBuffer: ScreenCaptureManager.CaptureResult? = null): Boolean {
    val screen = byteBuffer
        ?: ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return false
    return findMultiColors(byteBuffer = screen, schema = MyColors.RedX, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.CommonDialog, increment = 1) != null
}

/**
 * Closes any dismissible popup found on screen. Mirrors the generic white-X / red-X cleanup
 * (函数58a) of the legacy script: it only taps the well-known close buttons, never the
 * confirm/action buttons, so it is safe to call from any wait-loop.
 *
 * It never closes the 训练部队页面 itself: that page's top-right X also matches `MyColors.RedX`,
 * but tapping it means "leave the training page", which callers such as `waitForScene(TRAINING_PAGE)`
 * would then never reach. See [isTrainingPageOnScreen].
 *
 * @return true if a popup was closed.
 */
suspend fun sweepBlockingPopups(): Boolean {
    val screen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return false
    // 掉线弹窗（"还在吗？因为太久没有进行操作，您已断开连接。"）：它既没有红 x 也不是通用对话框，
    // 会让等待循环一直转下去。命中"重新载入游戏"按钮文字就点它（特征来自真实截图）。
    findMultiColors(byteBuffer = screen, schema = MyColors.ReloadGameButton, increment = 1)?.let {
        TouchActions.tap(it.x + 14, it.y + 16, delayTime = 500)
        ShowMessage("检测到掉线弹窗，已点击「重新载入游戏」")
        return true
    }
    // Generic red-X close button (top-right round button).
    // 注意：训练部队页面右上角也有一个"红叉"（点它是关闭训练页），它同样会被 RedX 特征命中
    // （实机命中 (1217,65)）。在这里点它就会把训练页关掉 —— 例如 waitForScene(TRAINING_PAGE)
    // 的每一轮都先清弹窗，训练页会被立刻关掉、永远等不到。所以在训练页上跳过这一分支。
    if (!isTrainingPageOnScreen(screen)) {
        findMultiColors(byteBuffer = screen, schema = MyColors.RedX, increment = 1)?.let {
            TouchActions.tap(it.x, it.y, delayTime = 500)
            ShowMessage("已关闭通用弹窗（红x）")
            return true
        }
    }
    // Common wood-panel dialog: tap its top-right close region (the dialog gold edge starts ~x=150).
    findMultiColors(byteBuffer = screen, schema = MyColors.CommonDialog, increment = 1)?.let {
        TouchActions.tap(it.x + 960, it.y + 30, delayTime = 500)
        ShowMessage("已关闭通用对话框")
        return true
    }
    return false
}

/**
 * Unified "wait until a target screen appears" loop, ported from the legacy freescript
 * `函数275a` wait skeleton (while countdown { 清弹窗 -> 取屏 -> 判目标 -> 扣时 -> 超时置状态 }).
 *
 * Every iteration first sweeps blocking popups, then re-detects the current scene and logs it,
 * so the user always sees which page the script is sitting on instead of guessing. On timeout
 * it records [GameRunControl.INTERFACE_TIMEOUT] rather than failing silently.
 *
 * @param target the scene to wait for.
 * @param timeoutSeconds max seconds to wait before giving up.
 * @param onTick called once per iteration with the currently detected scene (for HUD/log).
 * @return true if [target] was reached within the timeout.
 */
suspend fun waitForScene(
    target: GameScene,
    timeoutSeconds: Int = 30,
    onTick: (GameScene) -> Unit = {},
): Boolean {
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    while (System.currentTimeMillis() < deadline) {
        // 1. Dismiss any blocking popup first (函数58a idea).
        sweepBlockingPopups()
        // 2. Observe the current page and make it visible to the user.
        val scene = detectCurrentScene()
        SceneState.setScene(scene)
        onTick(scene)
        if (scene == target) return true
        // 3. Tick once per second so we do not burn CPU.
        delay(1000)
    }
    // 4. Time out: leave an explicit trail instead of killing the game silently.
    SceneState.setRunControl(GameRunControl.INTERFACE_TIMEOUT)
    ShowMessage("等待页面【${target.displayName}】超时（${timeoutSeconds}s）")
    captureDebugSnapshot("等待页面【${target.displayName}】超时，当前仍停在【${SceneState.currentScene.displayName}】")
    return false
}

/**
 * Recovery entry ported from the legacy freescript "回到主村庄" idea: if we are not on the
 * home base, try to reach it by first dismissing popups and waiting, then falling back to the
 * full [enterMainScreen] re-sync. Returns true once on the home base.
 */
suspend fun recoverToMainScreen(timeoutSeconds: Int = 30): Boolean {
    if (waitForScene(GameScene.MAIN_VILLAGE, timeoutSeconds)) return true
    SceneState.setRunControl(GameRunControl.ENTER_HOME_FAILED)
    return enterMainScreen()
}

/**
 * Detects a fatal run-control state from the current screen. This mirrors the legacy 函数45a
 * image+UI recognition for 网络异常/系统维护中/封号/顶号/另一个设备登录.
 *
 * NOTE: the project has no per-frame game UIXML keyword reader (the accessibility service only
 * clicks system dialogs), so detection here is image-based and limited to schemas that exist.
 * [MyColors.Reconnection] is covered via [com.coc.zkqcode.jar.code.universal.smalltools.checkReconnections].
 * The remaining fatal dialogs (封号 / 维护 / 网络异常 / 顶号 / 另一个设备登录) MUST be added here
 * as schemas when their screenshots are captured; the mapping below is the extension point.
 *
 * @return the detected control state (defaults to [GameRunControl.RUNNING]).
 */
suspend fun detectGameRunControl(): GameRunControl {
    // Extension point: pair a fatal-dialog schema with its control state, e.g.
    //   MyColors.BannedDialog to GameRunControl.BANNED,
    //   MyColors.MaintenanceDialog to GameRunControl.MAINTENANCE,
    //   MyColors.AnotherDeviceDialog to GameRunControl.ANOTHER_DEVICE,
    // Once those schemas exist, append them to this list and the loop will set the state.
    val fatalDialogs: List<Pair<ColorSchema, GameRunControl>> = emptyList()
    for ((schema, control) in fatalDialogs) {
        if (findMultiColors(schema = schema, increment = 1) != null) {
            SceneState.setRunControl(control)
            captureDebugSnapshot("识别到异常状态：${control.displayName}（对应弹窗待纳入处理）")
            return control
        }
    }
    return GameRunControl.RUNNING
}

/**
 * Unified recovery dispatcher ported from the legacy freescript `游戏运行控制` loop: reads
 * [currentRunControl] and decides how to proceed BEFORE each main-loop iteration, so the state
 * machine actually drives behavior instead of only recording a label.
 *
 * Called at the top of `runMainScript`'s loop. Transient/auto-recovered states (timeout, home
 * failed, account switch, restart, network) are cleared back to RUNNING because the action that
 * set them already ran; fatal states needing human attention (封号 / 维护 / 顶号 / 强制下线 /
 * 判断大本失败) stop the script; STOPPED (repeated unknown) also stops.
 *
 * @return true to continue the loop, false to break out (caller should return from runMainScript).
 */
suspend fun handleRunControl(): Boolean {
    // Proactive connectivity probe: if the link dropped (and we are not already in a fatal state),
    // raise NETWORK_ERROR so the user sees it and we wait for recovery instead of failing obscurely.
    if (SceneState.currentRunControl == GameRunControl.RUNNING && !isNetworkConnected()) {
        SceneState.setRunControl(GameRunControl.NETWORK_ERROR)
    }
    // 游戏卡死兜底：画面中央橙色转圈 12 秒无变化 = 网络异常 → 重启游戏。
    // 放在主循环每轮调用处，覆盖所有日常流程（等待循环则由 checkReconnections 负责）。
    if (SceneState.currentRunControl == GameRunControl.RUNNING && detectStuckNetworkSpinner()) {
        SceneState.setRunControl(GameRunControl.RESTART_GAME)
        return true
    }
    return when (val control = SceneState.currentRunControl) {
        GameRunControl.RUNNING -> true

        // Stop the whole script (already cancelled by noteUnrecognizedPage / fatal path below).
        GameRunControl.STOPPED -> {
            ShowMessage("脚本已停止：短时间内多次无法识别当前页面")
            currentCoroutineContext()[Job]?.cancel()
            false
        }

        // Network dropped: wait for recovery (up to the cap), then clear and continue; if it does
        // not come back, stop rather than loop forever.
        GameRunControl.NETWORK_ERROR -> {
            if (waitForNetwork()) {
                SceneState.setRunControl(GameRunControl.RUNNING)
                true
            } else {
                ShowMessage("网络长时间未恢复，已停止脚本")
                currentCoroutineContext()[Job]?.cancel()
                false
            }
        }

        // Transient / already-actioned states: the companion code (killGame / re-sync /
        // account switch) already ran, so clear and continue the loop.
        GameRunControl.INTERFACE_TIMEOUT,
        GameRunControl.ENTER_HOME_FAILED,
        GameRunControl.SWITCH_ACCOUNT,
        GameRunControl.RESTART_GAME -> {
            SceneState.setRunControl(GameRunControl.RUNNING)
            true
        }

        // Fatal states requiring human attention: do NOT loop blindly, stop the script.
        GameRunControl.BANNED,
        GameRunControl.MAINTENANCE,
        GameRunControl.ANOTHER_DEVICE,
        GameRunControl.FORCED_OFFLINE,
        GameRunControl.IDENTIFY_TH_FAILED -> {
            ShowMessage("遇到需人工处理的异常状态：${control.displayName}，已停止脚本")
            captureDebugSnapshot("异常状态：${control.displayName}（需人工处理，已停止脚本）")
            currentCoroutineContext()[Job]?.cancel()
            false
        }
    }
}

/**
 * True when the device currently has an internet-capable network. Uses the app context held by
 * [ScreenCaptureManager]; if the context is missing we assume connected (never block on it).
 * Backs the [GameRunControl.NETWORK_ERROR] branch of [handleRunControl].
 */
fun isNetworkConnected(): Boolean {
    val ctx = ScreenCaptureManager.getContext() ?: return true
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Blocks until connectivity is restored (or [timeoutSeconds] pass), polling every 3s, so the
 * script waits out a transient network drop instead of failing obscurely. Returns true if the
 * network came back.
 */
suspend fun waitForNetwork(timeoutSeconds: Int = 120): Boolean {
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    while (System.currentTimeMillis() < deadline) {
        if (isNetworkConnected()) return true
        ShowMessage("网络异常，等待恢复中…")
        delay(3000)
    }
    return isNetworkConnected()
}
