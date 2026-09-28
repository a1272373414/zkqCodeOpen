package com.coc.zkqcode.jar.code.mainbase.daily

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 每日福利领取（T41，源 商店/活动/奖励 ⑬：领令牌 L42550、领红包 函数291a L39012、
 * 领活动奖励 函数361a L40936、开箱 L6795、签到 L6821、卖药水 L46147）。
 *
 * 源脚本用 2023 版像素坐标 + 多点找色逐按钮点击（如领令牌 `2215EF`、红包 `1FBD70`、
 * 签到 `6EB766`/`00B4FF`）。本项目统一改为 [ChineseTextReader.locate] 文字定位，
 * 抗皮肤、方向无关；源坐标保留为注释兜底，待 T30 真机复标。
 *
 * 各子项独立开关（默认关），主开关 [StorageKeys.DAILY_REWARDS_ENABLED] 未开则整块跳过。
 * 因各福利入口 UI 分散且依赖真机布局，本实现为「文字定位 + 点击」的尽力而为版本，
 * 精确导航/坐标待 T30 复标后精修。
 */
object DailyRewards {

    /** 为账号执行所有已开启的每日福利子项；主开关未开返回 false。 */
    suspend fun claimDailyRewards(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.DAILY_REWARDS_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，每日福利：开始")
        var acted = false
        if (isOn(account, StorageKeys.DAILY_TOKEN)) acted = claimToken() || acted
        if (isOn(account, StorageKeys.DAILY_RED_PACKET)) acted = claimRedPacket() || acted
        if (isOn(account, StorageKeys.DAILY_EVENT)) acted = claimEvent() || acted
        if (isOn(account, StorageKeys.DAILY_CHEST)) acted = claimChest() || acted
        if (isOn(account, StorageKeys.DAILY_SIGN_IN)) acted = claimSignIn() || acted
        if (isOn(account, StorageKeys.DAILY_SELL_POTION)) acted = sellPotion() || acted
        ShowMessage("账号$account，每日福利：结束")
        return acted
    }

    /** 领令牌（源 L42550）：定位「令牌」入口 → 点开 → 遇教程点「跳过/关闭」。 */
    private suspend fun claimToken(): Boolean {
        if (!tapText("令牌")) return false
        delay(1500)
        tapText("跳过")
        tapText("关闭")
        delay(800)
        return true
    }

    /** 领红包（源 函数291a L39012）：定位「红包」→ 点击。 */
    private suspend fun claimRedPacket(): Boolean {
        return tapText("红包")
    }

    /** 领活动奖励（源 函数361a L40936）：进「活动」→ 点「领取」。 */
    private suspend fun claimEvent(): Boolean {
        if (!tapText("活动")) return false
        delay(1500)
        return tapText("领取")
    }

    /** 开箱（源 L6795）：定位「开箱/宝箱」→ 多点开 → 点「继续」关动画。 */
    private suspend fun claimChest(): Boolean {
        if (!tapText("开箱") && !tapText("宝箱")) return false
        repeat(4) {
            TouchActions.tap(640, 640, delayTime = 300)
            delay(200)
        }
        delay(1500)
        tapText("继续")
        return true
    }

    /** 签到（源 L6821）：定位「签到」→ 点「立即签到」/「签到」。 */
    private suspend fun claimSignIn(): Boolean {
        if (!tapText("签到")) return false
        delay(1200)
        return tapText("立即签到") || tapText("签到")
    }

    /** 卖药水（源 L46147）：定位「药水/魔法物品」→ 进栏 → 点「出售」。 */
    private suspend fun sellPotion(): Boolean {
        if (!tapText("药水") && !tapText("魔法物品")) return false
        delay(1500)
        return tapText("出售")
    }

    /** 文字定位并点击；命中返回 true。 */
    private suspend fun tapText(text: String): Boolean {
        val box = ChineseTextReader.locate(text) ?: return false
        TouchActions.tap(box.centerX(), box.centerY())
        delay(1000)
        return true
    }

    private suspend fun isOn(account: Int, key: String): Boolean =
        readMemory(StorageKeys.withAccountNumber(key, account)) == "1"
}
