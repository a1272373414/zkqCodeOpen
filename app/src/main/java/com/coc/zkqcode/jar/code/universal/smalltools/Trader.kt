package com.coc.zkqcode.jar.code.universal.smalltools

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import kotlinx.coroutines.delay

/**
 * 每周精选 / 商人购买（T08，源 商人(18)+精选(20)，文档 ⑬ L42550-44297）。
 *
 * 源脚本按「账号开关 × 商品」矩阵逐个买：读活动币 → 进每周精选页 → 对每个开启的商品
 * 定位并点「购买」→ 点绿色「确认」→ 翻页继续；最后切宝石栏领免费物品。
 *
 * 本项目实现策略（区别于源，更抗版本/皮肤）：
 *  - 用 [ChineseTextReader.locate] 按**文字**定位商品与「确认/购买/领取」按钮（方向无关，复用 T35）；
 *  - 不照搬源里大量 2023 版像素坐标（蓝/绿配色待真机复标，见 T30）；
 *  - 每个商品的开启与否由本账号记忆键 `Trader_<商品名>` 控制（默认关，绝不误买）。
 *
 * 注：源里「读活动币余额比对价格（bi()）」的预算校验此处省略——余额不足时确认按钮不会出现，
 *     点确认自然无效；精确预算校验待 T30 接入账号配置后补。
 */
object Trader {

    /** 各商品名（与源「买XXzhN」开关一一对应，待 T30 接入账号配置）。 */
    private val ITEMS = listOf(
        "自动买史诗", "木棍马驹", "火箭飞矛", "陨石法杖", "巨大火球",
        "火箭背包", "雷电战靴", "英雄火炬", "灵蛇手镯", "冷冽冰晶"
    )
    private const val ITEM_KEY_PREFIX = "Trader_"

    /**
     * 为指定账号执行每周精选购买 + 领免费物品。
     * @return 是否执行了动作（主开关未开则返回 false）
     */
    suspend fun purchaseWeeklySelection(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.TRADER_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，商人：开始每周精选购买")
        if (!enterTrader()) {
            ShowMessage("账号$account，商人：未能进入商人界面")
            return false
        }
        enterWeeklySelection()
        for (item in ITEMS) {
            if (!isItemEnabled(account, item)) continue
            buyByText(item)
            delay(800)
        }
        claimFreeItem()
        ShowMessage("账号$account，商人：每周精选购买结束")
        return true
    }

    /** 进商人界面：主界面点「商人」/「商店」入口（文字定位，方向无关）。 */
    private suspend fun enterTrader(): Boolean {
        val entry = ChineseTextReader.locate("商人") ?: ChineseTextReader.locate("商店")
        if (entry != null) {
            TouchActions.tap(entry.centerX(), entry.centerY())
            delay(1500)
            return true
        }
        return false
    }

    /** 进「每周精选」标签页。 */
    private suspend fun enterWeeklySelection() {
        ChineseTextReader.locate("每周精选")?.let {
            TouchActions.tap(it.centerX(), it.centerY())
            delay(1200)
        }
    }

    /** 按文字定位商品 → 点购买 → 点绿色「确认」。 */
    private suspend fun buyByText(item: String): Boolean {
        val itemBox = ChineseTextReader.locate(item) ?: return false
        TouchActions.tap(itemBox.centerX(), itemBox.centerY())
        delay(1000)
        val confirm = ChineseTextReader.locate("确认") ?: ChineseTextReader.locate("购买")
        if (confirm != null) {
            TouchActions.tap(confirm.centerX(), confirm.centerY())
            delay(3500) // 等购买动画/弹窗消失
            return true
        }
        return false
    }

    /** 切到宝石栏领免费物品（源 L43962-43985）。 */
    private suspend fun claimFreeItem() {
        ChineseTextReader.locate("免费")?.let { freeTab ->
            TouchActions.tap(freeTab.centerX(), freeTab.centerY())
            delay(1500)
            ChineseTextReader.locate("领取")?.let { claim ->
                TouchActions.tap(claim.centerX(), claim.centerY())
                delay(2000)
            }
        }
    }

    /** 本账号是否开启该商品的购买（记忆键 `Trader_<商品名>`，默认关）。 */
    private suspend fun isItemEnabled(account: Int, item: String): Boolean =
        readMemory(StorageKeys.withAccountNumber(ITEM_KEY_PREFIX + item, account)) == "1"
}
