@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IMainBaseAttackColors {
    val SetBaseIcon: ColorSchema
    val InnerSetBase: ColorSchema
    val SearchOpponents: ColorSchema
    val AttackButton: ColorSchema
    val NextOpponent: ColorSchema
    val GoldColor: ColorSchema
    val ElixirColor: ColorSchema
    val DarkElixirColor: ColorSchema
    val DarkElixirIcon: ColorSchema
    val InsufficientGold: ColorSchema
    val DragonAtDeploymentBar: ColorSchema

    // Troop deploy bar color variants
    val BarbarianAtDeploymentBar: ColorSchema
    val BarbarianAtDeploymentBar2: ColorSchema
    val GiantAtDeploymentBar: ColorSchema
    val GiantAtDeploymentBar2: ColorSchema
    val ArcherAtDeploymentBar: ColorSchema
    val ArcherAtDeploymentBar2: ColorSchema
    val ArcherAtDeploymentBar3: ColorSchema
    val ArcherAtDeploymentBar4: ColorSchema
    val DragonAtDeploymentBar2: ColorSchema

    // Battle and hero colors
    val EndBattle: ColorSchema
    val KingBarbarian: ColorSchema
    val QueenArcher: ColorSchema
    val QueenArcher2: ColorSchema
    val QueenArcher3: ColorSchema
    val MinionPrince: ColorSchema
    val MinionPrince2: ColorSchema
    val GrandWarden: ColorSchema
    val GrandWarden2: ColorSchema
    val GrandWarden3: ColorSchema
    val GrandWarden4: ColorSchema
    val RoyalChampion: ColorSchema
    val RoyalChampion2: ColorSchema
    val DragonDuke: ColorSchema
    val TroopColorAtDeploymentBar: ColorSchema
    val SpellColorAtDeploymentBar: ColorSchema
    val SuperTroopColorAtDeploymentBar: ColorSchema
    val SpecialTroopColorAtDeploymentBar: ColorSchema

    // 源 cocfz-apk-test 部署栏色特征（90°旋转迁移，live_bar_b1 实测命中）
    val ReviveSpellAtDeploymentBar: ColorSchema
    val PrinceAtDeploymentBar: ColorSchema
    val TotemAtDeploymentBar: ColorSchema
    val DragonRiderAtDeploymentBar: ColorSchema
    val DragonAtDeploymentBar3: ColorSchema
    val QueenArcherLegacy: ColorSchema
    val GrandWardenLegacy: ColorSchema
    val MinionPrinceLegacy: ColorSchema

    val WaitForBattle: ColorSchema
    val BattlePage: ColorSchema

    // Event reward popup ("选择一项奖励！") title ribbon — battle variant (ribbon y:90-132)
    val RewardPopupTitle: ColorSchema

    // Event reward popup title ribbon — settlement variant (ribbon shifted down, y:175-217)
    val RewardPopupTitle2: ColorSchema

    // Migrated from the legacy freescript UI lookup table (函数275a) — battle screens
    val GiveUpButton: ColorSchema
    val ClanGamesEntry: ColorSchema
    val VictoryStar: ColorSchema
    val VictoryStar2: ColorSchema
    val VictoryStar3: ColorSchema
    // 源 cocfz-apk-test `器列表`(找机器) / `找援兵`：攻城机器 9 种 + 部落城堡援兵
    // （90°旋转迁移：区域 (9,1,132,1279)->(1,587,1279,710)，偏移 (dx,dy)->(dy,-dx)）
    val SiegeChariot: ColorSchema
    val SiegeChariot2: ColorSchema
    val SiegeAirship: ColorSchema
    val SiegeAirship2: ColorSchema
    val SiegeWarBall: ColorSchema
    val SiegeWarBall2: ColorSchema
    val SiegeBarracks: ColorSchema
    val SiegeBarracks2: ColorSchema
    val SiegeLogLauncher: ColorSchema
    val SiegeLogLauncher2: ColorSchema
    val SiegeFlameThrower: ColorSchema
    val SiegeFlameThrower2: ColorSchema
    val SiegeDrill: ColorSchema
    val SiegeDrill2: ColorSchema
    val SiegeTroopLauncher: ColorSchema
    val SiegeTroopLauncher2: ColorSchema
    val SiegeSkyChariot: ColorSchema
    val SiegeSkyChariot2: ColorSchema
    val ClanCastleTroop: ColorSchema
    val ClanCastleTroop2: ColorSchema
    val ClanCastleTroop3: ColorSchema
    val ClanCastleTroop4: ColorSchema
    val ClanCastleTroop5: ColorSchema
}

