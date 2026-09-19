@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Clan capital army / spell features, used to recognise troops on the capital army screen.
 *
 * The clan capital roster is a standalone set that must NOT be mixed up with the main village
 * or the builder base: 17 troops + 7 spells = 24 entries. The old freescript only knew 13 of
 * them, so the whole set was re-derived from real 1280x720 screenshots of the capital army
 * screen: every icon was located, and for each one an anchor pixel plus offset points were
 * chosen so that a feature matches ONLY its own icon (no feature ever collides with another
 * icon, verified over all available screenshots).
 *
 * All features share the troop-list search area (131, 396, 1140, 679).
 * Regenerate with tools/... (see docs/复用优化方案20260919.md section 11.9b).
 */
interface ICapitalTroopColors {
    val CapitalSuperBarbarian: ColorSchema
    val CapitalSuperGiant: ColorSchema
    val CapitalSkeletonArmy: ColorSchema
    val CapitalRocketBalloon: ColorSchema
    val CapitalFlyingFortress: ColorSchema
    val CapitalThunderPekka: ColorSchema
    val CapitalSuperDragon: ColorSchema
    val CapitalSneakyArcher: ColorSchema
    val CapitalBattleRam: ColorSchema
    val CapitalSuperWizard: ColorSchema
    val CapitalSkeletonBarrel: ColorSchema
    val CapitalRaidCart: ColorSchema
    val CapitalHogRaider: ColorSchema
    val CapitalMountainGolem: ColorSchema
    val CapitalInfernoDragon: ColorSchema
    val CapitalSuperSparky: ColorSchema
    val CapitalHealingSpell: ColorSchema
    val CapitalLightningSpell: ColorSchema
    val CapitalRageSpell: ColorSchema
    val CapitalHasteSpell: ColorSchema
    val CapitalSuperMiner: ColorSchema
    val CapitalJumpSpell: ColorSchema
    val CapitalFreezeSpell: ColorSchema
    val CapitalSkeletonSpell: ColorSchema
}

