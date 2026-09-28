@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * 部落竞赛（Clan Games）相关识别特征，移植自源脚本 `awcocx_main.lua`：
 *  - 函数233a L33513 竞赛主流程（找竞赛屋 → 进界面 → 判已接/蓝徽章/积分满 → 领奖励）
 *  - 函数230a L33267 接任务（进界面点绿色按钮）
 *  - 函数232a L33559 领奖励（在指定区域找「可领奖励」图标并领取）
 *  - L42649 主循环调度（大本 > 5 且开关打开时，按节流时间检查竞赛）
 *
 * 坐标：源脚本是竖屏 720x1280，本项目是横屏 1280x720，按项目通用 90° 映射换算
 * （区域 (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1)；偏移 (dx,dy) -> (dy,-dx)）。
 * 颜色串沿用源脚本的 BGR 写法，与 [ColorSchema.parse] 一致。
 *
 * TODO(真机标定)：全部特征均来自 2023 年版源脚本，聚类/按钮配色大概率已变，
 * 首轮真机需按 `tools/make_feature.py` 逐个复核（尤其 竞赛屋 / 已接任务 / 绿接任务按钮 三个关键特征）。
 */
interface IClanGamesColors {
    /** 主村庄里的竞赛屋（源 L33517 的 findImage「竞赛屋.png」备选颜色特征）。 */
    val ClanGamesHut: ColorSchema

    /** 竞赛面板里「已接任务」的蓝色标记（源 L33532）。 */
    val ClanGamesTaskAccepted: ColorSchema

    /** 竞赛面板里任务已完成可领奖的「蓝徽章」（源 L33538，配合全量查找计数 ≥3 生效）。 */
    val ClanGamesBlueBadge: ColorSchema

    /** 竞赛结束时「可领奖励」的绿色标识（源 L33551）。 */
    val ClanGamesRewardsReady: ColorSchema

    /** 任务详情里的绿色「接受任务」按钮（源 L33272）。 */
    val ClanGamesAcceptButton: ColorSchema

    /** 已接任务「任务失败」的蓝色标题（源 L33717）。 */
    val ClanGamesTaskFailed: ColorSchema

    /** 放弃任务后的冷却提示（源 L33695）。 */
    val ClanGamesCooldown1: ColorSchema

    /** 放弃任务后的冷却提示（源 L33706）。 */
    val ClanGamesCooldown2: ColorSchema

    /** 「竞赛开始倒计时」弹窗（源 L33684）。 */
    val ClanGamesCountdownPopup: ColorSchema

    /** 任务列表第一张卡（源 L34004，无任务可接时用它放弃刷新）。 */
    val ClanGamesFirstTaskCard: ColorSchema

    /** 任务详情里的绿色「放弃任务」按钮（源 L34008）。 */
    val ClanGamesGiveUpButton: ColorSchema

    /** 放弃任务后的蓝色确认按钮（源 L34021）。 */
    val ClanGamesAbandonConfirm: ColorSchema

    /** 放弃任务后的绿色按钮（源 L34025）。 */
    val ClanGamesAbandonGreenConfirm: ColorSchema

    /** 奖励列表里「这一行可领」的绿色勾（源 L33634，用全量查找枚举每一行）。 */
    val ClanGamesRewardRow: ColorSchema

    /** 下滑到底后最后一行的绿色勾（源 L33644）。 */
    val ClanGamesRewardRowBottom: ColorSchema

    /** 任务列表右侧一列的绿色操作按钮（源 L33640）。 */
    val ClanGamesGreenActionButton: ColorSchema

    /** 点奖励图标后弹出的绿色领取确认按钮（源 L33610）。 */
    val ClanGamesClaimConfirm: ColorSchema

    /** 竞赛面板右上角灰白色「我的任务 / 全部任务」切换按钮（源 L33616）。 */
    val ClanGamesTasksTab: ColorSchema

    /** 奖励卡的橙色底（源 L33618，找不到具体奖励图标时随机点一张）。 */
    val ClanGamesTaskCardOrange: ColorSchema

