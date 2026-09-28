package com.coc.zkqcode.jar.code.mainbase.league

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
 * 联赛(CWL)进攻（T10，源 联赛(67)，文档 ⑫ 函数183a·打联赛战 L21543-21593 + 函数331a·联赛选敌 L22899-22916）。
 *
 * 与部落战(T09)高度同构：进部落 → 切「联赛」标签 → 判有无进攻机会 → 选敌（源用「联赛找对位」+ OCR 我的名次做对位对齐）→
 * 开战 → **下兵复用主世界 [mainBaseDeployTroops]** → 等放弃按钮。
 *
 * 本项目实现要点：
 *  - 下兵复用主世界部署（与 T09 一致，不另写一套）；
 *  - 选敌简化为「联赛标签下首个可进攻敌营」（源「对位」精确匹配：OCR 我的名次 → 上下翻动对齐，待 T30 真机复标）；
 *  - 进场 / 选敌优先文字 [ChineseTextReader.locate]（方向无关、抗皮肤），源 2023 像素坐标作兜底并标注待 T30 复标。
 */
object League {

    /**
     * 为指定账号执行一次联赛进攻（若开关开启且有进攻机会）。
     * @return 是否执行了进攻动作
     */
    suspend fun leagueAttack(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.LEAGUE_WAR_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，联赛：开始")
        if (!enterClan()) {
            ShowMessage("账号$account，联赛：未能进入部落")
            return false
        }
        if (!enterLeagueTab()) {
            ShowMessage("账号$account，联赛：未找到联赛标签")
            return false
        }
        if (detectAttackAvailable()) {
            ShowMessage("账号$account，联赛：无可用进攻机会")
            return false
        }
        if (!startWarBattle()) {
            ShowMessage("账号$account，联赛：未能开战")
            return false
        }
        // 复用主世界下兵（核心：与 T09 一致）
        mainBaseDeployTroops()
        waitBattleEnd()
        ShowMessage("账号$account，联赛：进攻结束")
        return true
    }

    /** 进部落：主界面点「部落」入口（竖屏 (280,80) → 横屏 (80,1000)）。 */
    private suspend fun enterClan(): Boolean {
        val entry = ChineseTextReader.locate("部落")
        if (entry != null) {
            TouchActions.tap(entry.centerX(), entry.centerY())
            delayWithMultiplier(1500)
            return true
        }
        TouchActions.tap(80, 1000, delayTime = 1500)
        return true
    }

    /** 切到「联赛」标签（源 函数331a·进部落战 判是否联赛战，再切标签）。 */
    private suspend fun enterLeagueTab(): Boolean {
        val tab = ChineseTextReader.locate("联赛") ?: ChineseTextReader.locate("部落对战联赛")
        if (tab != null) {
            TouchActions.tap(tab.centerX(), tab.centerY())
            delayWithMultiplier(1500)
            return true
        }
        return false
    }

    /**
     * 判有无进攻机会：源用底部战斗图标判断（函数183a·打联赛战）。
     * 本项目优先用文字「进攻」判定。返回 true 表示【无机会】。
     */
    private suspend fun detectAttackAvailable(): Boolean {
        return ChineseTextReader.locate("进攻") == null
    }

    /** 选敌并开战：点「进攻」打开地图 → 选首个敌营候选位 → 点「进攻」→ 点「开始战斗」。 */
    private suspend fun startWarBattle(): Boolean {
        val attackEntry = ChineseTextReader.locate("进攻") ?: return false
        TouchActions.tap(attackEntry.centerX(), attackEntry.centerY())
        delayWithMultiplier(1500)
        var opened = false
        for (candidate in listOf(Pair(640, 640), Pair(640, 450), Pair(640, 800))) {
            TouchActions.tap(candidate.first, candidate.second, delayTime = 800)
            if (ChineseTextReader.locate("进攻") != null) { opened = true; break }
        }
        if (!opened) return false
        ChineseTextReader.locate("进攻")?.let {
            TouchActions.tap(it.centerX(), it.centerY()); delayWithMultiplier(1200)
        }
        val start = ChineseTextReader.locate("开始战斗") ?: ChineseTextReader.locate("进攻")
        if (start != null) {
            TouchActions.tap(start.centerX(), start.centerY())
            delayWithMultiplier(3000)
        }
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
                ShowMessage("账号${InGamesVars.currentAccountNumber}，联赛超时强制退出")
                break
            }
            if (handleRewardPopup()) continue
            val endBattleButton = findMultiColors(MyColors.EndBattle)
                ?: findMultiColors(MyColors.GiveUpButton)
            if (endBattleButton == null) {
                ShowMessage("账号${InGamesVars.currentAccountNumber}，联赛：未找到放弃按钮，对战结束")
                delayWithMultiplier(1000)
                TouchActions.tap(640, 610, delayTime = 1500)
                break
            }
            delay(500)
        }
        enterMainScreen()
    }
}
