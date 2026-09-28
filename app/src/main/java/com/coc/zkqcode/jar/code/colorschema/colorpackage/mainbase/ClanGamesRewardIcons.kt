@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * 部落竞赛「可领奖励图标」模板库，移植自源脚本 `awcocx_main.lua` 函数232a（L33559~33605）。
 *
 * 作用：竞赛的奖励档位是一排卡片，源脚本在某一行奖励区域里按顺序尝试 16 组奖励图标模板，
 * 命中即点该图标领取；一组都没命中时退回"随便点一张奖励卡"。
 *
 * 坐标同 [ClanGamesTaskIcons]：竖屏 720x1280 -> 横屏 1280x720 的 90° 映射。
 * 这里的区域 (445,199,1110,489) 是"整条奖励带"；调用方会按命中行用
 * [ColorSchema.rescope] 收窄到该行附近（对应源 `函数232a(230, y-44, 520, y+52)`）。
 *
 * TODO(真机标定)：同任务图标，配色需真机复核。
 */
object ClanGamesRewardIcons {

    /** 源 L33560 */
    val ClanGamesRewardIcon01 = ColorSchema.parse(
        445, 199, 1110, 489, "2F8AA1",
        "5|3|2F8AA1,0|-7|40C8EA,3|-4|40C8EA,8|-1|40C8EA,12|3|2F8AA1,29|3|358BA5,51|3|2F8AA1,61|2|2F8AA1,63|-1|2E89A1,58|-2|40C8EA,61|-4|40C8EA,0|-14|40C8EA",
        0, 0.9, "ClanGamesRewardIcon01"
    )

    /** 源 L33564 */
    val ClanGamesRewardIcon02 = ColorSchema.parse(
        445, 199, 1110, 489, "7A84ED",
        "3|8|2E305E,4|4|7D85ED,6|-1|EBFFFF,6|-6|E8FFFF,12|-11|828BF0,13|10|2F305F,16|11|2E305E,17|7|858EF0,17|3|E9FFFF,21|4|EAFFFF,21|8|828BEF,31|9|7B85EE,37|12|94BCD8",
        0, 0.9, "ClanGamesRewardIcon02"
    )

    /** 源 L33566 */
    val ClanGamesRewardIcon03 = ColorSchema.parse(
        445, 199, 1110, 489, "FFD654",
        "1|-7|F4C850,13|5|D7A944,8|-3|E7BC4C,5|-15|EAD092,12|-10|E9CF98,16|-9|F0DCAF,17|-23|384A56,17|-29|95D1FF,28|-6|7ED7F6",
        0, 0.9, "ClanGamesRewardIcon03"
    )

    /** 源 L33569 */
    val ClanGamesRewardIcon04 = ColorSchema.parse(
        445, 199, 1110, 489, "740D96",
        "3|-3|7E0DB2,3|-9|710D9D,14|3|550D61,17|2|4F0D59,17|-12|704474,18|-24|394A57,18|-30|94D0FF,25|-1|745E58,15|-5|5F0D7E",
        0, 0.9, "ClanGamesRewardIcon04"
    )

    /** 源 L33572 */
    val ClanGamesRewardIcon05 = ColorSchema.parse(
        445, 199, 1110, 489, "FFEBED",
        "1|-9|FFDEE8,8|3|FFDAE7,17|3|F8BEDC,21|3|F2AFD3,26|-2|82DFFC,2|-18|C1CAD0,6|-14|FDD1E7,16|-10|FED9EE,17|-19|F9C7E1,17|-30|93CFFF",
        0, 0.9, "ClanGamesRewardIcon05"
    )

    /** 源 L33575 */
    val ClanGamesRewardIcon06 = ColorSchema.parse(
        445, 199, 1110, 489, "91849C",
        "5|1|A5FFFF,7|-1|A5FFFF,-1|-13|93889F,2|-4|9289A1,5|-11|ADFFFF,8|-7|ADFFFF,11|-4|5F95BC,15|-1|75B7DF,16|-8|7EC8E8,11|-12|634D58",
        0, 0.9, "ClanGamesRewardIcon06"
    )

    /** 源 L33578 */
    val ClanGamesRewardIcon07 = ColorSchema.parse(
        445, 199, 1110, 489, "706D66",
        "-4|-13|B0B9B7,-7|-23|AAB1AE,5|-29|A8B1AE,-2|-22|AEB6B4,1|-12|B3569B,4|-6|B4559E,9|-6|CB6BC3,9|-12|E68DE6,8|-20|DE7DD8",
        0, 0.9, "ClanGamesRewardIcon07"
    )

