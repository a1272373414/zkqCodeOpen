package com.coc.zkqcode.jar.code.universal.smalltools

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.jar.code.universal.recognizer.PixelFontChinese

/**
 * T34：中文字库自动采集（挂在 bot 跑图流程里）。
 *
 * 为什么要挂 bot：手动在主界面点「采集字模」时，辅助自己的悬浮窗面板容易入镜，
 * 采到的都是"主页设置 / 保存并运行"这类辅助 UI 文字；而 bot 运行时面板是关的，
 * 截屏拿到的是**纯游戏画面**，采到的才是部落名 / 玩家名 / 任务描述这些真正有用的字。
 *
 * 用法：账号记忆 `AutoHarvestFont<账号>` = "1" 开启，由 [MainScript] 在
 * 进入主界面之后调用 [maybeHarvest]。节流间隔 [HARVEST_INTERVAL_MS]，
 * 累计新增 [SAVE_THRESHOLD] 条才落盘一次，避免频繁写文件。
 *
 * 注意：`harvestFromScreen` 要截屏 + 跑一次 ML Kit（约 2~3 秒），
 * 所以**只能挂在低风险空闲时机**，绝不能放在下兵等时序敏感路径上。
 */
object FontHarvest {

    /** 节流间隔：同一账号多久采集一次（默认 15 分钟）。 */
    private const val HARVEST_INTERVAL_MS = 15 * 60 * 1000L

    /** 累计新增多少条后写回字库文件。 */
    private const val SAVE_THRESHOLD = 40

    /** 距上次落盘以来累计新增的条数（进程内计数）。 */
    @Volatile
    private var pendingSinceLastSave = 0

    /**
     * 条件满足时采集一轮中文字模。
     *
     * @param account 当前账号
     * @return 本轮新增条数；未达条件返回 0
     */
    suspend fun maybeHarvest(account: Int): Int {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.AUTO_HARVEST_FONT, account)) != "1") {
            return 0
        }
        val last = readMemory(StorageKeys.withAccountNumber(StorageKeys.FONT_HARVEST_LAST, account))
            .toLongOrNull() ?: 0L
        val now = System.currentTimeMillis()
        if (now - last < HARVEST_INTERVAL_MS) return 0

        writeMemory(StorageKeys.withAccountNumber(StorageKeys.FONT_HARVEST_LAST, account), now.toString())

        val added = try {
            PixelFontChinese.harvestFromScreen()
        } catch (e: Exception) {
            ShowMessage("中文字库自动采集失败：${e.message}")
            return 0
        }
        if (added <= 0) return 0

        pendingSinceLastSave += added
        if (pendingSinceLastSave >= SAVE_THRESHOLD) {
            if (PixelFontChinese.saveHarvested()) {
                ShowMessage("中文字库自动采集：累计 $pendingSinceLastSave 条已写入")
                pendingSinceLastSave = 0
            }
        }
        return added
    }
}