object MainBaseAttackColors : IMainBaseAttackColors {
    override val SetBaseIcon = ColorSchema.parse(
        546, 78, 573, 118, "1AABFE", "5|0|1AB4FE,11|0|1AB8FE,16|0|1AB5FE,21|0|1AAEFE,0|20|16B4FA,5|20|12B6FA,11|20|11B7FB,16|20|11B6FB,21|20|10B3FA", 0, 0.93, "布阵按钮"
    )
    override val InnerSetBase = ColorSchema.parse(
        341, 46, 504, 146, "4B5261", "33|0|4B5261,66|0|4B5261,98|0|4B5261,131|0|4B5261,0|50|C6D2D9,33|50|C6D2D9,66|50|C6D2D9,98|50|C6D2D9,131|50|C6D2D9", 0, 0.97
    )
    override val SearchOpponents = ColorSchema.parse(
        89, 501, 350, 570, "2DADF9", "26|-9|3CB7FC,14|13|2CADF9,4|32|2CADF9,202|-5|2FB0FA,226|0|2DADF9,236|13|2CADF9,237|20|2CADF9,227|32|2CADF9,211|45|2CADF9", 0, 0.95, "搜索对手"
    )
    override val AttackButton = ColorSchema.parse(
        1045, 624, 1245, 662, "9BFDCF", "13|-5|A1FED2,24|-4|A1FED2,16|1|9AFCCE,8|20|4EE79F,121|-5|A1FED2,140|-3|A0FED1,146|3|97FCCC,132|20|4EE79F,137|20|4EE79F", 0, 0.9, "进攻！"
    )
    // NextOpponent: "下一个 X金币" button, bottom-right of the match-found screen (1280x720)
    // Real pixel sampling (2026-09-13): button bounds x:1065-1267, y:467-556
    // Two-tone gradient: top band golden #FFCB4C, bottom band orange #F57626 (BGR 2676F5)
    // Offsets use three stable horizontal bands (y=479/517/549) on the left/middle of the
    // button only — the right side (coin icon + dynamic gold cost digits) is never sampled.
    override val NextOpponent = ColorSchema.parse(
        1063, 465, 1270, 558, "2676F5", "70|12|4CCBFF,120|12|4CCBFF,170|12|4CCBFF,70|50|2676F5,120|50|2676F5,70|82|2274F5,120|82|2274F5", 0, 0.85, "下一个对手"
    )
    override val GoldColor = ColorSchema.parse(
        998, 19, 1215, 136, "0DC0E7", "0|1|0DC0E7,0|2|0DC0E7,0|3|0DC0E7,0|4|0DC0E7,0|5|0DC0E7,0|6|0DC0E7,0|7|0DC0E7", 0, 0.97,
    )
    override val ElixirColor = ColorSchema.parse(
        998, 19, 1215, 136, "C027C0", "0|1|C027C0,0|2|C027C0,0|3|C027C0,0|4|C027C0,0|5|C027C0,0|6|C027C0,0|7|C027C0", 0, 0.97,
    )
    override val DarkElixirColor = ColorSchema.parse(
        1060, 130, 1210, 200, "330D27", "0|1|330D27,0|2|330D27,0|3|330D27,0|4|330D27,0|5|330D27,0|6|330D27,0|7|330D27", 0, 0.97
    )
    override val DarkElixirIcon = ColorSchema.parse(
        1214, 136, 1255, 233, "4A3445", "5|0|443241,9|0|554050,13|0|695162,18|0|685062,0|13|342E37,5|13|38303C,9|13|3A313D,13|13|3B313E,18|13|3B303D", 0, 0.9,
    )
    override val InsufficientGold = ColorSchema.parse(
        679, 440, 699, 470, "79F7DD", "4|0|7BF8DE,8|0|80F9E2,12|0|87FCE7,16|0|8CFFEB,0|15|96FEE5,4|15|83FAD7,8|15|71F5CB,12|15|8BFDE0,16|15|7FFCDB", 0, 0.9, "搜索金币不足"
    )
    override val DragonAtDeploymentBar = ColorSchema.parse(
        85, 589, 1189, 717, "DB5C6E", "14|6|4F2D8C,27|10|EB6B79,24|22|BF4F5E,15|30|2E268C,25|37|5537A9,27|45|5C336D,25|47|552268,2|23|7393F7,-10|19|883542", 0, 0.9, "部署飞龙"
    )

    // Troop deploy bar color variants
    override val BarbarianAtDeploymentBar = ColorSchema.parse(
        85, 589, 1189, 717, "2FB2F1", "9|17|5C96F9,16|24|5AE6FE,8|36|202780,8|23|4DD4FC,9|12|223E85,4|8|2EA2DC,9|8|2EA6E2,12|24|52DCFD,24|36|62E1FD", 0, 0.9, "部署野蛮人"
    )

