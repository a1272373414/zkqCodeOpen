package com.coc.zkqcode.jar.code.mainbase.upgrade

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 升级建筑（T13，源 升级(202a/205a/206a)，文档 P 段 L24387-34252）。
 *
 * 源流程：升级总入口 [函数202a] → 读配置 → 开建筑列表 → [函数205a] 逐列 OCR 建筑名/等级，
 * 匹配「目的」建筑 → 查空闲工人 → 升级/创建 → 宝石秒([函数219a])。
 *
 * 本项目实现要点（区别于源）：
 *  - 用 [ChineseTextReader.locate] 文字定位「商店/建筑」入口、目标建筑名、「升级」按钮，方向无关、抗皮肤；
 *  - 源里「建筑列表逐列 OCR(字库 jianzhu0.txt) + 空闲工人判断 + 宝石秒」依赖 OCR 字典与 2023 版坐标，
 *    本项目先用文字兜底：定位目标建筑 → 点「升级」→ 点「确定」；
 *  - 每个建筑的开启由本账号记忆键 `Upgrade_<名称>` 控制（默认无该键=不升）。
 *
 * 注：源「建筑列表 OCR 扫描 + 空闲工人数/预留工人判断 + 整列表回滚重扫」待 T30 真机复标 + OCR 字典接入后再补；
 * 当前为可运行的结构化初版，命中率依赖文字定位。
 */
object Upgrade {

    private const val BUILDING_KEY_PREFIX = "Upgrade_"

    private val BUILDINGS = listOf(
        "大本营", "实验室", "兵营", "训练营", "法术工厂", "暗黑训练营", "暗黑法术工厂", "迫击炮",
        "加农炮", "箭塔", "防空火箭", "法师塔", "炸弹塔", "特斯拉电磁塔", "地狱之塔", "X弩",
        "储金罐", "储水罐", "圣水瓶", "黑油罐", "部落城堡", "战争机器工坊", "建筑工人小屋",
        "野蛮人之王", "弓箭女王", "大守护者", "飞盾战神"
    )

    /**
     * 为指定账号升级建筑（若开关开启且配置了目标建筑）。
     * @return 是否执行了升级动作
     */
    suspend fun upgradeBuildings(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.UPGRADE_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，升级：开始")
        if (!openShop()) {
            ShowMessage("账号$account，升级：未能打开建筑列表")
            return false
        }
        // 源 函数205a 在「空闲工人<=预留」时停止；此处每升一个就尝试下一个，无空闲工人时「升级」按钮缺失即自然停
        for (b in BUILDINGS) {
            if (!buildingEnabled(account, b)) continue
            upgradeOne(b)
            delayWithMultiplier(800)
        }
        ShowMessage("账号$account，升级：结束")
        return true
    }

    /** 打开商店/建筑列表（源 函数202a 打开建筑列表）。 */
    private suspend fun openShop(): Boolean {
        val shop = ChineseTextReader.locate("商店") ?: ChineseTextReader.locate("建筑")
        if (shop != null) {
            TouchActions.tap(shop.centerX(), shop.centerY())
            delayWithMultiplier(1500)
            return true
        }
        return false
    }

    /** 升级单个建筑：定位名称 → 选中 → 点「升级」→ 点「确定」。 */
    private suspend fun upgradeOne(name: String): Boolean {
        val box = ChineseTextReader.locate(name) ?: return false
        TouchActions.tap(box.centerX(), box.centerY())
        delayWithMultiplier(1000)
        val up = ChineseTextReader.locate("升级") ?: return false
        TouchActions.tap(up.centerX(), up.centerY())
        delayWithMultiplier(1500)
        // 确认升级（绿色确认按钮，源 函数188a 点绿色升级/确认）
        ChineseTextReader.locate("确定")?.let {
            TouchActions.tap(it.centerX(), it.centerY())
            delayWithMultiplier(1200)
        }
        return true
    }

    private suspend fun buildingEnabled(account: Int, name: String): Boolean =
        readMemory(StorageKeys.withAccountNumber(BUILDING_KEY_PREFIX + name, account)) == "1"
}
