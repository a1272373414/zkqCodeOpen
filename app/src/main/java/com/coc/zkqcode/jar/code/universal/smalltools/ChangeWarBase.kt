package com.coc.zkqcode.jar.code.universal.smalltools

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.ui.schema.Schema
import kotlinx.coroutines.delay

/**
 * 战争基地阵型切换（T04，源 换阵模式 L55700-55783）。
 *
 * 流程：读 CHANGE_BASE 开关 → 取本账号目标阵型号(1~6) → 进战争基地编辑 → 开阵型列表
 *      → 按阵型号点槽位（>3 先上滑露出）→ 解锁则点绿色「装备」。
 *
 * 坐标说明：源脚本为竖屏 720x1280，本项目横屏 1280x720，按下述 90° 映射换算
 *     点(x,y) -> (y, 719-x)；区域(x1,y1,x2,y2) -> (y1,719-x2,y2,719-x1)
 *     （与 T07/T25 一致，如源(66,1080)->横屏(1080,653)、(560,840)->(840,159)）。
 * 源配色为 2023 版（蓝 1511EC 列表标记 / 绿 28AC80 解锁 / 灰 989898 锁定 / 绿 1FBB6C 装备），
 * 真机会复标（T30）；此处优先用文字 [ChineseTextReader.locate] 定位，坐标仅作兜底。
 */
object ChangeWarBase {

    /**
     * 为指定账号切换战争基地阵型。
     * @param account 当前账号编号（与 [InGamesVars.currentAccountNumber] 一致）
     * @return 是否执行了切换动作（未开开关 / 目标为 0/未配置 时返回 false，表示不换）
     */
    suspend fun changeWarBaseLayout(account: Int): Boolean {
        // 主开关（MAIN_BASE_SETTINGS.change_base，按账号配置组）
        if (getConfigRuntime(Schema.MAIN_BASE_SETTINGS.CHANGE_BASE.key) != "1") return false
        // 本账号目标阵型号（1~6），存于账号记忆；0/空=不换
        val layout = readMemory(
            StorageKeys.withAccountNumber(StorageKeys.WAR_BASE_LAYOUT, account)
        ).toIntOrNull() ?: 0
        if (layout !in 1..6) return false
        ShowMessage("账号$account，换阵：准备切换到阵型 $layout")

        // 1) 进战争基地编辑：优先文字定位，兜底用源竖屏(66,1080)/(278,1209) → 横屏(1080,653)/(1209,441)
        val edit = ChineseTextReader.locate("阵型")
            ?: ChineseTextReader.locate("编辑")
            ?: run { TouchActions.tap(1080, 653); null }
        if (edit != null) TouchActions.tap(edit.centerX(), edit.centerY())
        delay(1500)

        // 2) 等阵型列表出现（源用蓝 1511EC 标记，2023 配色待复标；这里延时+文字兜底）
        delay(800)

        // 3) 点列表头展开（源竖屏(560,840) → 横屏(840,159)）
        ChineseTextReader.locate("布局")?.let { TouchActions.tap(it.centerX(), it.centerY()) }
            ?: TouchActions.tap(840, 159)
        delay(1500)

        // 4) 选阵型号对应槽位（源竖屏(350,240/640/1040) → 横屏(240/640/1040,369)）
        val (sx, sy) = when (layout) {
            1 -> 240 to 369
            2 -> 640 to 369
            3 -> 1040 to 369
            else -> {
                // >3 先上滑露出（源竖屏(350,1260)->(350,10) 即横屏(1260,369)->(10,369)）
                TouchActions.swipe(1260, 369, 10, 369, delayTime = 200)
                delay(800)
                when (layout) {
                    4 -> 240 to 369
                    5 -> 640 to 369
                    6 -> 1040 to 369
                    else -> 240 to 369
                }
            }
        }
        TouchActions.tap(sx, sy)
        delay(1000)

        // 5) 点绿色「装备」（源竖屏(60,880) → 横屏(880,659)；优先文字定位）
        ChineseTextReader.locate("装备")?.let { TouchActions.tap(it.centerX(), it.centerY()) }
            ?: TouchActions.tap(880, 659)
        delay(1500)

        ShowMessage("账号$account，换阵：已尝试切换到阵型 $layout")
        return true
    }
}
