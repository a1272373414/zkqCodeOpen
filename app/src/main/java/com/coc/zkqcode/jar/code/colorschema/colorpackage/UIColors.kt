@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IUIColors {
    val Reconnection: ColorSchema
    val ReconnectionOnCloudPhone: ColorSchema
    val RatingOnCloudPhone: ColorSchema
    val GreenConfirm: ColorSchema
    val CNAd: ColorSchema
    val ClanChat: ColorSchema
    val CNProsperity: ColorSchema
    val CNPuppetAd: ColorSchema
    val Achievement: ColorSchema
    val ClaimAchievement: ColorSchema
    val ReturnAwards: ColorSchema
    val BuilderBackToCamp: ColorSchema
    val MainBackToCamp: ColorSchema
    val CNBackFromAwards: ColorSchema
    val CollectChest: ColorSchema
    val EditModeWrench: ColorSchema
    val CancelEditMode: ColorSchema
    val GoldenPass: ColorSchema
    val OldShopButton: ColorSchema
    val NewShopButton: ColorSchema
    val MagicalItem: ColorSchema
    val ArrowPointingDown: ColorSchema
    val DailyLoginReward: ColorSchema
    val BuilderBaseStarBonus: ColorSchema
    val MiddleGreenButton: ColorSchema
    val ExclusiveGift: ColorSchema
    val MorePointCoupon: ColorSchema
    val TencentChildProtection: ColorSchema
    val CollectElixirCartTutorial: ColorSchema

    // Migrated from the legacy freescript UI lookup table (函数275a)
    val PlayerProfileButton: ColorSchema
    val RedX: ColorSchema
    val RedX2: ColorSchema
    val RedX3: ColorSchema
    val RedX4: ColorSchema
    val RedX5: ColorSchema
    val RedX6: ColorSchema
    val RedX7: ColorSchema
    val RedX8: ColorSchema
    val RedX9: ColorSchema

    /** 掉线弹窗（"还在吗？因为太久没有进行操作，您已断开连接。"）里的"重新载入游戏"按钮文字。 */
    val ReloadGameButton: ColorSchema

    /** 游戏启动/载入时的合规告示页（纯黑底 + 白色 SUPERCELL logo）。 */
    val GameLoadingNotice: ColorSchema

    /** 夜世界（建筑大师基地）布局"编辑模式"：右侧一列亮绿色按钮（删除模式关闭/移除全部/…/取消）。 */
    val BuilderBaseEditMode: ColorSchema
}