    /** 竞赛积分已做满（源 L33544 的 cmpColorEx，多点比色）。 */
    val ClanGamesPointsFull: ColorSchema

    /** 「任务完成」提示（源 L33658 的 cmpColorEx，多点比色）。 */
    val ClanGamesTaskCompleted: ColorSchema

    /** 「你需要加入部落才能参加竞赛」提示（源 L33678 的 cmpColorEx，多点比色）。 */
    val ClanGamesNoClan: ColorSchema
}

object ClanGamesColors : IClanGamesColors {

    /** 源 L33517：竞赛屋（棕色木屋 + 深色窗 + 屋顶）。 */
    override val ClanGamesHut = ColorSchema.parse(
        322, 72, 670, 319, "634020",
        "8|2|a57c52,9|3|bc8a58,6|13|0e1720,8|17|15212f,8|-8|6c686c,11|-5|7a592d,10|-17|50341b",
        0, 0.9, "竞赛屋"
    )

    /** 源 L33532：竞赛面板「已接任务」蓝标。 */
    override val ClanGamesTaskAccepted = ColorSchema.parse(
        467, 268, 522, 324, "40C8EA",
        "-2|-2|40C8EA,6|2|40C8EA,-4|4|2F8AA1,-2|5|2F8AA1,2|8|2F8AA1,7|8|2F8AA1,14|8|2F8AA1,8|3|40C8EA,16|3|40C8EA",
        0, 0.9, "竞赛已接任务"
    )

    /** 源 L33538：任务完成可领奖的蓝徽章（同一行出现 ≥3 个才算数）。 */
    override val ClanGamesBlueBadge = ColorSchema.parse(
        565, 261, 1082, 301, "D1B7B6",
        "1|-1|D8BFBD,-3|-2|EDDCDB,-1|-4|F2E3E1,-9|7|52E053,5|7|52DF52,-10|-9|52E053,-4|-12|FFC078,1|-11|FFC075,3|-13|FFA64A,7|-10|52E053",
        0, 0.9, "竞赛蓝徽章"
    )

    /** 源 L33551：竞赛结束「可领奖励」绿色标识。 */
    override val ClanGamesRewardsReady = ColorSchema.parse(
        447, 139, 538, 182, "60D2AD",
        "-2|-6|60D3A9,2|-15|60D1AA,7|-3|60D2A9,7|-10|60D1A9,55|-1|60D2AC,58|-8|60D2AA,55|-15|60D2AC,48|-15|60D4B0,48|-1|60D3AF",
        0, 0.9, "竞赛可领奖励"
    )

    /** 源 L33272：任务详情里的绿色「接受」按钮（按钮 + 高光边）。 */
    override val ClanGamesAcceptButton = ColorSchema.parse(
        400, 110, 1082, 546, "36D186",
        "4|-72|8DE9BD,160|-3|35D18D,154|-71|8DE9BD,84|8|35D48B,78|-74|8DE9BD",
        0, 0.9, "竞赛接受任务"
    )

    /** 源 L33717：已接任务失败（蓝色标题竖排四点校验）。 */
    override val ClanGamesTaskFailed = ColorSchema.parse(
        443, 269, 653, 314, "242BE7",
        "19|0|242BE6,38|0|282FEA,58|0|242CE5,79|0|242BE7",
        0, 0.9, "竞赛任务失败"
    )

    /** 源 L33695：放弃任务后的冷却提示（横向多行卡片）。 */
    override val ClanGamesCooldown1 = ColorSchema.parse(
        605, 83, 1086, 265, "F0F4F0",
        "2|-30|C5844B,49|0|F0F4F0,47|-30|C1844F,98|0|F0F4F0,101|-29|C5844B,151|-1|F0F4F0,149|-31|C2844E,93|-52|D09C70,143|-52|D09C70",
        0, 0.9, "竞赛冷却中1"
    )

