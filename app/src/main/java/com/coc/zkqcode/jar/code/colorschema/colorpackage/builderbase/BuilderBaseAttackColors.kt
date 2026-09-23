@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.builderbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IBuilderBaseAttackColors {
    val BuilderBaseGold: ColorSchema
    val BuilderBaseExiler: ColorSchema
    val AttackNow: ColorSchema
    val TrainTroopsWarning: ColorSchema
    val CancelAttackSearch: ColorSchema
    val ExitBattleButton: ColorSchema
    val NightWitch: ColorSchema
    val TroopSkills: ColorSchema
    val MachineSkills: ColorSchema
    /** 战争机器技能就绪：英雄卡槽顶部充能条第 1 格亮起。 */
    val HeroChargeReady: ColorSchema
    val BattleCopterSkills: ColorSchema
    val BattleCopterSkillsAlt: ColorSchema
    val BuilderBaseBarbarian: ColorSchema
    val TroopsWithSkills: ColorSchema
    val TroopsWithOutSkills: ColorSchema
    val BuilderBaseMachine: ColorSchema
    val BuilderBaseBattleCopter: ColorSchema
    val BuilderBasePekka: ColorSchema
    val BuilderBaseGiant: ColorSchema
    val BuilderBaseGiantAlt: ColorSchema
    val BuilderBaseArcher: ColorSchema
    val BuilderBaseCannonCart: ColorSchema
    val BuilderBaseBomber: ColorSchema
    val BuilderBaseHog: ColorSchema
    val BuilderBaseBalloon: ColorSchema
    val BuilderBaseBabyDragon: ColorSchema
    val BuilderBaseBabyDragonAlt: ColorSchema
    val BuilderBaseMinion: ColorSchema
    val BuilderBaseWizard: ColorSchema
}

object BuilderBaseAttackColors : IBuilderBaseAttackColors {
    override val BuilderBaseGold = ColorSchema.parse(
        1000, 20, 1270, 80, "72DCF4", "0|1|72DCF4,0|2|72DCF4,0|3|72DCF4,0|4|72DCF4,0|5|72DCF4", 0, 0.95
    )
    override val BuilderBaseExiler = ColorSchema.parse(
        1000, 50, 1270, 140, "DF93B2", "-1|1|DF93B2,-1|2|DF93B2,-1|3|DF93B2,-1|4|DF93B2,-1|5|DF93B2", 0, 0.95
    )
    override val AttackNow = ColorSchema.parse(
        825, 431, 1075, 518, "86E9BA", "29|1|85E8B9,64|0|86E9BA,120|-2|89E9BC,158|-5|8CEABE,14|51|3AD48B,54|46|3AD48B,91|51|3AD48B,158|53|3AD38B,188|46|3AD48B", 0, 0.9, "夜世界立即进攻"
    )
    override val TrainTroopsWarning = ColorSchema.parse(
        491, 198, 596, 216, "2021FE", "1|0|2122FE,5|0|2122FE,9|0|2021FE,9|3|3335F6,6|3|3335F6,3|3|292AFC,1|3|3435F6,0|3|3335F6,-1|3|3335F6", 0, 0.95, "练兵警告"
    )
    override val CancelAttackSearch = ColorSchema.parse(
        569, 611, 714, 649, "7D77FE", "29|0|7D77FE,71|-3|8079FF,87|0|7D77FE,116|0|7D77FE,0|19|110FDB,29|19|110FDB,58|19|110FDB,87|19|110FDB,116|19|110FDB", 0, 0.9, "取消搜索"
    )
    override val ExitBattleButton = ColorSchema.parse(
        33, 491, 153, 518, "5F5DF4", "24|0|5F5DF4,18|2|5F5DF2,72|0|5F5DF4,96|0|5F5DF4,0|13|0E0DCE,24|13|0E0DCE,46|18|0E0DCF,72|13|0E0DCE,96|13|0E0DCE", 0, 0.9, "红色退出对战"
    )
    override val NightWitch = ColorSchema.parse(
        190, 587, 800, 712, "191814", "9|0|141413,19|0|292E32,29|0|2E3438,38|0|A5A4CB,0|17|836F5D,9|17|745F48,19|17|2E3438,29|17|AEAFDE,38|17|9497DA", 0, 0.9
    )
    override val TroopSkills = ColorSchema.parse(
        189, 570, 1241, 600, "FF44C9", "7|0|FF44C9,14|0|FF44C9,20|0|FF44C9,27|0|FF44C9,0|7|FF69D1,7|7|FF69D1,14|7|FF69D1,20|7|FF69D1,27|7|FF69D1", 0, 0.9, "开部队技能"
    )
    override val MachineSkills = ColorSchema.parse(
        139, 553, 160, 563, "FF35CF", "4|0|FF35CF,9|0|FF35CF,13|0|FF35CF,17|0|FF35CF,0|5|FF49D4,4|5|FF49D4,9|5|FF49D4,13|5|FF49D4,17|5|FF49D4", 0, 0.9, "机器技能"
    )
    // 战争机器技能就绪 = 英雄卡槽顶部"充能条"第 1 格亮起（用户在实机确认的机制）。
    // 第 1 格位置固定在 (95,560)-(112,565)，亮色约 RGB C022FB（BGR FB22C0）。
    // 由 tools/make_feature.py 从实机截图 `I:\coc\游戏截图\夜世界-英雄充能\夜世界-英雄充能进度1~3.jpg`
    // 自动生成并自检（进度 1/2/3 命中、进度 0 不命中）。
    override val HeroChargeReady = ColorSchema.parse(
        95, 560, 112, 565, "FB22C0",
        "0|0|FB22C1,16|4|FE3AC7,8|2|FC2AC2,2|4|FE3AC7",
        0, 0.9, "英雄充能就绪"
    )
    // 夜飞机(空中机器)技能就绪粉光（源 战斗监控 16639/16664：FFB2FF 或 FE3AC7，位于英雄槽旁）。
    // 偏移按 (dx,dy)->(dy,-dx) 旋转；实际搜索区在 realAttack 中按英雄槽位置 rescope。
    override val BattleCopterSkills = ColorSchema.parse(
        550, 555, 580, 575, "FFB2FF",
        "0|-2|FFA2FF,9|0|FFB2FF,9|-3|FF9DFF,15|-1|FFA8FF,15|-3|FF9DFF",
        0, 0.9, "夜飞机技能"
    )
    override val BattleCopterSkillsAlt = ColorSchema.parse(
        550, 555, 580, 575, "FE3AC7",
        "0|-2|FC2AC2,11|0|FE3AC7,11|-3|FB25C1,17|0|FE3AC7,17|-3|FB25C1",
        0, 0.9, "夜飞机技能(备选)"
    )
    override val BuilderBaseBarbarian = ColorSchema.parse(
        193, 585, 1261, 623, "FF763A", "7|0|FF763A,15|0|FF773B,23|0|FF783C,30|0|FF793C,0|6|FF773A,7|6|FF793C,15|6|FF7B3E,23|6|FF7D40,30|6|FF7E41", 0, 0.9, "夜世界野蛮人"
    )