    // Additional barbarian deploy bar color variant
    override val BarbarianAtDeploymentBar2 = ColorSchema.parse(
        85, 589, 1189, 717, "36B6F1", "8|0|3FCCFB,16|0|48C5F8,18|44|689BF1,37|47|5877C1,0|16|868FDA,8|16|5A89F3,16|16|619DFB,24|16|689DF1,32|16|73B2FB", 0, 0.9, "部署野蛮人2"
    )
    override val GiantAtDeploymentBar = ColorSchema.parse(
        85, 589, 1189, 717, "82B5FC", "13|15|6A9FF3,24|8|77AAF8,24|-12|4A71B3,8|-21|56A1FC,-15|-21|3C91FC,-23|-2|465B92,-12|3|5983D1,-3|12|3E5497,22|31|6CACF8", 0, 0.9, "部署巨人"
    )

    // Additional giant deploy bar color variant
    override val GiantAtDeploymentBar2 = ColorSchema.parse(
        85, 589, 1189, 717, "154AA2", "10|0|0944A8,21|0|3454A8,29|40|7AAFF9,42|31|669AF0,0|16|608EE0,10|16|5463A2,21|16|A3C8FD,31|16|6EA0F3,41|16|76ACF6", 0, 0.9, "部署巨人2"
    )
    override val ArcherAtDeploymentBar = ColorSchema.parse(
        85, 589, 1189, 717, "662DBC", "5|12|91A8FB,4|24|5368AD,-8|21|6070B9,-22|5|210E4D,-18|-14|7D3BBC,-8|-18|6F30C0,4|-19|672CC1,7|-14|431A8A,6|-6|331270", 0, 0.9, "部署弓箭手"
    )

    // Additional archer deploy bar color variant
    override val ArcherAtDeploymentBar2 = ColorSchema.parse(
        85, 589, 1189, 717, "8E43C3", "9|0|6D2DB9,17|0|662BBA,25|0|662DBB,20|37|431D87,24|58|199D7D,9|15|535A84,17|15|481A75,25|15|652BBD,34|15|672BC2", 0, 0.9, "部署弓箭手2"
    )

    // Third archer deploy bar color variant
    override val ArcherAtDeploymentBar3 = ColorSchema.parse(
        85, 589, 1189, 717, "8A40B3", "9|0|7633BA,18|0|6329BD,26|0|652CBD,35|0|692BC3,0|17|3F1A81,9|17|6B71AD,18|17|2B3665,26|17|5D65AF,35|17|566AB7", 0, 0.9, "部署弓箭手3"
    )

    // Fourth archer deploy bar color variant
    override val ArcherAtDeploymentBar4 = ColorSchema.parse(
        85, 589, 1189, 717, "34125E", "9|0|8E44C4,18|0|632CBD,26|0|652CBD,35|0|682CC6,0|17|29105A,9|17|6069A2,18|17|34426E,26|17|717BCB,35|17|7A93E9", 0, 0.9, "部署弓箭手4"
    )

    // Dragon deploy bar color variant
    override val DragonAtDeploymentBar2 = ColorSchema.parse(
        85, 589, 1189, 717, "C75161", "9|0|B86A75,18|0|562D87,27|26|6035E8,30|44|C0505B,0|12|2A0578,9|12|6A243A,18|12|712C4E,27|12|D35868,36|12|AE4857", 0, 0.9, "部署飞龙2"
    )

    // Battle and hero colors
    override val EndBattle = ColorSchema.parse(
        114, 526, 152, 556, "5F5DF4", "7|0|5F5DF4,15|0|5F5DF4,23|0|5F5DF4,30|0|5F5DF4,0|15|0E0DCE,7|15|0E0DCE,15|15|0E0DCE,23|15|0E0DCE,30|15|0E0DCE", 0, 0.9,
    )
    override val KingBarbarian = ColorSchema.parse(
        80, 590, 1200, 720, "24388B", "8|0|5272BF,16|0|6284D8,23|0|729AEF,31|0|44A7EA,0|14|283A6C,8|14|213365,16|14|1D2E5C,23|14|1A2954,31|14|1F2D65", 0, 0.9, "野蛮人之王"
    )
    override val QueenArcher = ColorSchema.parse(
        80, 590, 1200, 720, "DAB9DA", "9|0|83AAF6,19|0|883044,28|0|AC3F66,37|0|B34169,0|15|1C0814,9|15|253B6F,19|15|8A2E4E,28|15|37111F,37|15|872D4E", 0, 0.9, "弓箭女皇"
    )

    // Second queen archer deploy bar color variant
    override val QueenArcher2 = ColorSchema.parse(
        80, 590, 1200, 720, "576EB9", "11|0|8898F2,22|0|5F87E0,32|0|AB3C64,43|0|B24169,0|11|6E82B3,11|11|304986,22|11|913457,32|11|A23B60,43|11|6D253E", 0, 0.9, "弓箭女皇2"
    )