    /** 源 L33706：放弃任务后的冷却提示（灰色遮罩）。 */
    override val ClanGamesCooldown2 = ColorSchema.parse(
        485, 258, 615, 297, "DDDDDD",
        "4|2|C2C2C2,-1|-9|B3B3B3,-4|-9|959595,6|-9|979797,4|-9|B2B2B2,11|6|A6A6A6,11|-11|A5A5A5",
        0, 0.9, "竞赛冷却中2"
    )

    /** 源 L33684：「竞赛开始倒计时」弹窗（白底 + 蓝字 + 彩带）。 */
    override val ClanGamesCountdownPopup = ColorSchema.parse(
        974, 58, 1073, 150, "F8F7FF",
        "17|0|FCFDFA,-13|0|231FF4,31|-2|1F1EF6,-3|12|241DF3,20|13|231EF1,-11|-24|807FFB,1|-23|FFFDFE,17|-22|FAFDFF,31|-21|7F77FE,-11|-34|8883FF,31|-34|8784FF,-15|13|211FEF,37|11|201DF6",
        0, 0.9, "竞赛开始倒计时"
    )

    /** 源 L34004：任务列表第一张卡（无任务可接时点它放弃刷新）。 */
    override val ClanGamesFirstTaskCard = ColorSchema.parse(
        483, 253, 622, 307, "F0E0DD",
        "3|2|D7BEBB,-4|-9|FFA84B,-1|-9|FFC077,3|-9|FFC076,11|-10|52E053,12|5|51DE52,-41|10|52E053",
        0, 0.9, "竞赛第一张任务卡"
    )

    /** 源 L34008：任务详情里的绿色「放弃任务」按钮。 */
    override val ClanGamesGiveUpButton = ColorSchema.parse(
        822, 269, 999, 418, "3AD48B",
        "-15|1|3AD48B,-25|0|3AD48B,-33|-1|3AD48B,-42|-1|3AD48B,-71|-27|8EEABF,-74|-30|3AD48B,-73|49|37C781,90|49|37C781,88|-27|8EEABF,89|-30|3AD48B",
        0, 0.9, "竞赛放弃任务"
    )

    /** 源 L34021：放弃任务后的蓝色确认按钮。 */
    override val ClanGamesAbandonConfirm = ColorSchema.parse(
        820, 271, 1000, 408, "0D4FD3",
        "30|-15|0D39A9,31|1|0D38AA,-59|-45|8785FF,-67|27|221EF6,96|29|221EF2,91|-43|8785FF,-23|-4|221EF7,51|-5|221EF7",
        0, 0.9, "竞赛放弃确认"
    )

    /** 源 L34025：放弃任务后的绿色按钮（放弃完成）。 */
    override val ClanGamesAbandonGreenConfirm = ColorSchema.parse(
        620, 369, 941, 560, "1FBD70",
        "2|-73|8CFAE2,204|-2|1DBD6D,200|-69|85F6DD,103|-74|8DFAE3,105|-102|E0E8E8,112|26|E0E8E8,110|3|20C075,42|0|1FBD70,42|-74|8DFAE3,162|1|1FBE71,162|-73|8CFAE2",
        0, 0.9, "竞赛放弃完成"
    )

    /** 源 L33634：奖励列表里「这一行可领」的绿色勾（全量查找枚举每一行）。 */
    override val ClanGamesRewardRow = ColorSchema.parse(
        445, 135, 1110, 186, "20AE60",
        "-2|-2|4CCF9D,-4|-5|4CCF9E,3|-4|4CCF9D,4|-6|4CCF9D,5|-9|6AE0C0,6|-12|6AE0C0,-26|2|61D2AD,-27|-12|62D3AE,30|2|61D2AD,30|-13|62D2AD,32|-6|62D3AE,-29|-6|62D3AE",
        0, 0.9, "竞赛可领奖励行"
    )

