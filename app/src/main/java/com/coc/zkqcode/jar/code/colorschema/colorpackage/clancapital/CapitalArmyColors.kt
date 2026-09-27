@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * UI elements of the clan capital army screen (independent from main village / builder base).
 *
 * Migrated from the legacy freescript and rotated to this project's 1280x720 landscape space
 * (x' = y, y' = 719 - x, offset (dx, dy) -> (dy, -dx)), then verified against real screenshots.
 */
interface ICapitalArmyColors {
    /** Green "train / confirm" button of the army screen. */
    val CapitalTrainConfirm: ColorSchema
    /** 都城内左侧"军队"入口图标（聊天框下方、回营上方），点击直接打开"更改都城军队"面板。 */
    val CapitalArmyEntryIcon: ColorSchema
    /** "更改都城军队"面板里的红色删除(垃圾桶)按钮：清空已编部队。 */
    val CapitalArmyDeleteButton: ColorSchema
}

object CapitalArmyColors : ICapitalArmyColors {
    // "更改都城军队"面板的"保存"按钮：由实机面板截图 panel_open 标定。
    // 注意：原绿色特征(7DF2D5)区域(747,259)-(938,351)在实机截图上对应"突袭信息"按钮，
    // 并非保存按钮；实机保存按钮在右侧、主体为红色（RGB(222,18,23)），与"取消"按钮同色但分居两侧。
    // 只取 x≈625~700 的保存按钮红色背景，避免与"取消"按钮(555~600)重叠。
    override val CapitalTrainConfirm = ColorSchema.parse(
        625, 302, 700, 336, "1712E8",
        "0|0|1713DA,56|24|2326A9,32|0|1813E9",
        0, 0.9, "都城训练确认"
    )

    // 都城内左侧"军队"入口图标（橙红圆角图标：黄色小人 + 蓝色瓶子，带红点角标）。
    // 2026-09-27 由实机都城截图 zoom2_base 标定：蓝瓶 RGB(40,234,228)@(78,520)、
    // 黄小人 RGB(255,235,84)@(46,510)，图标主体 y 493~531，点击中心 (62,512)。
    // 注意：该图标并非都城独有（主世界/夜世界也可能出现同类入口），只能用于"点击打开造兵面板"，
    // 不能作为"是否在都城"的判定依据；都城判定见 [MyColors.CapitalVillageLabel] 与 [MyColors.CapitalGoldIcon]。
    override val CapitalArmyEntryIcon = ColorSchema.parse(
        10, 470, 110, 560, "E4EA28-282828",
        "-32|-10|54EBFF-282828",
        0, 0.88, "都城军队入口图标"
    )

    // "更改都城军队"面板的红色删除(垃圾桶)按钮：由实机面板截图 panel_open 标定。
    // 只取按钮红色背景左侧边缘（x≈470~490），避免误匹配顶部军队卡片上的"移除"小红叉，
    // 也避免与右侧"取消"按钮重叠。主色 RGB(222,18,23)。
    override val CapitalArmyDeleteButton = ColorSchema.parse(
        472, 302, 488, 336, "1712E8",
        "0|0|2018E4,14|32|2326A9,14|12|1712E9",
        0, 0.9, "都城军队删除按钮"
    )
}