    // Third queen archer deploy bar color variant
    override val QueenArcher3 = ColorSchema.parse(
        80, 590, 1200, 720, "6C80C9", "9|0|8098F2,19|0|6991EE,29|0|A53B62,38|0|A74366,0|15|605D84,9|15|273E7C,19|15|8D3355,29|15|9F3A60,38|15|A33A61", 0, 0.9, "弓箭女皇3"
    )
    override val MinionPrince = ColorSchema.parse(
        80, 590, 1200, 720, "1C1B1B", "8|0|82B2DE,17|0|FFF77C,25|0|E2B22D,33|0|CE9C0E,0|11|221201,8|11|3F3322,17|11|311D05,25|11|965A00,33|11|8B5810", 0, 0.9, "亡灵王子"
    )

    // Second minion prince deploy bar color variant
    override val MinionPrince2 = ColorSchema.parse(
        80, 590, 1200, 720, "A47033", "9|0|433F40,18|0|98D5FF,27|0|FDD44B,36|0|DAA700,0|13|1E0E01,9|13|2E1B01,18|13|574712,27|13|1D0000,36|13|804D07", 0, 0.9, "亡灵王子2"
    )
    override val GrandWarden = ColorSchema.parse(
        80, 590, 1200, 720, "57054A", "11|0|742377,23|0|831B79,35|0|A264C8,46|0|D14DC9,0|11|922A82,11|11|A0419C,23|11|69125F,35|11|AF29A4,46|11|410E3C", 0, 0.9, "大守护者"
    )
    override val GrandWarden2 = ColorSchema.parse(
        80, 590, 1200, 720, "882673", "11|0|902086,23|0|6D1162,35|0|BC10AC,46|0|C234B7,0|11|881E71,11|11|8F2D88,23|11|9F2494,35|11|C843C0,46|11|CD47C5", 0, 0.9, "大守护者2"
    )

    // Third grand warden deploy bar color variant
    override val GrandWarden3 = ColorSchema.parse(
        80, 590, 1200, 720, "771262", "11|0|9439A1,23|0|651360,35|0|81BAFB,46|0|CB43C0,0|14|942C84,11|14|993E99,23|14|1E030F,35|14|C941C1,46|14|CE45C6", 0, 0.9, "大守护者3"
    )

    // Fourth grand warden deploy bar color variant
    override val GrandWarden4 = ColorSchema.parse(
        80, 590, 1200, 720, "8C2473", "10|0|85318E,21|0|4A0B41,31|0|BA30B0,41|0|5A105A,0|10|912580,10|10|9F419D,21|10|75166D,31|10|C842C0,41|10|C845C0", 0, 0.9, "大守护者4"
    )
    override val RoyalChampion = ColorSchema.parse(
        80, 590, 1200, 720, "467BDC", "7|0|133070,15|0|0F2D62,22|0|23280B,29|0|4B5D71,0|14|3871D3,7|14|112A5B,15|14|386BCB,22|14|3D63C2,29|14|2D3C74", 0, 0.9, "飞盾战神"
    )

    // Second royal champion deploy bar color variant
    override val RoyalChampion2 = ColorSchema.parse(
        80, 590, 1200, 720, "497DDE", "8|0|628BF1,16|0|2F65C4,24|0|2B65C3,32|0|2B58A9,0|11|366ED0,8|11|123069,16|11|3368C8,24|11|395FBA,32|11|394173", 0, 0.9, "飞盾战神2"
    )
    override val DragonDuke = ColorSchema.parse(
        80, 590, 1200, 720, "000023", "7|0|1A134C,15|0|120E53,23|0|070828,30|0|000021,0|15|58506E,7|15|F2F8FF,15|15|303B7F,23|15|A5A3DB,30|15|E9ECFF", 0, 0.9, "飞龙公爵"
    )
    override val TroopColorAtDeploymentBar = ColorSchema.parse(
        80, 590, 1200, 720, "D08E4C", "5|0|D18F4D,11|0|D1904E,16|0|D19150,21|0|D29250,0|5|BE8444,5|5|BF8544,11|5|C08646,16|5|C18848,21|5|C1894A", 0, 0.95, "部队颜色"
    )
    override val SpellColorAtDeploymentBar = ColorSchema.parse(
        80, 590, 1200, 720, "DA5372", "3|0|DA5372,6|0|DA5372,9|0|DA5372,12|0|DA5372,0|7|BF475E,3|7|BF485F,6|7|BF475E,9|7|BF475E,12|7|BE485E", 0, 0.95, "法术颜色"
    )
    override val SuperTroopColorAtDeploymentBar = ColorSchema.parse(
        80, 590, 1200, 720, "3E38D1", "3|0|3E38D2,6|0|3E38D2,9|0|3E38D2,12|0|3E38D2,0|5|3832B4,3|5|3832B5,6|5|3832B5,9|5|3932B5,12|5|3832B6", 0, 0.95, "超级兵颜色"
    )
    override val SpecialTroopColorAtDeploymentBar = ColorSchema.parse(
        80, 590, 1200, 720, "F7E2D1", "3|0|F7E2D1,6|0|F7E2D1,9|0|F7E2D1,12|0|F7E2D1,0|7|DDC5B2,3|7|DDC5B3,6|7|DFC6B3,9|7|DEC7B3,12|7|DEC6B3", 0, 0.95, "活动兵颜色"
    )

