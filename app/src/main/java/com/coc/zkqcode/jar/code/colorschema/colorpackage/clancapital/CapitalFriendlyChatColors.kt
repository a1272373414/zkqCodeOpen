@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * 聊天界面「都城友谊战」卡片的识别特征。
 *
 * 都城友谊战卡片在部落聊天里长这样（区别于主世界/夜世界友谊战）：
 *   - 卡片底部一行三个彩色按钮：绿「侦察」+ 红「进攻」+ 蓝「详细信息」。
 *   - 已攻破的友谊战卡片（如「法师山谷 100%」）只有绿「回放」+ 蓝「详细信息」、没有红「进攻」，
 *     因此本特征只命中【尚未进攻过】的都城友谊战，正好用于不消耗突袭次数的都城下兵测试入口。
 *
 * 颜色串按 BGR 写（ColorSchema.parseBgrToRgb 按 0xBBGGRR 解析）。
 *
 * 标定自实机聊天截图（1280×720，2026-09-27 重新发起的友谊战卡片）：
 *   - 卡片在聊天中位置不固定（随聊天滚动上下移动），故扫描区覆盖整个聊天可见区。
 *   - 三按钮填充色：红(进攻) RGB(243,37,41)→BGR 2925F3；绿(侦察) RGB(136,206,58)→BGR 3ACE88；
 *     蓝(详情) RGB(81,146,243)→BGR F39251。
 *   - 以红「进攻」作主色；偏移绿「侦察」(-78,+24) 与蓝「详细信息」(+170,+22) 双重校验，
 *     主世界/夜世界友谊战（只有单个红按钮、无此组合）不会误命中。
 *   - 命中返回点落在红按钮顶部偏右（按钮内有白字，多色匹配只在文字外的填充区触发），
 *     调用方点击时 `y+23` 即可落到红按钮竖直中心。
 */
interface ICapitalFriendlyChatColors {
    /** 卡片里的红色「进攻」按钮：本特征同时用于「识别」和「点击进入」。 */
    val CapitalFriendlyAttackButton: ColorSchema
}

object CapitalFriendlyChatColors : ICapitalFriendlyChatColors {
    // 红「进攻」作主色：命中即代表该卡片是未进攻过的都城友谊战。
    // 偏移绿「侦察」(-78,+24) 与蓝「详细信息」(+170,+22) 双重校验，主世界/夜世界友谊战（只有单个红按钮、无此组合）不会误命中。
    override val CapitalFriendlyAttackButton = ColorSchema.parse(
        2, 90, 480, 690, "2925F3",
        "-78|24|3ACE88,170|22|F39251",
        0, 0.85, "CapitalFriendlyAttackButton"
    )
}