    /** 源 L33581 */
    val ClanGamesRewardIcon08 = ColorSchema.parse(
        445, 199, 1110, 489, "1FA9FF",
        "0|-10|2FC1FF,4|-17|D0F0FC,5|3|1EAAFF,14|-1|23B7FF,14|-6|7CDFFF,18|-13|EAF5FC,25|-8|7BCAF1,17|-26|3A4C5A,17|-31|96D2FF",
        0, 0.9, "ClanGamesRewardIcon08"
    )

    /** 源 L33584 */
    val ClanGamesRewardIcon09 = ColorSchema.parse(
        445, 199, 1110, 489, "534CEE",
        "0|-9|5486F1,6|3|4F4BF0,13|3|4F48F0,17|3|462FE8,12|-5|88CBF8,13|-10|DFEDFA,15|-12|EDF5FD,9|-32|9AD7FF,21|-29|9AD6FF",
        0, 0.9, "ClanGamesRewardIcon09"
    )

    /** 源 L33587 */
    val ClanGamesRewardIcon10 = ColorSchema.parse(
        445, 199, 1110, 489, "2CB287",
        "0|-7|89FADE,0|-14|4DD8A1,4|-16|50DDAA,10|-15|84FCE9,4|-11|83F8E0,7|-2|77FAD0,10|-4|77FBD7,9|1|116237",
        0, 0.9, "ClanGamesRewardIcon10"
    )

    /** 源 L33590 */
    val ClanGamesRewardIcon11 = ColorSchema.parse(
        445, 199, 1110, 489, "3D3038",
        "2|-4|5E4D5E,1|-8|905A5C,-2|-8|915A5C,3|-13|8C585A,2|-17|4D7EAE,-6|-14|4C80B6,-12|-11|5087C0,-16|-7|7FBFE3,-10|0|70B0CC",
        0, 0.9, "ClanGamesRewardIcon11"
    )

    /** 源 L33593 */
    val ClanGamesRewardIcon12 = ColorSchema.parse(
        445, 199, 1110, 489, "10C7FF",
        "-2|-2|55E7FF,1|-6|55F2FF,3|-9|57FFFF,5|-13|58FFFF,3|-4|13D2FF,5|-6|1BE0FF,7|-9|24F0FF,9|-11|2EFDFF,9|-1|17D4FF,-7|0|55E7FF,14|-12|40FFFF",
        0, 0.96, "ClanGamesRewardIcon12"
    )

    /** 源 L33596 */
    val ClanGamesRewardIcon13 = ColorSchema.parse(
        445, 199, 1110, 489, "812272",
        "0|-7|DF49B2,5|-12|FF5AFF,8|-16|FF28FF,13|-11|FFFFFF,12|-7|FF51FF,12|-3|FF24DF,12|1|DA22AF,10|6|712270,20|-1|F723CC,20|-9|FF24FF,16|-18|FF41FF",
        0, 0.96, "ClanGamesRewardIcon13"
    )

    /** 源 L33599 */
    val ClanGamesRewardIcon14 = ColorSchema.parse(
        445, 199, 1110, 489, "302D35",
        "-3|-9|3D2E38,0|-15|523B4D,2|-18|593F53,5|-19|453342,14|-21|6E5567,12|-15|FFFFFF,18|-13|513D4E,20|-7|3C2D3B,15|0|35303E,8|1|332F38,8|-3|3B313D,8|-6|3D2F3D",
        0, 0.96, "ClanGamesRewardIcon14"
    )

    /** 源 L33602 */
    val ClanGamesRewardIcon15 = ColorSchema.parse(
        445, 199, 1110, 489, "85D73D",
        "0|-8|7BD135,5|5|75CD35,11|0|55BD17,16|4|67C50D,17|-10|DEE9DE,26|3|558DC3,14|-30|91CEFF",
        0, 0.96, "ClanGamesRewardIcon15"
    )

    /** 源 L33605 */
    val ClanGamesRewardIcon16 = ColorSchema.parse(
        445, 199, 1110, 489, "FB3965",
        "1|-7|EB385D,6|4|E5325D,7|-2|DD325A,15|3|BD294D,21|4|9A1935,24|-2|6F5D55,15|-10|ED95BD,16|-31|92CDFE",
        0, 0.96, "ClanGamesRewardIcon16"
    )

    /** 全部奖励图标模板（源 函数232a 的 16 组奖励图标） */
    val ALL = listOf(
        ClanGamesRewardIcon01,
        ClanGamesRewardIcon02,
        ClanGamesRewardIcon03,
        ClanGamesRewardIcon04,
        ClanGamesRewardIcon05,
        ClanGamesRewardIcon06,
        ClanGamesRewardIcon07,
        ClanGamesRewardIcon08,
        ClanGamesRewardIcon09,
        ClanGamesRewardIcon10,
        ClanGamesRewardIcon11,
        ClanGamesRewardIcon12,
        ClanGamesRewardIcon13,
        ClanGamesRewardIcon14,
        ClanGamesRewardIcon15,
        ClanGamesRewardIcon16
    )

}
