package com.coc.zkqcode.jar.code.universal.smalltools

import android.os.Environment
import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.RunShell
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.ui.schema.Schema
import kotlinx.coroutines.delay


suspend fun checkReconnections(): Boolean {
    // 1. Capture the screen and cast safely

    checkPrivacy()

    // 网络异常兜底：中央橙色转圈卡死超过 12 秒 → 直接重启游戏
    if (detectStuckNetworkSpinner()) return true

    // 2. Define the schemas to check against
    // ReloadGameButton 是"还在吗？…您已断开连接。"弹窗里"重新载入游戏"按钮的文字特征，
    // 由真实截图派生，作为该弹窗的直接判据（比仅靠面板底色更可靠）。
    val homeSchemas = listOf(
        MyColors.Reconnection, MyColors.ReconnectionOnCloudPhone, MyColors.RatingOnCloudPhone,
        MyColors.ReloadGameButton
    )
    val screenBuffer = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult ?: return true
    // 3. Run the check and capture the result
    // We return the result of 'any' to determine if a reconnection event occurred.
    return homeSchemas.any { schema ->
        val match = findMultiColors(byteBuffer = screenBuffer, schema = schema)
        if (match != null) {
            // Count white pixels in the confirmation area; skip this iteration if insufficient
            val whiteCount = countWhitePixels(screenBuffer, 270, 230, 1010, 430)
            if (whiteCount <= 100) {
                return@any true // Not enough white pixels, skip this time
            }
            // Retrieve the configuration state for the specific account
            val configKey = Schema.GLOBAL_SETTINGS.AFTER_KICK_OPTION.key
            val action = GlobalVars.configStates[configKey]?.value?.toInt() ?: logAndRestart("Can not get $configKey")
            // 4. Implement logic based on the action value
            when (action) {
                0 -> {
                    // Action: Tap the "Reload" button.
                    // Prefer the precise schema (derived from a real disconnect-popup screenshot);
                    // fall back to the legacy blind taps only when the button text is not found.
                    val reload = findMultiColors(
                        byteBuffer = screenBuffer, schema = MyColors.ReloadGameButton, increment = 1
                    )
                    if (reload != null) {
                        TouchActions.tap(reload.x + 14, reload.y + 16)
                    } else {
                        TouchActions.tap(379, 458)
                        TouchActions.tap(793, 459)//Rating Notification
                        TouchActions.tap(338, 511)//Tutorial "Confirm" button
                    }
                }

                1 -> {
                    // Action: Signal a need to switch accounts
                    // By returning false here, we tell the caller the check 'failed' or needs a different flow
                    return false
                }

                2 -> {
                    // Action: Wait/Idle
                    delay(1000)
                }
            }
            true // Match found and handled
        } else {
            true// No match for this schema
        }
    }


}

/**
 * Counts pixels in the given region where R, G, B are all greater than 235 (white).
 * Uses absolute ByteBuffer.get(index) so it is position-independent.
 */
// 橙色转圈只出现在屏幕正中央，采样区收窄到中央一带：原来的 (400,250)-(880,500) 覆盖太大，
// 会把各种弹窗/界面里的橙色装饰一并算进来，造成"静止界面 = 网络卡死"的误判。
private const val SPINNER_X1 = 480
private const val SPINNER_Y1 = 280
private const val SPINNER_X2 = 800
private const val SPINNER_Y2 = 440
private const val SPINNER_MIN_ORANGE = 300
private const val SPINNER_STUCK_MS = 12_000L
private const val SPINNER_SAMPLE_MS = 2_000L

/**
 * 画面中央的橙色转圈 = 网络异常（连接卡死）。
 * 橙色图标存在，且 12 秒内该区域画面没有任何变化 → 重启游戏。
 *
 * @return true 表示已判定网络异常并重启了游戏。
 */
/**
 * 卡死计时（跨循环累计）：原实现在单次调用里 delay 采样 12 秒，会把主循环整整阻塞 12 秒——
 * 夜世界「开始进攻」确认弹窗就因此白等十几秒，最后还被判成卡死重启游戏
 * （实机日志：23:21:23 进入循环 → 23:21:38 重启，中间什么都没做）。
 * 现在每轮循环只采样一次，用时间戳累计"橙色 + 画面无变化"的持续时长，不再阻塞调用方。
 */
private var spinnerStuckSince = 0L
private var spinnerPrevFrame: ScreenCaptureManager.CaptureResult? = null

private fun resetSpinnerWatch() {
    spinnerStuckSince = 0L
    spinnerPrevFrame = null
}

