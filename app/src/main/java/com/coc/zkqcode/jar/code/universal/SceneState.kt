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
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.smalltools.isGameAtFront
import com.coc.zkqcode.jar.code.universal.smalltools.runGame
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
 * Detects the primary screen from the current framebuffer using a prioritized list of
 * feature schemas. Mirrors the legacy `函数275a` dispatch + 是否主村庄界面/是否夜村庄界面 predicates.
 *
 * @param byteBuffer optional pre-captured screen; a fresh capture is taken when null.
 * @return the detected [GameScene].
 */
suspend fun detectCurrentScene(byteBuffer: ScreenCaptureManager.CaptureResult? = null): GameScene {
    val screen = byteBuffer
        ?: ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        ?: return GameScene.UNKNOWN

    // 1. Main village: the bottom-left 训练部队 button only exists on the home screen.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.TrainTroops, increment = 1) != null) {
        return GameScene.MAIN_VILLAGE
    }

    // 2. Training page: left bar (训练部队页面) or the in-training 进攻 button.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.TrainingPage, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage2, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.AttackInTrainingPage3, increment = 1) != null
    ) {
        return GameScene.TRAINING_PAGE
    }

    // 3. Battle: the full 进攻！ launch button (distinct from the training-page 进攻 button).
    if (findMultiColors(byteBuffer = screen, schema = MyColors.AttackButton, increment = 1) != null) {
        return GameScene.BATTLE
    }

    // 4. Builder base / night village.
    if (findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseWorker, increment = 1) != null ||
        findMultiColors(byteBuffer = screen, schema = MyColors.BuilderBaseWorker2, increment = 1) != null
    ) {
        return GameScene.BUILDER_BASE
    }

    // Phase: if the screen just became unrecognized (was a known scene on the previous tick),
    // take a snapshot and feed the sliding-window guard. If "unrecognized" happens too many times
    // in a short window, the script is stopped (see noteUnrecognizedPage).
    if (SceneState.currentScene != GameScene.UNKNOWN) {
        noteUnrecognizedPage()
    }
    return GameScene.UNKNOWN
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
    findMultiColors(byteBuffer = screen, schema = MyColors.RedX, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 500)
        ShowMessage("已关闭通用弹窗（红x）")
        return true
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