    // === 源 cocfz-apk-test 部署栏色特征（90°旋转迁移，live_bar_b1 实测命中）===
    override val ReviveSpellAtDeploymentBar = ColorSchema.parse(
        1, 587, 1279, 710, "809BB3", "0|-9|F2FFFF,-6|0|66839A,-11|-1|3AB1FF,-13|-20|8C99A4,-8|-20|B1C9DF,10|-21|B6C5D0,14|-16|567482,16|2|092847,16|8|082947", 0, 0.9, "复苏法术（legacy 函数141a）"
    )
    override val PrinceAtDeploymentBar = ColorSchema.parse(
        1, 587, 1279, 710, "986E1F", "4|-8|FFFF82,30|-1|FFD53E,31|-7|FFFF5C,47|5|602E00,10|-12|110100,8|-20|FFFC5E,47|-1|683000", 0, 0.9, "王子（legacy 函数164a）"
    )
    override val TotemAtDeploymentBar = ColorSchema.parse(
        1, 587, 1279, 710, "BCA584", "0|-9|C3AB8B,-10|0|C7AE91,-10|-8|C7B093,-21|-15|9B6056,-21|-27|AF5E62,12|7|CE846C,10|-4|A46759,13|-19|975D54,11|-28|B55D65,0|-25|1E76ED", 0, 0.9, "图腾（legacy 函数141a）"
    )
    override val DragonRiderAtDeploymentBar = ColorSchema.parse(
        1, 587, 1279, 710, "CDE4EA", "2|5|414ADB,-4|-4|040505,-12|-18|07080A,-20|-20|717C84,-25|-27|1D1D1F,-12|-34|9CA5A9,25|29|1A58B8,27|11|011567,17|0|000E45", 0, 0.9, "龙骑（legacy 函数140a）"
    )
    override val DragonAtDeploymentBar3 = ColorSchema.parse(
        1, 587, 1279, 710, "6036E7", "11|-9|70303C,-1|-15|D85C6D,2|-20|E86078,-33|-29|D05868,-22|-41|F07080,-11|-47|D9707A,5|-46|F4F7F8", 0, 0.9, "飞龙（legacy 函数140a，补充 variant）"
    )
    override val QueenArcherLegacy = ColorSchema.parse(
        1, 587, 1279, 710, "903858", "1|-2|923958,3|-20|923558,6|-31|9F3A60,9|-15|822C4F,14|-26|98385D,26|-2|7A95E6,23|-6|7393E7,-7|-46|608CE4,27|-34|AD3C60", 0, 0.9, "女皇（legacy 函数163a，补充 variant）"
    )
    override val GrandWardenLegacy = ColorSchema.parse(
        1, 587, 1279, 710, "4B0D3D", "13|-1|8E2677,19|2|8C2875,23|-3|88267C,46|8|A32898,44|2|9C2A94,47|-4|C034B5,43|-48|72B1FA,51|-45|80C0FD,52|-37|B831AF,68|-5|7C2579", 0, 0.9, "守卫（legacy 函数165a，补充 variant）"
    )
    override val MinionPrinceLegacy = ColorSchema.parse(
        1, 587, 1279, 710, "1B1760", "-3|0|1C1758,-8|-4|02001A,-16|-5|5F5684,3|-11|2A2FD1,16|-9|5057FF,29|-3|D1D6FF,19|13|E5E8FF,22|16|CBA4B0,19|3|00004F", 0, 0.85, "公爵（legacy 函数，补充 variant）"
    )

    // Indicator shown when attack must wait (e.g. war cooldown)
    override val WaitForBattle = ColorSchema.parse(
        70, 480, 525, 585, "9D9D9D", "-8|10|9D9D9D,5|6|9D9D9D,13|6|9D9D9D,20|22|9D9D9D,108|18|9D9D9D,36|25|9D9D9D,22|35|9D9D9D,9|37|9D9D9D,-8|39|9D9D9D", 0, 0.9, "进攻需等待"
    )
    override val BattlePage = ColorSchema.parse(
        92, 198, 712, 455, "83BEFF", "23|0|A5BECF,46|0|4C3D57,68|0|29478F,91|0|5567A5,0|30|7B7C7C,23|30|254154,46|30|61B2FD,68|30|76BBFD,91|30|485992", 0, 0.9, "对战页面"
    )