object UIColors : IUIColors {
    override val Reconnection = ColorSchema.parse(
        240, 195, 1030, 530, "1B1B1C", "106|-4|1B1B1C,184|8|1B1B1C,142|-121|1B1B1C,236|-139|1B1B1C,302|-142|1B1B1C,442|-13|1B1B1C,541|-48|1B1B1C,539|-91|1B1B1C,523|-135|1B1B1C", 0, 0.99, "重连1"
    )
    override val ReconnectionOnCloudPhone = ColorSchema.parse(
        240, 195, 1030, 530, "424242", "106|-4|424242,184|8|424242,142|-121|424242,236|-139|424242,302|-142|424242,442|-13|424242,541|-48|424242,539|-91|424242,523|-135|424242", 0, 0.99, "重连1"
    )
    override val RatingOnCloudPhone = ColorSchema.parse(
        249, 206, 1035, 522, "424242", "157|0|424242,314|0|424242,471|0|424242,628|0|424242,0|158|424242,157|158|424242,314|158|424242,471|158|424242,628|158|424242", 0, 0.99, "云手机打分提示"
    )
    override val GreenConfirm = ColorSchema.parse(
        577, 564, 731, 642, "7AF1D2", "25|-3|7FF3D7,45|-4|81F4D9,60|-4|81F4D9,66|5|70ECC8,67|17|53DFAB,67|23|2CCD84,45|35|30BA71,22|33|2FBF76,-1|26|2CCC83", 0, 0.9, "绿色确认"
    )
    override val CNAd = ColorSchema.parse(
        1074, 44, 1140, 110, "FFFFFF", "5|9|FFFFFF,-17|18|F1F1F1,-15|-6|F8F9FA,-6|-18|F9FAF9,8|-18|FAFAF9,15|-17|F9FAF9,19|-8|F9FAFA,18|0|F5F5F5,15|7|E5EAEF", 0, 0.9, "国服广告"
    )
    override val ClanChat = ColorSchema.parse(
        485, 301, 562, 421, "28AAF3", "5|17|28AAF3,6|35|3B8AEA,3|54|3B8AEA,-15|56|3B8AEA,-23|46|3B8AEA,-23|40|3B8AEA,-24|31|3B8AEA,-22|14|28AAF3,-19|7|28AAF3", 0, 0.9, "部落聊天框"
    )
    override val CNProsperity = ColorSchema.parse(
        596, 379, 661, 491, "28AAF3", "1|16|28AAF3,0|33|3B8AEA,-15|49|3B8AEA,-24|40|3B8AEA,-27|26|3B8AEA,-31|13|28AAF3,-34|1|28AAF3,-27|-12|28AAF3,-13|-8|28AAF3", 0, 0.9, "繁荣度聊天框"
    )
    override val CNPuppetAd = ColorSchema.parse(
        1157, 84, 1206, 131, "6EB1F9", "-9|-3|173182,-1|-11|122C6E,5|-6|19317F,9|-3|17307F,5|3|638CDE,0|5|2136A1,-2|6|2337A6,-8|-2|193085,-11|-3|173181", 0, 0.9, "垃圾皮影广告"
    )
    override val Achievement = ColorSchema.parse(
        59, 3, 92, 39, "2915E2", "-3|0|2815E4,-15|0|2915E1,-11|-19|1818FF,-9|-19|1818FF,-7|-19|1818FF,-5|-19|1818FF,-4|-19|1818FF,-2|-19|1818FF,-1|-19|1818FF", 0, 0.9, "成就红色"
    )
    override val ClaimAchievement = ColorSchema.parse(
        932, 177, 1128, 666, "70EDC9", "22|0|70EDC9,26|-7|83F5DB,89|-3|7AF1D2,105|-2|77F0CF,120|-2|77F0CF,141|13|2CCC82,130|16|2EC47B,23|15|2DC77E,5|15|2DC77E", 0, 0.9, "领取成就绿色按钮"
    )

    override val ReturnAwards = ColorSchema.parse(
        110, 44, 1173, 679, "00528D", "116|-2|00518B,-68|491|00CCF3,-78|517|00DCFA,-78|523|00DDFA,801|536|00DDFA,840|529|009BD8,846|515|008AED,840|399|00437A,839|354|004279", 0, 0.9, "回归奖励"
    )

    override val BuilderBackToCamp = ColorSchema.parse(
        553, 574, 728, 648, "8BEABD", "18|-1|8CEABE,46|-1|8CEABE,68|0|8BEABD,80|2|88E9BB,89|35|3AD48B,62|38|3AD38B,35|32|3AD48B,22|34|3AD48B,-16|29|3AD48B", 0, 0.9, "夜世界回营"
    )

    override val MainBackToCamp = ColorSchema.parse(
        554, 578, 726, 658, "79F6D9", "40|-5|83F8DD,69|-7|86F8DF,88|8|69F2D0,97|11|64EFCB,96|33|1EBB6C,79|37|1FBB6C,39|35|1FBB6C,18|35|1FBB6C,0|32|1FBC6D", 0, 0.9, "主世界回营"
    )

    override val CNBackFromAwards = ColorSchema.parse(
        62, 74, 97, 122, "37E0FA", "0|2|39E2F7,-2|3|3AE1F9,-5|13|0DA2D1,-2|16|0DA1D0,-1|18|10A3CF,2|12|0D8CBC,2|8|3AE0F8,5|8|0EA3CF,5|12|066E9E", 0, 0.9, "垃圾奖励"
    )