suspend fun detectStuckNetworkSpinner(): Boolean {
    // 先排除"静止界面"误判：夜世界「开始进攻」部队确认弹窗的中央有大量橙色装饰（部队卡上的
    // 金色 x1 徽章、中部卡片图标），而该界面本身就是静止等待玩家点「立即寻找！」，正好命中
    // 下面的"橙色像素 + 画面无变化"判据 —— 实机已复现：脚本卡在该界面被反复重启游戏。
    if (findMultiColors(schema = MyColors.AttackNow) != null) {
        resetSpinnerWatch()
        return false
    }
    // 同理：搜索对手界面（常驻"取消搜索"按钮）也是静止等待状态，正常流程由等待循环处理
    if (findMultiColors(schema = MyColors.CancelAttackSearch) != null) {
        resetSpinnerWatch()
        return false
    }

    val shot = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult ?: return false
    if (countOrangePixels(shot) < SPINNER_MIN_ORANGE) {
        resetSpinnerWatch()
        return false
    }
    val prev = spinnerPrevFrame
    if (prev != null && frameDiffPixels(prev, shot) > 200) {
        resetSpinnerWatch() // 画面在变 → 正常的转圈动画，不是卡死
        return false
    }
    spinnerPrevFrame = shot
    val now = System.currentTimeMillis()
    if (spinnerStuckSince == 0L) {
        spinnerStuckSince = now // 首次命中，开始计时，本轮不做判定
        return false
    }
    if (now - spinnerStuckSince < SPINNER_STUCK_MS) return false
    resetSpinnerWatch()
    ShowMessage("检测到中央橙色转圈卡死超过12秒（网络异常），重启游戏")
    killGame()
    runGame()
    return true
}
/** 统计中央区域里的橙色像素数（橙色转圈口径：R 高、G 中、B 低）。 */
private fun countOrangePixels(screen: ScreenCaptureManager.CaptureResult): Int {
    var count = 0
    val buf = screen.buffer
    for (y in SPINNER_Y1 until SPINNER_Y2) {
        for (x in SPINNER_X1 until SPINNER_X2) {
            val o = y * screen.rowStride + x * screen.pixelStride
            val r = buf.get(o).toInt() and 0xFF
            val g = buf.get(o + 1).toInt() and 0xFF
            val b = buf.get(o + 2).toInt() and 0xFF
            if (r > 200 && g in 80..185 && b < 120) count++
        }
    }
    return count
}

/** 两帧在中央区域里"变化明显"的像素数，用于判断画面是否卡死。 */
private fun frameDiffPixels(
    a: ScreenCaptureManager.CaptureResult,
    b: ScreenCaptureManager.CaptureResult
): Int {
    var diff = 0
    for (y in SPINNER_Y1 until SPINNER_Y2) {
        for (x in SPINNER_X1 until SPINNER_X2) {
            for (c in 0 until 3) {
                val oa = y * a.rowStride + x * a.pixelStride + c
                val ob = y * b.rowStride + x * b.pixelStride + c
                val va = a.buffer.get(oa).toInt() and 0xFF
                val vb = b.buffer.get(ob).toInt() and 0xFF
                if (va - vb > 30 || vb - va > 30) {
                    diff++
                    break
                }
            }
        }
    }
    return diff
}

private fun countWhitePixels(
    screenBuffer: ScreenCaptureManager.CaptureResult, x1: Int, y1: Int, x2: Int, y2: Int
): Int {
    val buf = screenBuffer.buffer
    val pixelStride = screenBuffer.pixelStride
    val rowStride = screenBuffer.rowStride
    var count = 0
    for (y in y1 until y2) {
        for (x in x1 until x2) {
            val offset = y * rowStride + x * pixelStride
            val r = buf.get(offset).toInt() and 0xFF
            val g = buf.get(offset + 1).toInt() and 0xFF
            val b = buf.get(offset + 2).toInt() and 0xFF
            if (r > 235 && g > 235 && b > 235) count++
        }
    }
    return count
}

private suspend fun checkPrivacy() {
    val point = findMultiColors(schema = MyColors.PrivacyInfo)
    if (point != null) {
        TouchActions.tap(point.x, point.y, delayTime = 200)
        reExtractGameSavings()
    }
}

suspend fun reExtractGameSavings() {
    // 1. Configuration items
    val packageName = "com.supercell.clashofclans"
    val folderName = "zkqGlobalGameSave"
    val targetSubDirs = listOf("shared_prefs")

    // 2. Environment path preparation
    val suffix = InGamesVars.currentAccountNumber
    val sdPath = Environment.getExternalStorageDirectory().path

    val gameRootDir = "$sdPath/zkqFiles/$folderName"
    val targetSaveDir = "$gameRootDir/$suffix"

    // 3. Core modification: if folder exists, delete it directly for re-extraction
    // Use [ -d ] to check if directory exists, if yes execute rm -rf
    val cleanCmd = "[ -d \"$targetSaveDir\" ] && rm -rf \"$targetSaveDir\""
    RunShell.run(cleanCmd)

    // 4. Create directory structure (need to recreate after rm)
    RunShell.run("mkdir -p \"$targetSaveDir\"")

    // 5. Execute data copy
    targetSubDirs.forEach { dir ->
        val destPath = "$targetSaveDir/$dir"
        // Create target subdirectory
        RunShell.run("mkdir -p \"$destPath\"")

        // 使用 Root 权限从 /data/data/ 复制到 SD 卡
        // 注意：这里拷贝的是内容，建议保留目录权限或结构
        val copyCmd = "cp -r /data/data/$packageName/$dir/* \"$destPath\""
        RunShell.run(copyCmd)
    }
}