    // Event reward popup title ribbon "选择一项奖励！" (1280x720), battle variant.
    // Real pixel sampling (2026-09-13): ribbon top edge y:90-132, left body edge x≈408.
    // Anchor strip x:410-426 hits the ribbon top edge; all offset points stay left of the
    // white title text (x≥500) and were verified on two battle screenshots (similarity 0.85).
    override val RewardPopupTitle = ColorSchema.parse(
        410, 86, 426, 140, "2C2C9F", "42|8|26268C,10|20|2D2DA2,42|20|2C2CA2,74|20|2C2CA1,10|32|292995,42|32|292996,74|32|292996", 0, 0.85, "选择一项奖励-对战"
    )

    // Same title ribbon on the settlement screen: shifted down (top edge y:175-217).
    // Offsets verified on the settlement screenshot.
    override val RewardPopupTitle2 = ColorSchema.parse(
        410, 170, 426, 222, "2C2C9F", "42|8|21217A,74|8|282893,10|20|21217C,74|20|2B2C9F,74|32|292991", 0, 0.85, "选择一项奖励-结算"
    )

    // --- Migrated from the legacy freescript UI lookup table (函数275a) ---
    // The legacy script matches on the 720x1280 portrait framebuffer while this project
    // matches on 1280x720 landscape screenshots, so every region and offset point was
    // rotated by 90 degrees:  x' = y, y' = 719 - x,  offset (dx, dy) -> (dy, -dx).
    // Give up / surrender button shown during a battle
    override val GiveUpButton = ColorSchema.parse(
        11, 502, 223, 572, "635dfa-101010",
        "-56|-4|645dfc-101010,-57|14|5d5dec-101010,-57|20|0e0dcf-101010,-53|28|0e0dd3-101010,-16|28|0e0dd3-101010,13|28|0e0dd3-101010",
        0, 0.9, "放弃按钮"
    )
    // Clan games (weekend event) entry. Re-derived: the entry icon now sits at the top-right
    // of the event/friendly-battle config screen (found at ~1143,136 on 友谊战-配置页).
    override val ClanGamesEntry = ColorSchema.parse(
        1090, 90, 1200, 220, "1D21E4-101010",
        "10|0|2121E5-101010,5|-2|1F1DEC-101010,3|-17|F6F9F6-101010,7|-17|F6F9F6-101010,4|-20|FFFFFF-101010,-21|-20|1E1DF5-101010,30|-15|1E1DF5-101010",
        0, 0.9, "竞赛界面"
    )
    // Victory star on the battle result screen
    override val VictoryStar = ColorSchema.parse(
        232, 300, 673, 562, "03BAF2-101010",
        "4|-5|04C3FE-101010,9|-10|10D6FF-101010,13|-15|1BE8FF-101010,-5|-4|48DAFF-101010,1|-11|4AE7FF-101010,5|-17|4AF7FF-101010,9|-21|4CFFFF-101010",
        0, 0.96, "胜利之星"
    )
    // Fallback variant 2 (the legacy script tries variants in order)
    override val VictoryStar2 = ColorSchema.parse(
        537, 425, 742, 706, "20C074",
        "-1|-63|90FBE4,-29|-1|20BF72,-30|-64|92FBE4,-59|0|20C074,-60|-63|90FBE4,-85|-2|20C074,-78|-60|8CF9E2,40|-1|20BF72,42|-61|8DFAE2,70|-2|1FBD70,67|-64|92FBE5,77|-2|1FBE70,78|-63|90FBE4",
        0, 0.9, "胜利之星"
    )
    // Fallback variant 3 (the legacy script tries variants in order)
    override val VictoryStar3 = ColorSchema.parse(
        335, 557, 584, 681, "20C074",
        "-1|-63|90FBE4,-29|-1|20BF72,-30|-64|92FBE4,-59|0|20C074,-60|-63|90FBE4,-85|-2|20C074,-78|-60|8CF9E2,40|-1|20BF72,42|-61|8DFAE2,70|-2|1FBD70,67|-64|92FBE5,77|-2|1FBE70,78|-63|90FBE4",
        0, 0.9, "胜利之星"
    )

