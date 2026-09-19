@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Black-elixir troop recognition features for the main-base training panel.
 * Auto-named from the trophy icons (masked NCC) and derived from the real
 * 1280x720 screenshot "补充3\主世界-配兵-黑油兵2.png" (黑油兵 selection/training panel).
 * 戈仑石人/戈仑冰人/烈焰熔炉 use the user's blue-box-annotation ground-truth
 * positions (548,496)/(803,491)/(1063,490); the rest are masked-NCC located.
 * Shared search region: (234, 304, 1189, 680).
 * Regenerate with tools/gen_mainbase_black_elixir.py.
 */
interface IMainBaseBlackElixirTroopColors {
    val MainBaseMinion: ColorSchema
    val MainBaseDruid: ColorSchema
    val MainBaseBowler: ColorSchema
    val MainBaseRuinWitch: ColorSchema
    val MainBaseApprenticeWarden: ColorSchema
    val MainBaseHogRider: ColorSchema
    val MainBaseWitch: ColorSchema
    val MainBaseValkyrie: ColorSchema
    val MainBaseHunter: ColorSchema
    val MainBaseLavaHound: ColorSchema
    val MainBaseGolem: ColorSchema
    val MainBaseIceGolem: ColorSchema
    val MainBaseInfernoDragon: ColorSchema
}

object MainBaseBlackElixirTroopColors : IMainBaseBlackElixirTroopColors {
    override val MainBaseMinion = ColorSchema.parse(
        234, 304, 1189, 680, "FFFFFF",
        "12|-12|446989,15|6|20476A,15|-15|5181AB,15|-6|17507B,15|-3|1D4D76,15|0|1A496F,12|-15|194469,15|-9|184E7A,15|3|1A4366,12|-6|90A6BB",
        0, 0.9, "亡灵"
    )
    override val MainBaseDruid = ColorSchema.parse(
        234, 304, 1189, 680, "ABABAB",
        "9|-12|C29B6E,15|-12|C29B6F,6|6|D8D8D9,15|-15|C09A69,12|-15|BD996C,6|-9|A5856C,0|-12|7E685E,12|-9|B4926E,3|-9|846E66,15|-9|BA936F",
        0, 0.9, "德鲁伊"
    )
    override val MainBaseBowler = ColorSchema.parse(
        234, 304, 1189, 680, "587B80",
        "6|-15|1F1410,6|-12|241712,15|-12|241712,6|-9|251813,15|-9|251813,12|-6|261914,-9|-3|1F4A6F,9|-9|251813,0|3|326277,-6|-3|214C6F",
        0, 0.9, "巨石投手"
    )
    override val MainBaseRuinWitch = ColorSchema.parse(
        234, 304, 1189, 680, "FEFEFE",
        "-3|0|E8E5DC,0|3|D4D0C5,6|0|E0E0D4,-3|3|D3D0C5,9|-6|E9B3B1,15|0|E7B4B4,15|-6|FFBCBB,12|-12|FFB8B6,12|-6|FFBEBC,15|-9|FFBBB8",
        0, 0.9, "废墟女巫"
    )
    override val MainBaseApprenticeWarden = ColorSchema.parse(
        234, 304, 1189, 680, "FDFDFD",
        "15|-15|E45FFF,0|-3|B1B1B1,15|-12|EB66FF,15|-9|F26BFE,6|-15|FA6AFF,3|-12|FF70FF,3|-15|FF6DFF,12|-15|ED65FF,6|-12|FF6EFF,12|-9|F873FE",
        0, 0.9, "守护者学徒"
    )
    override val MainBaseHogRider = ColorSchema.parse(
        234, 304, 1189, 680, "FCFCFC",
        "12|-12|758198,15|-15|8392AC,12|-15|75839A,0|-15|4C3D43,12|-9|717C93,3|-12|47454F,3|-9|51535F,0|-12|6D6343,3|-6|585554,0|-9|5B4D34",
        0, 0.9, "野猪骑士"
    )
    override val MainBaseWitch = ColorSchema.parse(
        234, 304, 1189, 680, "FFFFFF",
        "-12|-15|66415E,-3|-12|B5858D,-3|12|636367,-12|-9|7C5579,-15|-9|695077,-6|0|4C4B4E,-6|-12|915F94,0|-15|DAB0C3,-9|-9|9F71A5,3|-9|C19EAE",
        0, 0.9, "女巫"
    )
    override val MainBaseValkyrie = ColorSchema.parse(
        234, 304, 1189, 680, "FDFDFD",
        "-3|-15|FD9666,3|15|D1532F,0|-15|FD9667,9|15|CA481E,6|-15|993316,-6|-15|A12818,6|15|D1512C,12|15|762010,12|-3|0D0D0D,12|-6|100F0F",
        0, 0.9, "瓦基丽武神"
    )
    override val MainBaseHunter = ColorSchema.parse(
        234, 304, 1189, 680, "FFFD3F",
        "-9|-9|6C64A8,12|0|8375A4,12|-3|887EB6,-6|-12|6B62A6,12|3|8473A5,0|-12|716DAC,15|15|8286BD,12|-6|8680BC,9|3|7B4769,-12|-6|6D5F9F",
        0, 0.9, "英雄猎手"
    )
    override val MainBaseLavaHound = ColorSchema.parse(
        234, 304, 1189, 680, "E5E3EC",
        "12|-15|FFDF4C,6|-3|A7ACBC,9|-15|FFA026,3|-15|BFBFCC,15|-15|D6C7B0,15|6|9497A7,3|-3|C0C7D9,3|-9|BBB1BF,3|-6|BFC5D6,-6|0|A98375",
        0, 0.9, "熔岩猎犬"
    )
    override val MainBaseGolem = ColorSchema.parse(
        234, 304, 1189, 680, "D5BDA4",
        "-3|-6|ADA29B,-6|6|908F8D,0|-3|A09083,-3|0|7F7E7A,-3|3|6E6C69,-3|-9|737175,9|-3|631A59,-3|6|55524C,3|0|BF8C98,0|3|535151",
        0, 0.9, "戈仑石人"
    )
    override val MainBaseIceGolem = ColorSchema.parse(
        234, 304, 1189, 680, "FDFDFD",
        "15|15|E3E1DD,9|15|E0DFDC,12|15|E4E2DE,9|-15|DFDEDB,12|12|E7E5E1,6|15|DEDEDC,3|12|E3E4E1,6|12|E0E1DF,-6|-9|787878,12|-12|D9D8D5",
        0, 0.9, "戈仑冰人"
    )
    override val MainBaseInfernoDragon = ColorSchema.parse(
        234, 304, 1189, 680, "B1DDF9",
        "15|-15|EAC92E,12|0|BF8F7F,9|-15|74BCE3,3|0|ECCCA3,0|-3|71C3F4,15|-6|E08723,6|6|666666,15|0|A57F6B,12|-3|AC5A35,6|-6|AA6A3A",
        0, 0.9, "烈焰熔炉"
    )
}