    override val CollectChest = ColorSchema.parse(
        547, 569, 735, 627, "88E9BB", "39|-2|8BEABD,83|-7|8EEABF,129|-5|8EEABF,130|18|3AD48B,124|26|3AD48B,107|32|39D38A,47|34|39D088,19|30|3AD48B,6|25|3AD48B", 0, 0.9, "领取宝箱"
    )
    override val EditModeWrench = ColorSchema.parse(
        1197, 285, 1257, 345, "FFFFFF", "-13|-15|ECFCF8,-1|-20|EFFDFB,4|-21|EFFDFB,13|-21|EFFDFB,18|-17|EDFDF9,22|-10|E9FAF7,6|2|FFFFFF,8|6|FFFFFF,27|5|BDDDDB", 0, 0.9, "编辑模式扳手"
    )
    override val CancelEditMode = ColorSchema.parse(
        1051, 484, 1269, 537, "FFFFFF", "3|6|FFFEF9,6|-2|FFFFFF,20|-5|7B76FF,18|-1|726DFE,21|2|6B66FD,21|15|1712E7,20|18|1611E1,21|19|1611DF,25|21|1511D9", 0, 0.9, "取消编辑模式"
    )
    override val GoldenPass = ColorSchema.parse(
        1118, 84, 1157, 124, "8381FF", "8|0|FFFFFF,16|0|111115,23|0|FFFFFF,31|0|8381FF,0|20|221EF7,8|20|FBFBFB,16|20|181715,23|20|FBFBFB,31|20|221EF7", 0, 0.9, "黄金令牌"
    )
    override val OldShopButton = ColorSchema.parse(
        1195, 19, 1261, 88, "FFFFFF", "-4|2|FAF6F6,-23|8|1A16ED,-20|-6|867FFF,-18|-15|958FFF,4|-20|958DFF,21|-7|8A82FF,14|4|1511EC,12|21|1913D3,-14|26|352DCE", 0, 0.9, "旧版商店叉叉"
    )
    override val NewShopButton = ColorSchema.parse(
        1159, 8, 1209, 57, "FAF6F6", "-3|-5|FFFFFF,-10|-5|857DFF,-5|-15|958DFF,14|-10|938BFF,13|-3|7D76FF,14|8|1612E6,3|12|1A15D1,-4|13|211BCE,-10|13|221CCF", 0, 0.9, "新版商店叉叉"
    )
    override val MagicalItem = ColorSchema.parse(
        960, 57, 1016, 110, "FBFBFB", "-14|-4|716FFE,-6|-14|8785FF,13|10|221EF2,12|-13|8785FF,13|-10|8583FF,13|-3|7A78FF,13|5|221EF7,10|17|1B18AA,3|14|221EF7", 0, 0.9, "魔法物品"
    )
    override val ArrowPointingDown = ColorSchema.parse(
        116, 92, 1124, 620, "26ACFF", "7|0|24B9FF,14|0|24BCFE,21|0|25B6FE,28|0|26A7FE,0|21|0FADFD,7|21|0AB0FC,14|21|0AB2FC,21|21|0AAEFB,28|21|10ADFE", 0, 0.92, "向下箭头"
    )
    override val DailyLoginReward = ColorSchema.parse(
        1032, 153, 1050, 172, "FBFCFD", "3|0|F3F5FA,16|6|E8E9ED,11|0|FFFFFF,14|0|B9C8DD,0|9|F2F1EF,3|9|ECF1F6,0|4|F5F6F7,11|9|FAFBFD,14|9|E9EDF1", 0, 0.9, "疼讯签到奖励"
    )
    override val BuilderBaseStarBonus = ColorSchema.parse(
        541, 522, 739, 606, "82E8B8", "39|0|82E8B8,79|0|82E8B8,119|0|82E8B8,158|0|82E8B8,0|42|3AD38A,39|42|3AD38A,79|42|3AD38A,119|42|3AD38A,158|42|3AD38A", 0, 0.9, "夜世界胜利之星"
    )

    // Middle green confirmation button (e.g. events and helper tutorials)
    override val MiddleGreenButton = ColorSchema.parse(
        577, 526, 704, 567, "86E8BA", "25|0|86E8BA,51|0|86E8BA,76|0|86E8BA,101|0|86E8BA,0|21|3AD48B,25|21|3AD48B,51|21|3AD48B,76|21|3AD48B,101|21|3AD48B", 0, 0.9, "中心绿色确认（类似活动和帮手教程）"
    )

    // Exclusive gift/offer popup indicator
    override val ExclusiveGift = ColorSchema.parse(
        1080, 113, 1119, 153, "8A83FF", "8|0|FFFFFF,16|0|FFFFFF,23|0|FFFFFF,31|0|8A83FF,12|8|FFFFFF,32|13|1611EC,13|30|2D25CD,-5|18|1A16ED,31|20|1511E8", 0, 0.9, "垃圾专属礼包"
    )

