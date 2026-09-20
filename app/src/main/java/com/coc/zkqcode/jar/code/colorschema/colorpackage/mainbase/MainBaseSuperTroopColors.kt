@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Main-base SUPER troop (超级兵) recognition features.
 * Derived from the 1280x720 screenshots in "补充4" (配兵 screen): the two
 * rightmost-column slots hold the super troops. Filename "A-B" = top,bottom.
 * Shared search region: (999, 427, 1258, 692).
 * Regenerate with tools/gen_mainbase_super_troop_colors.py.
 */
interface IMainBaseSuperTroopColors {
    val MainBaseSuperFrostHound: ColorSchema
    val MainBaseSuperBowler: ColorSchema
    val MainBaseSuperArcher: ColorSchema
    val MainBaseSuperGiant: ColorSchema
    val MainBaseSuperWizard: ColorSchema
    val MainBaseSuperMinion: ColorSchema
    val MainBaseSuperWallBreaker: ColorSchema
    val MainBaseRocketBalloon: ColorSchema
    val MainBaseSuperValkyrie: ColorSchema
    val MainBaseSuperWitch: ColorSchema
    val MainBaseSuperMiner: ColorSchema
    val MainBaseSuperHogRider: ColorSchema
    val MainBaseSneakyGoblin: ColorSchema
    val MainBaseSuperYeti: ColorSchema
}

object MainBaseSuperTroopColors : IMainBaseSuperTroopColors {
    override val MainBaseSuperFrostHound = ColorSchema.parse(
        999, 427, 1258, 692, "E9D9C3",
        "2|2|D8E5F3,5|8|F6F4EF,2|5|F4F2EF,-1|5|EEEFF3,-4|8|A8A5A4,11|8|F1F0F1,-4|11|AEAAAD,8|8|F4F2F0,14|11|F2F0E9,-1|2|F1F6FF",
        0, 0.9, "寒冰猎犬"
    )
    override val MainBaseSuperBowler = ColorSchema.parse(
        999, 427, 1258, 692, "FFEC4A",
        "20|8|F0AE49,14|11|F9BA59,20|11|EAAA4C,8|14|EEB056,11|14|EFB056,14|14|EFB156,14|-4|E4BB62,5|14|EDAF55,11|-4|F2C668,17|14|EFB25D",
        0, 0.9, "超级巨石投手"
    )
    override val MainBaseSuperArcher = ColorSchema.parse(
        999, 427, 1258, 692, "4E5348",
        "-19|8|A9B3C0,-19|11|ACB3C0,-16|2|ABB2BF,-19|2|AAB3C0,-22|2|AAB3C0,-19|5|A7B3C0,-22|8|ABACC0,-22|-1|A4AAB9,-13|14|ACB3C0,-22|5|A8B2BE",
        0, 0.9, "超级弓箭手"
    )
    override val MainBaseSuperGiant = ColorSchema.parse(
        999, 427, 1258, 692, "FFD99F",
        "5|11|F57A1F,-1|11|FD811F,5|8|FF9721,-7|5|F47E22,8|8|FF9429,11|8|FF952A,-16|11|FF8E22,-16|5|FFA035,-16|-19|C9611E,-19|5|FFAF49",
        0, 0.9, "超级巨人"
    )
    override val MainBaseSuperWizard = ColorSchema.parse(
        999, 427, 1258, 692, "F5CFBD",
        "-1|8|FFF7F8,-4|5|DBA9B1,-1|2|FFF1FE,-7|20|FEEEF0,5|2|E29193,14|20|846361,17|20|8C6A68,11|20|7C6062,8|17|23130F,20|11|5C4945",
        0, 0.9, "超级法师"
    )
    override val MainBaseSuperMinion = ColorSchema.parse(
        999, 427, 1258, 692, "65D1EE",
        "-1|-1|45B7DA,8|-16|288DB2,-4|-7|3EBDE1,-7|-1|3DA7D2,20|2|4AB9D2,11|-19|338EB2,-1|-22|3DB0D5,5|-16|48BADB,-4|-1|3BAFD6,-4|-10|30ABD2",
        0, 0.9, "超级亡灵"
    )
    override val MainBaseSuperWallBreaker = ColorSchema.parse(
        999, 427, 1258, 692, "151413",
        "-10|14|F9F1DC,-7|17|FFFFE9,-7|14|FFFCE9,-16|-22|C19471,-19|-22|B58866,-4|14|FEFBDF,2|8|372D26,-13|14|E6D9C4,-22|-22|A87E5E,-1|-13|E6BC92",
        0, 0.9, "超级炸弹人"
    )
    override val MainBaseRocketBalloon = ColorSchema.parse(
        999, 427, 1258, 692, "A79B9E",
        "5|-22|C5403E,11|-7|7B2F41,5|-16|B1393D,2|-22|DD7131,14|-19|847167,-4|-19|C33D3D,8|2|58515D,8|-1|28272C,-16|-19|BD393B,-16|-22|C03B3C",
        0, 0.9, "火箭气球兵"
    )
    override val MainBaseSuperValkyrie = ColorSchema.parse(
        999, 427, 1258, 692, "FEE6FF",
        "-4|2|E3C8F0,8|2|FFAF97,-1|5|CEB1DC,5|2|FFA9A4,-4|5|D2B2DE,11|2|FB9F87,11|5|FDB19A,8|-1|FFA289,5|-4|E8C7D4,11|-4|FFB298",
        0, 0.9, "超级瓦基丽武神"
    )
    override val MainBaseSuperWitch = ColorSchema.parse(
        999, 427, 1258, 692, "FFFCF3",
        "-1|-19|FECD58,2|-16|FFD75E,-1|-16|FBB158,5|-22|EDB45F,2|-13|F5A04B,-4|-19|FFBE53,5|-16|FFDC5F,8|-19|FFCE69,2|-19|FFD766,8|-13|FEE569",
        0, 0.9, "超级女巫"
    )
    override val MainBaseSuperMiner = ColorSchema.parse(
        999, 427, 1258, 692, "0E0F10",
        "-1|-10|4E5053,-22|-13|AAB0B9,-7|-4|706D6E,2|-1|282829,-19|-16|E3E3E0,-16|-16|DFDDD2,-19|-13|8F949B,-1|-22|818283,-13|-1|D5D3D5,14|-22|C4805F",
        0, 0.9, "超级矿工"
    )
    override val MainBaseSuperHogRider = ColorSchema.parse(
        999, 427, 1258, 692, "E4E4E4",
        "-1|-7|616161,-1|2|323232,11|2|FEE572,-13|-22|FFD457,11|-4|F8DD6D,-10|-22|FFD257,14|-1|E2DE6C,8|-16|744937,14|5|FDFB93,5|14|B77C66",
        0, 0.9, "超级野猪骑士"
    )
    override val MainBaseSneakyGoblin = ColorSchema.parse(
        999, 427, 1258, 692, "111210",
        "-1|-1|2B3125,-1|8|3F453C,8|-10|E9F751,5|-10|F1FF63,14|-22|C1D437,8|-7|CED339,5|-7|DDE54B,11|-19|BED53C,11|-22|C6DC41,11|17|A9BA39",
        0, 0.9, "隐秘哥布林"
    )
    override val MainBaseSuperYeti = ColorSchema.parse(
        999, 427, 1258, 692, "0D0D0D",
        "20|11|7793E7,17|11|7896E6,-22|-22|495089,20|8|566FB1,-13|11|7188DD,-1|2|3B3F45,-22|-13|D4AA60,-10|-13|F1C36F,14|8|536DAD,-22|11|728ADE",
        0, 0.9, "超级大雪怪"
    )
}
