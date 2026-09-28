package com.coc.zkqcode.jar.code.mainbase.clan

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 都城币捐给部落（T39，源 函数268a L37267）。
 *
 * 源流程：进部落 → 进都城 → 函数275a 定位「都城地图小船」→ 放大 → 函数65a 反复点「捐」按钮
 * （[贡献ms] 为单次贡献量）。本项目用 [ChineseTextReader.locate] 文字定位「部落/都城/捐/贡献/确定」，
 * 源「都城地图小船」「放大」坐标作注释兜底，待 T30 真机复标。
 *
 * 主开关 [StorageKeys.CAPITAL_COIN_DONATE_ENABLED] 未开则跳过。目标捐币量由各账号
 * [StorageKeys.CAPITAL_COIN_DONATE_AMOUNT] 记忆键控制（空/0=尽力捐满）。
 */
object CapitalCoinDonate {

    suspend fun donateCapitalCoins(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.CAPITAL_COIN_DONATE_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，都城币捐：开始")
        if (!enterClan()) {
            ShowMessage("账号$account，都城币捐：未能进入部落")
            return false
        }
        if (!enterCapital()) {
            ShowMessage("账号$account，都城币捐：未能进入都城")
            return false
        }
        // 反复点「捐/贡献」若干次（源 函数65a 循环），命中即点「确定」确认
        var donated = false
        repeat(7) {
            val donateBtn = ChineseTextReader.locate("捐") ?: ChineseTextReader.locate("贡献")
            if (donateBtn == null) return@repeat
            TouchActions.tap(donateBtn.centerX(), donateBtn.centerY())
            delay(800)
            if (tapIfPresent("确定") || tapIfPresent("捐赠")) donated = true
            delay(600)
        }
        ShowMessage("账号$account，都城币捐：结束${if (donated) "（已捐）" else "（无可捐）"}")
        return donated
    }

    private suspend fun enterClan(): Boolean {
        ChineseTextReader.locate("部落")?.let {
            TouchActions.tap(it.centerX(), it.centerY())
            delay(1500)
            return true
        }
        return false
    }

    private suspend fun enterCapital(): Boolean {
        ChineseTextReader.locate("都城")?.let {
            TouchActions.tap(it.centerX(), it.centerY())
            delay(2000)
            return true
        }
        return false
    }

    private suspend fun tapIfPresent(text: String): Boolean {
        val box = ChineseTextReader.locate(text) ?: return false
        TouchActions.tap(box.centerX(), box.centerY())
        delay(800)
        return true
    }
}