    // 源 cocfz-apk-test `器列表`(找机器) / `找援兵`：攻城机器 9 种 + 部落城堡援兵
    // （90°旋转迁移：区域 (9,1,132,1279)->(1,587,1279,710)，偏移 (dx,dy)->(dy,-dx)）
    override val SiegeChariot = ColorSchema.parse(
        1, 587, 1279, 710, "182755", "-1|-11|D8A870,20|0|1E2C98,19|-6|235098,28|-30|767B7E,51|-4|20478D,49|11|1B407D,58|3|25467C,36|10|172488,39|-6|234D94", 2, 0.9, "攻城器-战车（legacy 识别机器）"
    )
    override val SiegeChariot2 = ColorSchema.parse(
        1, 587, 1279, 710, "1E4386", "20|-34|767A7E,30|-18|2944E1,31|-2|1E309E,30|9|182688,13|7|18288D,52|7|203C6B,41|-34|696C70,-10|-14|D8A973,51|-13|D8A873", 2, 0.9, "攻城器-战车（legacy 识别机器）"
    )
    override val SiegeAirship = ColorSchema.parse(
        1, 587, 1279, 710, "404AC8", "-24|-7|878590,-17|-24|282730,8|-38|282C38,23|-41|9A9CB1,28|-33|E0B07A,24|-21|3A49D0,16|-3|4358E0,22|-1|3B51E0,42|-2|405C90", 2, 0.9, "攻城器-飞艇（legacy 识别机器）"
    )
    override val SiegeAirship2 = ColorSchema.parse(
        1, 587, 1279, 710, "404BCD", "-2|-25|7C74E4,-21|-11|A999F0,-20|-25|282730,7|-41|282D40,23|-43|989AAD,24|-19|3848D0,15|-3|4357E6,34|-11|3848D6,42|-10|465F96", 2, 0.9, "攻城器-飞艇（legacy 识别机器）"
    )
    override val SiegeWarBall = ColorSchema.parse(
        1, 587, 1279, 710, "343AA8", "-3|-7|3844BE,1|-15|3340C8,18|-33|5877FF,24|-33|6084FF,41|-17|60A0FF,43|-8|5490FF,35|0|3D5BFF,37|-29|5D68FF,50|-10|D4A46C", 2, 0.9, "攻城器-战球（legacy 识别机器）"
    )
    override val SiegeWarBall2 = ColorSchema.parse(
        1, 587, 1279, 710, "3850F8", "1|-7|405BFF,11|-6|4059FF,14|5|4052FF,19|-16|5691FF,17|-22|5C9EFF,-7|-43|5F78FF,-25|-27|3947DD,-32|-15|4048C0,-27|-6|3638A8", 2, 0.9, "攻城器-战球（legacy 识别机器）"
    )
    override val SiegeBarracks = ColorSchema.parse(
        1, 587, 1279, 710, "282690", "-5|0|D8A871,6|-7|282897,17|2|282492,34|9|2329C8,34|3|282ED0,43|14|585C5B,49|0|2229C8,55|13|2024A8,41|-23|DCB078", 2, 0.9, "攻城器-战营（legacy 识别机器）"
    )
    override val SiegeBarracks2 = ColorSchema.parse(
        1, 587, 1279, 710, "272491", "0|-10|282590,18|2|2329CA,17|-5|282CCA,22|-4|282CCF,22|13|535858,40|2|2024B0,33|-10|282ED3,39|-5|262AC5,24|-32|E0B47E", 2, 0.9, "攻城器-战营（legacy 识别机器）"
    )
    override val SiegeLogLauncher = ColorSchema.parse(
        1, 587, 1279, 710, "142480", "-12|7|1F3762,-11|-3|403D39,-15|-13|D9AD78,1|-15|285186,23|-15|305890,22|2|101B68,21|11|102077,33|-3|423F3F,24|-24|696361", 2, 0.9, "攻城器-滚木车（legacy 识别机器）"
    )
    override val SiegeLogLauncher2 = ColorSchema.parse(
        1, 587, 1279, 710, "1E345E", "0|-8|3B3C38,-8|-6|D8A870,9|-35|605956,38|-33|706462,35|-23|2F5B97,7|-19|20406B,12|-6|152480,38|-9|101968,48|-11|413C39", 2, 0.9, "攻城器-滚木车（legacy 识别机器）"
    )
    override val SiegeFlameThrower = ColorSchema.parse(
        1, 587, 1279, 710, "204A74", "0|7|001A4A,12|16|2A33B5,19|45|2D4E8C,24|45|2E5192,33|34|292F9B,45|43|3060B2,46|38|336AC6,45|30|3967CA,34|10|464042,12|-18|6F6051", 2, 0.9, "攻城器-烈焰战车（legacy 识别机器）"
    )
    override val SiegeFlameThrower2 = ColorSchema.parse(
        1, 587, 1279, 710, "001949", "13|9|2B34B5,22|41|2B4B85,27|41|2C4B85,36|32|282F97,50|37|3163B7,50|33|326AC5,49|24|3967CA,34|7|2931AD,37|3|443F41,13|-28|796858", 2, 0.9, "攻城器-烈焰战车（legacy 识别机器）"
    )
    override val SiegeDrill = ColorSchema.parse(
        1, 587, 1279, 710, "383838", "1|-14|6073FF,-8|-24|697CFF,-13|1|6E6C6F,-45|2|383735,-46|-7|7E7F80,-53|-10|D8A470,-48|-11|353CBC,-36|-27|3B40B8,-7|-23|6979FF,2|-14|6375FF,7|-17|D8A870", 2, 0.9, "攻城器-钻机（legacy 识别机器）"
    )
    override val SiegeDrill2 = ColorSchema.parse(
        1, 587, 1279, 710, "555555", "11|0|4D514D,19|-2|D59765,5|-15|454545,15|-20|E1AD75,7|-28|7183FF,-3|-38|7682FF,4|-42|E6B981,-37|-15|3D3D3D,-44|-21|8D8D8D,-44|-26|4349CB,-48|-28|E5B47D", 2, 0.9, "攻城器-钻机（legacy 识别机器）"
    )
    override val SiegeTroopLauncher = ColorSchema.parse(
        1, 587, 1279, 710, "4E55D0", "-8|-6|454CB5,-17|-5|454CB5,-2|-19|4D54D5,21|12|393125,24|14|383027,28|-7|6E74F6,24|-7|6C75F5,24|-12|6C75F4,27|-10|6E75F5,20|-25|A29182,-19|-17|827266", 2, 0.9, "攻城器-部队发射器（legacy 识别机器）"
    )
    override val SiegeTroopLauncher2 = ColorSchema.parse(
        1, 587, 1279, 710, "4E58D5", "0|-4|4A52C8,-9|-7|454CB1,-14|-11|474FB7,-16|-13|484FB6,-1|-24|4D55D0,25|9|3D3729,23|-9|6C75F5,28|-10|6C75F5,30|-14|6A72F5,28|-17|6A74F6,33|-23|A99888,23|-30|A89588", 2, 0.9, "攻城器-部队发射器（legacy 识别机器）"
    )
    override val SiegeSkyChariot = ColorSchema.parse(
        1, 587, 1279, 710, "5A63D5", "-4|8|5C65D7,0|22|1A248E,26|23|1B248A,-3|42|000342,-2|48|00012E,7|63|24375B,18|64|435276,37|57|6B5E50", 2, 0.9, "攻城器-空中战车（legacy 识别机器）"
    )
    override val SiegeSkyChariot2 = ColorSchema.parse(
        1, 587, 1279, 710, "1D2797", "27|0|1A248A,-4|28|00012D,31|21|0B1579,38|38|6A5B4F,19|44|455377,12|45|334468,7|45|27395C,-5|-16|5D64D3,-4|-26|5C64D6", 2, 0.9, "攻城器-空中战车（legacy 识别机器）"
    )
    override val ClanCastleTroop = ColorSchema.parse(
        1, 587, 1279, 710, "C0844B", "-1|-13|C18450,10|0|C38650,7|-3|C48650,2|-7|C48450,1|-66|D09C60,0|-80|D09A58,7|-79|D4A060,4|-72|D1A061,66|-76|D6A165,70|-63|D4A068,76|-75|D09858,15|-96|C88C48,9|-92|B88440,7|-103|DE9850", 0, 0.96, "部落城堡援兵（legacy 找援兵）"
    )
    override val ClanCastleTroop2 = ColorSchema.parse(
        1, 587, 1279, 710, "C68450", "-2|-15|C58752,-1|-29|C88C58,14|-9|CF9862,17|-7|D09863,6|-14|C8905E,7|-63|D8A569,17|-67|E0B078,-3|-67|D0985D,-1|-74|D09958,48|-67|E0B780,72|-69|D09A5C,65|-56|D8A76E,65|-43|D8A770", 0, 0.96, "部落城堡援兵（legacy 找援兵）"
    )
    override val ClanCastleTroop3 = ColorSchema.parse(
        1, 587, 1279, 710, "D4A270", "-15|-18|D09968,-14|-42|D8A873,47|-42|D8A873,45|-18|CE9B6D,-21|12|C58450,-24|-67|D09B58,57|-21|C88A58,58|-68|D09858,12|-88|D09050", 0, 0.96, "部落城堡援兵（legacy 找援兵）"
    )
    override val ClanCastleTroop4 = ColorSchema.parse(
        1, 587, 1279, 710, "C0844E", "0|-61|D09860,78|-77|D09858,77|-1|C08449,66|-19|CC9460,69|-43|D2A470,17|-14|D09866,7|-40|D4A06C,7|-58|D8A36C,35|-69|E8C08C,50|-88|B88448,52|-93|C88C48,52|-103|E09A55", 0, 0.96, "部落城堡援兵（legacy 找援兵）"
    )
    override val ClanCastleTroop5 = ColorSchema.parse(
        1, 587, 1279, 710, "CCB530", "8|6|D5BE36,15|16|E2CC41,21|22|EAD547,72|1|CCB530,68|9|D7C038,64|20|E3CE43,61|24|E8D446,0|41|EBD74E,8|41|F2DD51,66|42|F2DD52,77|42|E9D44D,17|69|EDD73E,11|81|E1CA37,7|91|D6BF32,2|94|D1B92F,-1|99|CBB32C", 0, 0.96, "部落城堡援兵（legacy 找援兵）"
    )
}
