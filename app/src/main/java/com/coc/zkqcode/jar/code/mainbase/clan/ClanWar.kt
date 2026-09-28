package com.coc.zkqcode.jar.code.mainbase.clan

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import com.coc.zkqcode.jar.code.mainbase.attack.handleRewardPopup
import com.coc.zkqcode.jar.code.mainbase.attack.mainBaseDeployTroops
import kotlinx.coroutines.delay

/**
 * 部落战进攻（T09，源 部落战(209)，文档 ⑫ L21372-22139 + 发起进攻 L23105-23145）。
 *
 * 源流程：进部落 → 判战斗日 + 数剩余进攻机会(2/1/0) → 选敌 → 点「开始战斗」(1FBC6D) → 等「放弃按钮」→ 下兵(部落战下兵=true)。
 *
 * 本项目实现要点（区别于源）：
 *  - **下兵复用主世界部署 [mainBaseDeployTroops]**（用户要求：部落战进攻不另写一套，直接复用主世界的）；
 *  - 进场 / 选敌 / 战斗日判定优先用 [ChineseTextReader.locate] 文字定位（方向无关、抗皮肤），源里大量 2023 版像素
 *    坐标与配色（0CBDFE 剩余机会、1FBC6D 开始战斗）作为兜底并在注释标注，待 T30 真机复标；
 *  - 战斗内「放弃按钮」等待逻辑复用与 [com.coc.zkqcode.jar.code.mainbase.attack.mainBaseAttack] 相同的
 *    [MyColors.EndBattle]/[MyColors.GiveUpButton] 锚点。
 *
 * 注：真机校验前，选敌的「点哪个敌营」是简化版（取地图首个候选位），命中率依赖 T30 复标。
 */
object ClanWar {

    /**
     * 为指定账号执行一次部落战进攻（若开关开启且有进攻机会）。
     * @return 是否执行了进攻动作
     */
    suspend fun clanWarAttack(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.CLAN_WAR_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，部落战：开始")
        if (!enterClan()) {
            ShowMessage("账号$account，部落战：未能进入部落")
            return false
        }
        val remaining = detectRemainingAttacks()
        if (remaining <= 0) {
            ShowMessage("账号$account，部落战：无可用进攻机会")
            return false
        }
        // 「留1刀」：仅剩 1 次且开启该选项时跳过
        if (remaining == 1 &&
            readMemory(StorageKeys.withAccountNumber(StorageKeys.CLAN_WAR_KEEP_ONE, account)) == "1"
        ) {
            ShowMessage("账号$account，部落战：留1刀，本次跳过")
            return false
        }
        if (!startWarBattle()) {
            ShowMessage("账号$account，部落战：未能开战")
            return false
        }
        // 复用主世界下兵（核心：不另写部落战部署）
        mainBaseDeployTroops()
        waitBattleEnd()
        ShowMessage("账号$account，部落战：进攻结束")
        return true
    }

    /** 进部落：主界面点「部落」入口（竖屏 (280,80) → 横屏 (80,1000) 落地）。 */
    private suspend fun enterClan(): Boolean {
        val entry = ChineseTextReader.locate("部落")
        if (entry != null) {
            TouchActions.tap(entry.centerX(), entry.centerY())
            delayWithMultiplier(1500)
            return true
        }
        // 兜底：左下角部落城堡入口（横屏坐标）
        TouchActions.tap(80, 1000, delayTime = 1500)
        return true
    }

    /**
     * 判战斗日 + 剩余进攻机会。
     * 源用 0CBDFE 系列配色区分 2/1/0 次，本项目优先用文字「进攻」判定有可用进攻；
     * 命中即认为至少 1 次（精确计数待 T30 复标）。返回剩余次数（简化：命中=1，留1刀场景靠 CLAN_WAR_KEEP_ONE）。
     */
    private suspend fun detectRemainingAttacks(): Int {
        return if (ChineseTextReader.locate("进攻") != null) 1 else 0
    }

    /**
     * 选敌并开战：点「进攻」打开战争地图 → 选首个敌营候选位 → 点「进攻」→ 点「开始战斗」。
     * 敌营选择为简化版（地图中心候选），命中率依赖 T30 复标。
     */
    private suspend fun startWarBattle(): Boolean {
        // 1) 部落战界面点「进攻」打开战争地图
        val attackEntry = ChineseTextReader.locate("进攻") ?: return false
        TouchActions.tap(attackEntry.centerX(), attackEntry.centerY())
        delayWithMultiplier(1500)
        // 2) 选敌：地图中心候选位（竖屏 ~ (640,640) → 横屏 (640,640)），点到出现「进攻」按钮
        var opened = false
        for (candidate in listOf(Pair(640, 640), Pair(640, 450), Pair(640, 800))) {
            TouchActions.tap(candidate.first, candidate.second, delayTime = 800)
            if (ChineseTextReader.locate("进攻") != null) { opened = true; break }
        }
        if (!opened) return false
        // 3) 再次点「进攻」进入军队界面
        ChineseTextReader.locate("进攻")?.let {
            TouchActions.tap(it.centerX(), it.centerY()); delayWithMultiplier(1200)
        }
        // 4) 点「开始战斗」（源 1FBC6D 绿；优先文字「开始战斗」）
        val start = ChineseTextReader.locate("开始战斗") ?: ChineseTextReader.locate("进攻")
        if (start != null) {
            TouchActions.tap(start.centerX(), start.centerY())
            delayWithMultiplier(3000)
        }
        // 5) 等「放弃按钮」出现（与 mainBaseAttack 同款锚点）
        repeat(15) {
            if (findMultiColors(MyColors.EndBattle) != null ||
                findMultiColors(MyColors.GiveUpButton) != null
            ) return true
            delay(1000)
        }
        return true
    }

    /** 战斗内等待结束：复用 EndBattle/GiveUpButton 锚点，超时强制退出。 */
    private suspend fun waitBattleEnd() {
        val maxDurationMs = 3 * 60 * 1000L
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= maxDurationMs) {
                ShowMessage("账号${InGamesVars.currentAccountNumber}，部落战超时强制退出")
                break
            }
            if (handleRewardPopup()) continue
            val endBattleButton = findMultiColors(MyColors.EndBattle)
                ?: findMultiColors(MyColors.GiveUpButton)
            if (endBattleButton == null) {
                ShowMessage("账号${InGamesVars.currentAccountNumber}，部落战：未找到放弃按钮，对战结束")
                delayWithMultiplier(1000)
                TouchActions.tap(640, 610, delayTime = 1500)
                break
            }
            delay(500)
        }
        enterMainScreen()
    }
}