    // 夜世界王(战争机器)：精确识别部署栏里的机器卡，替代原先写死的 tap(125,610)。
    // 源脚本 `findMultiColor(48,15,142,500,"3067A2",...)`，经 90° 映射后区域为 (15,577,500,671)。
    override val BuilderBaseMachine = ColorSchema.parse(
        15, 577, 500, 671, "3067A2",
        "13|6|00040A,18|-1|98B9FF,27|13|305F99,32|4|010005,43|5|3064A8,18|5|8E5566",
        0, 0.85, "夜世界王(战争机器)"
    )

    // 夜飞机(空中机器)：源脚本 `findMultiColor(48,15,142,500,"505B80",...)`，经 90° 映射后区域为 (15,578,500,672)。
    // 偏移按 (dx,dy)->(dy,-dx) 旋转（与 BuilderBaseMachine 同口径）。
    override val BuilderBaseBattleCopter = ColorSchema.parse(
        15, 578, 500, 672, "505B80",
        "5|0|55618E,13|-4|68ACFF,17|-9|65A7F8,29|-11|507DC5,46|-13|4A7BBF,57|-2|FFF8F8,59|1|FFF4F8,37|-6|FFFCFA",
        0, 0.85, "夜世界夜飞机(空中机器)"
    )

    // 夜世界兵种卡槽（源 `函数123a` 行 15997~16071）。源区域 (43,15,108,1279) 经 90° 映射为
    // (15,612,1279,677)；各偏移按 (dx,dy)->(dy,-dx) 旋转（与 BuilderBaseMachine 同口径）。
    override val BuilderBasePekka = ColorSchema.parse(
        15, 612, 1279, 677, "3D2D29",
        "-2|4|452F21,4|3|030404,10|4|2E221C,16|1|030304,-28|-12|4C390F,4|-11|463C3C,33|-12|3E3307,-11|-12|804C28,21|-13|402A1C",
        0, 0.95, "夜世界皮卡"
    )
    override val BuilderBaseGiant = ColorSchema.parse(
        15, 612, 1279, 677, "79ACF7",
        "3|-24|FE884B,2|-17|80B0F4,50|-24|3484F3,78|-23|FE8A4C,62|-5|6090E8,74|0|FC9858,51|1|284078,32|2|5888DA,17|-3|2072E8",
        0, 0.95, "夜世界巨人"
    )
    override val BuilderBaseGiantAlt = ColorSchema.parse(
        15, 612, 1279, 677, "78ACF1",
        "0|-23|FC8849,1|-18|9BC0F8,31|-14|5883E6,52|-21|2774EF,65|-17|1F70EF,57|-7|7894F5,67|-2|608EE5,78|4|FC9556,81|-12|FC9052",
        0, 0.95, "夜世界巨人(备选)"
    )
    override val BuilderBaseArcher = ColorSchema.parse(
        15, 612, 1279, 677, "495382",
        "12|4|83A3F3,11|10|7C99EB,22|11|2B174E,22|6|2B1853,6|-11|2A154C,-3|-10|3B2462,-11|11|7189D9,-14|4|4E5989,10|-3|6F7C89",
        0, 0.9, "夜世界弓箭"
    )
    override val BuilderBaseCannonCart = ColorSchema.parse(
        15, 612, 1279, 677, "9A8D91",
        "-9|-14|8FA7BB,12|-13|000000,35|-12|AAA4A4,46|-12|171418,57|-14|615D61,64|-14|FC9254,71|-13|FC894C,67|4|FC9354,71|3|FE8C4E",
        0, 0.9, "夜世界炮车"
    )
    override val BuilderBaseBomber = ColorSchema.parse(
        15, 612, 1279, 677, "FE8C50",
        "1|-15|FD8C4F,15|-10|184A73,13|-17|1A466C,35|-14|184780,43|-14|0A366A,45|-20|4896DC,61|-14|60C2F8,64|0|FCA263,62|-10|FCA464",
        0, 0.9, "夜世界炸弹"
    )
    override val BuilderBaseHog = ColorSchema.parse(
        15, 612, 1279, 677, "9E8274",
        "-1|-14|B59683,7|2|333857,10|-4|313E6A,11|-9|ACADAD,11|-15|293659,17|-17|314272,22|-13|2C3B68,28|-14|8C9CD0,27|-1|455791,31|3|354A86,40|8|4E65AF,37|5|8F9FD3",
        0, 0.9, "夜世界野猪"
    )
    override val BuilderBaseBalloon = ColorSchema.parse(
        15, 612, 1279, 677, "8AA8BA",
        "-5|-11|FD9050,0|-10|B4C4C0,6|-9|FC9C5F,25|-8|88A6BB,33|-6|A0BED8,41|-5|FCB070,27|-16|7A7890,54|-9|A3B9C9,65|-3|6D9ACD",
        0, 0.9, "夜世界气球"
    )
    override val BuilderBaseBabyDragon = ColorSchema.parse(
        15, 612, 1279, 677, "FD8C50",
        "9|-18|FC9454,13|-10|5FC367,15|-5|56A655,34|-20|4FD487,48|-20|A0E0F2,74|-20|FD8E50,73|-11|2E8C5F,77|2|58DD90,60|-3|40B758",
        0, 0.9, "夜世界龙宝"
    )
    override val BuilderBaseBabyDragonAlt = ColorSchema.parse(
        15, 612, 1279, 677, "FC9054",
        "5|-14|FC9456,12|-10|58BA60,23|-8|46BC68,46|8|4054B7,51|1|4054B8,46|-13|6AD988,62|-8|40B45E,62|0|40B55C,78|-19|FE8D50",
        0, 0.9, "夜世界龙宝(备选)"
    )
    override val BuilderBaseMinion = ColorSchema.parse(
        15, 612, 1279, 677, "4B7A58",
        "-4|-4|FD8F50,4|-7|FC995B,0|-15|186344,14|-9|50714B,43|-12|3A795B,57|-11|BFF8E0,68|-11|186F44,65|-1|FC9B5A,63|1|FC9C5C",
        0, 0.9, "夜世界亡灵"
    )
    override val BuilderBaseWizard = ColorSchema.parse(
        15, 612, 1279, 677, "243878",
        "-6|-8|FD9050,-9|-15|FD8849,14|-11|5096FF,21|-4|5090FF,45|-12|80AFE8,42|-16|8CC2FF,51|-15|242E48,61|-4|FC995A,61|5|FC9A5A",
        0, 0.9, "夜世界法师"
    )

    // Troops with/without skills indicators
    override val TroopsWithSkills = ColorSchema.parse(
        80, 570, 1210, 630, "FE41C8", "7|0|FE41C8,14|0|FE41C8,21|0|FE41C8,28|0|FE41C8,0|9|FF74D4,7|9|FF74D4,14|9|FF74D4,21|9|FF74D4,28|9|FF74D4", 0, 0.9, "带技能部队"
    )
    override val TroopsWithOutSkills = ColorSchema.parse(
        80, 570, 1210, 630, "FF763A", "7|0|FF763A,17|1|FF763A,22|2|FF773B,26|2|FF773B,24|8|FF7C3F,16|12|FF7E41,8|13|FF7C3F,3|12|FF793C,1|9|FF773A", 0, 0.9, "无技能部队"
    )
}