object CapitalTroopColors : ICapitalTroopColors {
    override val CapitalSuperBarbarian = ColorSchema.parse(
        131, 396, 1140, 679, "8E8E95",
        "2|-2|0D0D0D,-2|-14|5B6FA8,2|-22|6F9CE6,14|-10|6892D7,14|-14|648FD6,-6|-22|5E75B6,14|-22|BEF5FF,14|-18|5B89DE,10|-10|638ACC",
        0, 0.9, "超级野蛮人"
    )
    override val CapitalSuperGiant = ColorSchema.parse(
        131, 396, 1140, 679, "2DB7FF",
        "-14|6|1F7EFF,-6|10|1F66D9,2|18|1F6FD9,2|10|1E71EE,-14|10|1E5FD2,-2|10|1E6CE4,-22|6|1E69D6,-22|10|1E63CA,-14|22|B4E4FF,-22|2|1E79F2",
        0, 0.9, "超级巨人"
    )
    override val CapitalSkeletonArmy = ColorSchema.parse(
        131, 396, 1140, 679, "110E11",
        "-6|-14|5D3625,18|-2|6A2F10,-2|-14|734410,18|22|C68534,14|18|AE8231,-10|22|864310,22|22|D09B3B,22|-2|331F33,22|-6|A4620F,18|6|A06723",
        0, 0.9, "亡灵大军"
    )
    override val CapitalRocketBalloon = ColorSchema.parse(
        131, 396, 1140, 679, "2E313A",
        "-18|2|685B56,-18|14|1A191A,10|22|5582BD,-22|14|141414,-22|2|594D49,-2|10|17191C,-22|10|151516,14|22|436FA5",
        0, 0.9, "火箭气球兵"
    )
    override val CapitalFlyingFortress = ColorSchema.parse(
        131, 396, 1140, 679, "FCFBFB",
        "10|-10|A6A6A6,6|-22|B4AAA5,10|-22|BEB5AE,2|-18|A5B2B7,6|-14|9DA8AE,-18|-18|5FB8E6,6|-18|A89C9D,22|-22|777DFF,22|-18|ACC0DC,-22|-14|3E7FA6",
        0, 0.9, "飞行堡垒"
    )
    override val CapitalThunderPekka = ColorSchema.parse(
        131, 396, 1140, 679, "8B7026",
        "10|-6|121111,-22|-2|AE8748,14|-10|D6BE2F,2|-2|59481B,-14|-2|D4A968,-10|-18|C09A53,10|-10|DFC63C,22|18|DFBE56,-22|-6|C38C33,-2|-18|B47645",
        0, 0.9, "雷霆皮卡"
    )
    override val CapitalSuperDragon = ColorSchema.parse(
        131, 396, 1140, 679, "8EEFFF",
        "-22|22|3D89D8,-22|2|172E52,18|2|588ABF,-22|10|152B51,-18|2|273E61,-6|2|70ADE4,-22|6|192D4E,2|14|456085,22|10|7099C5,-6|10|4E7CA7",
        0, 0.9, "超级飞龙"
    )
    override val CapitalSneakyArcher = ColorSchema.parse(
        131, 396, 1140, 679, "100D0F",
        "-22|14|E8A1FF,-18|-14|9E45CA,-18|-10|855BE5,-10|-2|B25766,2|-22|693698,-14|-10|D8636E,-14|-6|B754E7,-10|-10|73343C,2|18|BECEF1,-18|6|AF5569",
        0, 0.9, "隐秘弓箭手"
    )
    override val CapitalBattleRam = ColorSchema.parse(
        131, 396, 1140, 679, "767C81",
        "-10|-6|8FA2AC,-18|-2|909FAC,-22|-22|819BE1,-10|-10|8EC8FF,-14|2|879AAC,-6|-14|7DB8F0,-14|-2|8E9DAC,-22|2|7D95AC,-6|-18|6DE7FF,-6|-10|9FDAFF",
        0, 0.9, "野蛮人攻城槌"
    )
    override val CapitalSuperWizard = ColorSchema.parse(
        131, 396, 1140, 679, "0F1123",
        "-10|22|3955A8,-10|14|5A6EA8,-22|22|28327B,-14|6|2075C6,-2|-14|3D4660,-18|14|5B74AE,6|-6|112950",
        0, 0.9, "超级法师"
    )
    override val CapitalSkeletonBarrel = ColorSchema.parse(
        131, 396, 1140, 679, "4048E5",
        "-2|2|35384D,-2|6|192559,22|2|394191,-2|10|18235B,22|10|414998,14|14|2F3493,18|14|282C71,10|14|333AAA,2|22|6271FF,14|18|2E3373",
        0, 0.9, "骷髅飞桶"
    )
    override val CapitalRaidCart = ColorSchema.parse(
        131, 396, 1140, 679, "101315",
        "-6|2|294260,-2|14|1F2C39,-10|2|544B47,-10|22|303237,14|-6|666666,-6|10|304A67,2|-6|5D8EB8,-6|14|355677,-2|-2|598DB5,2|2|5B5350",
        0, 0.9, "突袭炮车"
    )
    override val CapitalHogRaider = ColorSchema.parse(
        131, 396, 1140, 679, "FFFFFF",
        "2|14|7192E3,-14|-18|C7D3E9,-14|-6|829ABD,-22|-2|88A3CB,-10|14|6285CE,6|22|4C70C1,-10|2|B77832,-14|-14|F6E6D1,-18|2|1C2439,6|18|597BCD",
        0, 0.9, "野猪突袭队"
    )
    override val CapitalMountainGolem = ColorSchema.parse(
        131, 396, 1140, 679, "324636",
        "-2|2|0D0D0D,14|-2|C7D1CA,10|2|B7C3BC,22|-10|42B489,-22|-10|388962,-18|-2|DEE6E1,6|-6|45B283,22|-14|38806D,-2|-18|5D605E,-6|-18|585C5A",
        0, 0.9, "高山戈仑"
    )
    override val CapitalInfernoDragon = ColorSchema.parse(
        131, 396, 1140, 679, "242947",
        "14|-2|97B7FF,-2|-6|62577C,2|-6|64597D,-2|2|615E7D,2|6|6A6C9A,18|2|5C8EFF,2|-2|4C496F,22|14|3F6AC1,2|2|6A6C95,10|14|34416B",
        0, 0.9, "地狱飞龙"
    )
    override val CapitalSuperSparky = ColorSchema.parse(
        131, 396, 1140, 679, "495961",
        "2|6|3A7091,2|-22|FC922D,22|-18|8E610E,6|-18|FFA130,14|-14|FFBB2A,6|-14|C4492A,10|18|89610D,14|-18|FFC132,-2|-22|C9643A,18|-14|996B12",
        0, 0.9, "超级电磁炮"
    )
    override val CapitalHealingSpell = ColorSchema.parse(
        131, 396, 1140, 679, "1298EE",
        "-2|-22|58B8EE,18|-22|37C2FF,-6|-22|71BFE9,2|-22|41B2F2,-2|-18|54B7ED,-6|2|0D7ABD,14|-14|3FB5F9,10|10|265B7B",
        0, 0.9, "疗伤法术"
    )
    override val CapitalLightningSpell = ColorSchema.parse(
        131, 396, 1140, 679, "FFFFFF",
        "-2|22|FFD6A6,-2|18|FFD6A6,-2|2|FBB7BD,22|22|FFD8A7,18|6|F89B8D,-2|6|FDC48D,22|6|F99C8E,14|6|F89A8D,18|22|FFD6A6,22|10|FFB79A",
        0, 0.9, "雷电法术"
    )
    override val CapitalRageSpell = ColorSchema.parse(
        131, 396, 1140, 679, "E93DB9",
        "14|2|BC387F,-14|-14|FF5FEB,10|-22|F674D3,-10|-14|FF7AFF,18|2|B24389,-22|-14|C23A95,6|-18|F787EE,18|-2|BB438F,2|-14|F477E5,-6|6|BE6AC0",
        0, 0.9, "狂暴法术"
    )
    override val CapitalHasteSpell = ColorSchema.parse(
        131, 396, 1140, 679, "0D0D0D",
        "18|-22|E18EFC,-6|-18|7C379C,22|-18|B554D1,2|-18|8A3FAC,-6|-22|8D45AD,22|-22|E497FD,-2|-18|813BA3,2|-22|9E59B2,-2|-22|9A4FBB,-22|-14|8B3A94",
        0, 0.9, "永恒急速法术"
    )
    override val CapitalSuperMiner = ColorSchema.parse(
        131, 396, 1140, 679, "7AB7BB",
        "-14|22|6CA6F0,-10|18|77B3FF,-22|2|5481C6,-22|22|3A8DE4,-18|14|4F7FC6,-6|10|467CCF,-14|18|4E80C4,-14|6|66A3F7,-6|18|5573A3,-10|14|84BAFF",
        0, 0.9, "超级矿工"
    )
    override val CapitalJumpSpell = ColorSchema.parse(
        131, 396, 1140, 679, "E5FFFF",
        "18|-14|A5F6D5,10|-2|52BE7D,14|-6|4FBD7C,-2|-6|9CFFCF,10|-6|50BE7D,18|2|51BD7D,22|2|50BC7C,14|-18|5CC488,14|6|ADB3AC,6|-22|88C7A2",
        0, 0.9, "弹跳法术"
    )
    override val CapitalFreezeSpell = ColorSchema.parse(
        131, 396, 1140, 679, "F7E6E2",
        "18|-22|DCC3BA,18|10|7F7F7F,10|-14|D0A598,2|-14|D1A69A,-18|-2|77949C,18|-18|D5AB9E,14|-22|D7B5AA,22|-22|E0CEC9,22|-14|D6ADA1,10|-10|DAB5AB",
        0, 0.9, "冰霜法术"
    )
    override val CapitalSkeletonSpell = ColorSchema.parse(
        131, 396, 1140, 679, "8B6A69",
        "-14|-2|B18683,-14|14|A99090,-18|18|A88786,-14|2|C0C7CA,-22|-2|CFA8A6,2|14|A78B8B,-2|22|B87A74,-10|14|A49190,-2|10|DECFCF,-18|-22|BF5B6D",
        0, 0.9, "骷髅召唤法术"
    )
}