    // More coupons/points button
    override val MorePointCoupon = ColorSchema.parse(
        880, 195, 925, 240, "8B83FF", "9|0|FFFFFF,19|0|FFFFFF,18|8|FFFFFF,34|11|716BFD,42|15|2621F0,38|23|1712EB,19|23|0D0D0D,28|23|FAF6F6,37|23|1611EA", 0, 0.9, "更多点券"
    )
    override val TencentChildProtection = ColorSchema.parse(
        344, 66, 928, 235, "8ED139", "117|0|85CA2E,234|0|78C11F,351|0|7BC520,468|0|9FDF38,0|85|8BCF35,117|85|7FC627,234|85|75C01A,351|85|75C013,468|85|89CF14", 0, 0.9, "未成年守护"
    )
    override val CollectElixirCartTutorial = ColorSchema.parse(
        618, 461, 658, 542, "02A5FE", "8|0|00A8FD,16|0|00A9FD,24|0|00A9FD,32|0|00A5FD,0|41|22AFFF,8|41|21B8FF,16|41|21BDFF,24|41|21BBFF,32|41|22B2FF", 0, 0.9, "领圣水车教程"
    )

    // --- Migrated from the legacy freescript UI lookup table (函数275a) ---
    // The legacy script matches on the 720x1280 portrait framebuffer while this project
    // matches on 1280x720 landscape screenshots, so every region and offset point was
    // rotated by 90 degrees:  x' = y, y' = 719 - x,  offset (dx, dy) -> (dy, -dx).
    // Player profile entry
    override val PlayerProfileButton = ColorSchema.parse(
        1096, 36, 1139, 78, "2722F0-101010",
        "8|0|FBFBFB-101010,18|1|FBFBFB-101010,25|0|2722F0-101010,26|-12|807DF9-101010,18|-13|FFFFFF-101010,8|-11|FFFFFF-101010,0|-11|7B78F8-101010",
        0, 0.9, "个人信息"
    )
    // Red X close button. In the current game version this is a small red "X" on a white
    // round button at the top-right. Note: ColorSchema colors are BGR-ordered, so the red
    // pixel (r=240,g=34,b=39) is written as "2722F0". The legacy freescript's "1511EC" was
    // also red (BGR), but its offset points no longer match the current button, so this is
    // re-derived from the real screenshots.
    override val RedX = ColorSchema.parse(
        950, 10, 1270, 230, "2722F0",
        "-1|-1|2722F0,1|-1|2722F0,-1|1|2722F0,1|1|2722F0,0|1|2722F0,0|-1|2722F0,-1|0|2722F0,1|0|2722F0",
        0, 0.85, "红x"
    )
    // RedX2-9 below are retained from the legacy freescript as extra candidate anchors; the
    // current game's close button is covered by the primary RedX above.
    // Fallback variant 2 (the legacy script tries variants in order)
    override val RedX2 = ColorSchema.parse(
        657, 3, 1273, 329, "FBFBFB-101010",
        "0|-17|FFFFFF-101010,11|0|FBFBFB-101010,12|-18|FFFFFF-101010,-10|5|2621E3-101010,-9|-20|8D8EF3-101010,-6|-9|8D8EF3-101010,-8|-3|2722F0-101010,22|4|2621E6-101010,20|-3|2722F0-101010,20|-8|8C8EF2-101010,21|-21|8D8EF3-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 3 (the legacy script tries variants in order)
    override val RedX3 = ColorSchema.parse(
        657, 3, 1273, 329, "211DEC-101010",
        "0|-4|171515-101010,1|-10|F9F9F9-101010,-7|-14|211DF5-101010,-7|-22|7976FD-101010,-9|-34|8585FE-101010,18|0|211DEC-101010,17|-5|151413-101010,15|-10|F9F9F9-101010,15|-30|FFFFFF-101010,18|-37|8585FF-101010,26|-22|7976FD-101010,28|-12|211DF5-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 4 (the legacy script tries variants in order)
    override val RedX4 = ColorSchema.parse(
        657, 3, 1273, 329, "231FEA-101010",
        "-1|-3|171514-101010,-1|-7|FBFBFB-101010,-9|-10|221EF7-101010,-6|-15|7A78FF-101010,13|1|2320E6-101010,9|-3|181715-101010,9|-8|FBFBFB-101010,18|-10|221EF7-101010,14|-15|7A78FF-101010",
        0, 0.9, "红x"
    )
    // Fallback variant 5 (the legacy script tries variants in order)
    override val RedX5 = ColorSchema.parse(
        657, 3, 1273, 329, "FFFFFF-101010",
        "-9|0|0300C1-101010,-7|-7|7A77EA-101010,-8|-14|7D77EF-101010,18|6|0100B8-101010,9|0|FFFFFF-101010,16|-6|7A77E9-101010,5|-7|FFFFFF-101010,17|-19|7E77F2-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 6 (the legacy script tries variants in order)
    override val RedX6 = ColorSchema.parse(
        657, 3, 1273, 329, "FAF6F6-101010",
        "13|0|FAF6F6-101010,-1|5|0D0D0D-101010,14|5|0D0D0D-101010,-7|-4|1511EC-101010,-7|-10|7972FF-101010,21|-3|1511EC-101010,20|-11|7C75FF-101010,-8|9|1A15D1-101010,8|10|1D18CE-101010,19|9|1A15D1-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 7 (the legacy script tries variants in order)
    override val RedX7 = ColorSchema.parse(
        657, 3, 1273, 329, "FBFBFB-101010",
        "12|0|FBFBFB-101010,-1|4|171614-101010,11|4|181715-101010,0|7|2520DD-101010,12|7|2520DD-101010,-5|-5|2722F0-101010,-5|-10|6460F6-101010,18|-4|2722F0-101010,18|-11|6B68F7-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 8 (the legacy script tries variants in order)
    override val RedX8 = ColorSchema.parse(
        657, 3, 1273, 329, "F9F9F9-101010",
        "9|0|F9F9F9-101010,0|3|272727-101010,10|3|0F0F0D-101010,-6|-3|2521F1-101010,-5|-9|8C8EF3-101010,14|-2|2521ED-101010,15|-9|8C8EF2-101010,0|6|251FDC-101010,10|6|251EDC-101010",
        2, 0.93, "红x"
    )
    // Fallback variant 9 (the legacy script tries variants in order)
    override val RedX9 = ColorSchema.parse(
        657, 3, 1273, 329, "ffffff-101010",
        "-5|-5|948cff-101010,11|5|867fff-101010,7|9|7c75ff-101010,14|18|1712ec-101010,8|17|1511ec-101010,0|19|faf6f6-101010,10|30|1611d9-101010,-1|31|1611d5-101010",
        0, 0.9, "红x"
    )
    // 掉线弹窗（"还在吗？…您已断开连接。"）中的"重新载入游戏"按钮：青色文字 + 12 个同色偏移点，
    // 由真实截图（I:\coc\游戏截图\补充5\游戏掉线弹窗-*.png）派生；已校验在训练/法术/攻城器选兵界面上零误命中。
    override val ReloadGameButton = ColorSchema.parse(
        23, 429, 1279, 693, "C4CB80",
        "41|19|C4CB80,38|22|C4CB80,20|19|C4CB80,20|1|C4CB80,11|13|C4CB80,-7|19|C4CB80,-10|19|C4CB80,-19|19|C4CB80,29|1|C3CA80,44|16|C3CA7F,-1|1|C0C77E,26|16|BFC67E",
        0, 0.9, "重新载入游戏"
    )

    /**
     * 游戏启动/载入的合规告示页（"健康游戏忠告" + SUPERCELL logo）：整屏纯黑底，仅少量白色文字。
     * 特征 = 大面积纯黑（偏移点都落在背景黑处），据此与正常游戏画面区分。
     * 命中后应"等待载入完成"，绝不能按返回键（按返回会弹出游戏自己的退出确认框）。
     */
    override val GameLoadingNotice = ColorSchema.parse(
        0, 0, 1279, 719, "000000",
        "150|150|000000,640|150|000000,1100|150|000000,150|520|000000,640|520|000000,1100|520|000000,640|700|000000",
        0, 0.95, "游戏载入中(黑屏)"
    )

    /**
     * 夜世界（建筑大师基地）布局编辑模式：右侧一列亮绿色按钮，采样自真实截图
     * （RGB 9DDA45，BGR 即 45DA9D）。命中即说明当前停在编辑模式，应回退为"夜世界"而非未知页面。
     */
    override val BuilderBaseEditMode = ColorSchema.parse(
        1085, 44, 1205, 68, "45DA9D",
        "0|0|45DA9D,25|0|45DA9D,50|0|45DA9D,75|0|45DA9D",
        0, 0.95, "夜世界编辑模式"
    )
}