    /** 源 L33644：下滑到底后最后一行的绿色勾。 */
    override val ClanGamesRewardRowBottom = ColorSchema.parse(
        1040, 135, 1140, 186, "20AE60",
        "-2|-2|4CCF9D,-4|-5|4CCF9E,3|-4|4CCF9D,4|-6|4CCF9D,5|-9|6AE0C0,6|-12|6AE0C0,-26|2|61D2AD,-27|-12|62D3AE,30|2|61D2AD,30|-13|62D2AD,32|-6|62D3AE,-29|-6|62D3AE",
        0, 0.9, "竞赛可领奖励行(底部)"
    )

    /** 源 L33640：任务列表右侧一列的绿色操作按钮（一列 4 个，用于判断列表是否到底）。 */
    override val ClanGamesGreenActionButton = ColorSchema.parse(
        840, 477, 1162, 580, "3AD48B",
        "0|-26|80E6B6,-125|4|39D289,-127|-14|3AD48B,-126|-25|7EE6B5,-121|-38|8EEABF,-73|2|3AD38B,-73|-39|8EEABF,51|3|3AD38A,53|-37|8EEABF,92|1|3AD48B,95|-38|8EEABF,124|1|3AD48B,128|-37|8EEABF",
        0, 0.9, "竞赛绿色操作按钮"
    )

    /** 源 L33610：点奖励图标后弹出的绿色领取确认按钮。 */
    override val ClanGamesClaimConfirm = ColorSchema.parse(
        657, 388, 902, 538, "1FBC6D",
        "-1|-67|90FBE4,-106|9|20BF73,-103|-67|90FBE4,103|11|20C177,95|-68|91FBE4,97|-15|23C475,-93|-13|23C274",
        0, 0.9, "竞赛领取确认"
    )

    /** 源 L33616：竞赛面板右上角灰白色「我的任务 / 全部任务」切换按钮。 */
    override val ClanGamesTasksTab = ColorSchema.parse(
        888, 19, 946, 108, "E0E8E8",
        "0|-7|E0E8E8,0|-14|E0E8E8,-1|-22|E0E8E8,-1|-36|E0E8E8,6|-29|51545E,6|-16|51545E,6|-8|51545E,7|-3|51545E,12|-2|51545E,19|-3|51545E,6|2|E0E8E8,14|2|E0E8E8,19|2|E0E8E8",
        0, 0.9, "竞赛任务切换按钮"
    )

    /** 源 L33618：奖励卡的橙色底（找不到具体奖励图标时随机点一张）。 */
    override val ClanGamesTaskCardOrange = ColorSchema.parse(
        445, 199, 1110, 489, "DC9254",
        "0|-5|F6C484,0|-15|FFE2B8,14|0|DC9254,14|-6|F6C484,14|-15|FFE2B8,6|-5|FFFFFF,6|-8|FFFFFF,6|-13|FFFFFF",
        0, 0.9, "竞赛奖励卡"
    )

    /** 源 L33544 cmpColorEx：竞赛积分进度条已填满（= 积分做满）。 */
    override val ClanGamesPointsFull = ColorSchema.parse(
        340, 630, 380, 680, "40C8EA",
        "0|-4|40C8EA,-1|-12|6ADDFA,2|-12|6ADDFA,2|-2|6ADDFA",
        0, 0.9, "竞赛积分已做满"
    )

    /** 源 L33658 cmpColorEx：「任务完成」提示条。 */
    override val ClanGamesTaskCompleted = ColorSchema.parse(
        585, 255, 630, 295, "3DC6E7",
        "-1|-8|65DDF5,1|-4|3DC7E8,2|-4|65DDFA,8|0|3DC7E8,8|-9|3DC7E8,14|-9|E0E8E8,14|3|E0E8E8",
        0, 0.9, "竞赛任务完成"
    )

    /** 源 L33678 cmpColorEx：「你需要加入部落才能参加竞赛」提示。 */
    override val ClanGamesNoClan = ColorSchema.parse(
        950, 100, 1040, 180, "221DF5",
        "4|-45|8583FD,18|-14|FDF9FD,34|-35|FFFFFF,50|0|1E1DF5,49|-44|8583FD",
        0, 0.9, "竞赛未加入部落"
    )
}
