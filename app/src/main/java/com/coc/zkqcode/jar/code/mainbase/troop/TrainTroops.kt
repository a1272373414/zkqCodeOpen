package com.coc.zkqcode.jar.code.mainbase.troop

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 训练部队 / 法术（T12，源 造兵(241a/239a/240a)，文档 Q 段 L34253-41171）。
 *
 * 源流程：进军队/造兵栏界面 → 按「造兵配置」逐一造兵种与法术（含活动兵、超级兵强化、单位占用扣减）。
 *
 * 本项目实现要点（区别于源）：
 *  - 用 [ChineseTextReader.locate] 文字定位「军队/训练营」入口、兵种/法术名、「训练」按钮，方向无关、抗皮肤；
 *  - 源里「按兵种显示左/中/右栏切换 + 按造兵特征点兵 + 单位占用扣减」依赖 2023 版特征表，本项目简化为文字定位 + 按数量连点；
 *  - 每个兵种/法术的开启与数量由本账号记忆键 `Train_<名称>` 控制（默认无该键=不造）。
 *
 * 注：源「单位占用扣减、活动兵/超级兵强化」等精细逻辑待 T30 真机复标后再补；当前为可运行的结构化初版。
 */
object TrainTroops {

    private const val TROOP_KEY_PREFIX = "Train_"

    // 常见兵种名（与源 L829 兵列表顺序一致），实际造哪些由账号记忆键控制
    private val TROOPS = listOf(
        "野蛮人", "弓箭手", "巨人", "哥布林", "气球兵", "法师", "炸弹人", "飞龙", "皮卡超人",
        "野猪骑士", "瓦基丽武神", "戈仑石匠", "女巫", "熔岩猎犬", "矿工", "飞龙宝宝", "掘地矿工",
        "雷电飞龙", "超哥布林", "超炸弹人", "超巨人", "隐秘弓箭手", "超野蛮人", "超法师",
        "超级弓箭手", "大力士", "寒冰猎犬"
    )
    private val SPELLS = listOf(
        "骷髅法术", "治疗法术", "狂暴法术", "弹跳法术", "冰冻法术", "镜像法术", "毒药法术",
        "地震法术", "急速法术", "蝙蝠法术", "隐形法术", "召回法术"
    )

    /**
     * 为指定账号训练部队（若开关开启且配置了兵种/法术）。
     * @return 是否执行了训练动作
     */
    suspend fun trainTroops(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.TRAIN_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，训练：开始")
        if (!enterArmy()) {
            ShowMessage("账号$account，训练：未能进入军队界面")
            return false
        }
        for (troop in TROOPS) {
            val count = troopCount(account, troop) ?: continue
            trainOne(troop, count)
        }
        // 切到法术页再训练法术
        ChineseTextReader.locate("法术")?.let {
            TouchActions.tap(it.centerX(), it.centerY()); delayWithMultiplier(1200)
        }
        for (spell in SPELLS) {
            val count = troopCount(account, spell) ?: continue
            trainOne(spell, count)
        }
        ShowMessage("账号$account，训练：结束")
        return true
    }

    /** 进军队/造兵栏界面（源 函数241a 先找「军队界面」再找「造兵栏界面」）。 */
    private suspend fun enterArmy(): Boolean {
        val entry = ChineseTextReader.locate("军队") ?: ChineseTextReader.locate("训练营")
        if (entry != null) {
            TouchActions.tap(entry.centerX(), entry.centerY())
            delayWithMultiplier(1500)
            return true
        }
        return false
    }

    /** 训练某兵种/法术 count 次：定位名称 → 选中 → 点「训练」按钮 count 次。 */
    private suspend fun trainOne(name: String, count: Int) {
        val box = ChineseTextReader.locate(name) ?: return
        TouchActions.tap(box.centerX(), box.centerY())
        delayWithMultiplier(600)
        val trainBtn = ChineseTextReader.locate("训练") ?: ChineseTextReader.locate("造兵")
        repeat(count) {
            trainBtn?.let {
                TouchActions.tap(it.centerX(), it.centerY())
                delay(300)
            }
        }
        delayWithMultiplier(800)
    }

    /** 本账号该兵种/法术的训练数量（记忆键 `Train_<名称>`）。 */
    private suspend fun troopCount(account: Int, name: String): Int? =
        readMemory(StorageKeys.withAccountNumber(TROOP_KEY_PREFIX + name, account))
            .toIntOrNull()?.takeIf { it > 0 }
}
