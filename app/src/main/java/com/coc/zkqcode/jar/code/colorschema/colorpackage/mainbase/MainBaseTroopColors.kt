@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Main-base army-screen troop recognition features.
 * Auto-named from the trophy icons (masked NCC) and derived from real
 * 1280x720 screenshots of the "部队配置" screen (7x2 slot grid).
 * Region shared by all features: (290, 428, 1204, 693).
 * Regenerate with tools/gen_mainbase_troop_colors.py.
 */
interface IMainBaseTroopColors {
    val MainBaseGoblin: ColorSchema
    val MainBaseYeti: ColorSchema
    val MainBaseHealer: ColorSchema
    val MainBaseGiant: ColorSchema
    val MainBaseArcher: ColorSchema
    val MainBaseMiner: ColorSchema
    val MainBaseWizard: ColorSchema
    val MainBaseWallBreaker: ColorSchema
    val MainBasePekka: ColorSchema
    val MainBaseBarbarian: ColorSchema
    val MainBaseElectroDragon: ColorSchema
    val MainBaseDragon: ColorSchema
    val MainBaseBabyDragon: ColorSchema
}

object MainBaseTroopColors : IMainBaseTroopColors {
    override val MainBaseGoblin = ColorSchema.parse(
        290, 428, 1204, 693, "C7A04B",
        "5|8|997F61,-7|8|888A84,-4|8|917B5D,-1|-1|96A596,-1|14|81664C,5|14|947457,-1|8|937A5D,8|5|8FACA0,20|8|A48840,-7|11|96775A",
        0, 0.9, "哥布林"
    )
    override val MainBaseYeti = ColorSchema.parse(
        290, 428, 1204, 693, "F1F9FF",
        "2|-13|3180A9,17|17|2D2D6B,14|11|868BAF,-4|-1|CAD5F1,-22|11|BFC8D5,17|14|403F79,20|17|6B6AB9,17|20|6E6CB7,-10|17|B6C0D0,-16|20|989FAE",
        0, 0.9, "大雪怪"
    )
    override val MainBaseHealer = ColorSchema.parse(
        290, 428, 1204, 693, "FEFEFE",
        "14|-16|CFEBF8,-1|-1|B3B3B3,8|-19|D1D1CE,14|-10|D79E95,14|-7|CE928A,-1|14|E4D6CC,-13|-22|C6E1F2,-16|-22|C3DCEE,2|-16|BCB5B7,-16|-19|C0D6E6",
        0, 0.9, "天使"
    )
    override val MainBaseGiant = ColorSchema.parse(
        290, 428, 1204, 693, "494544",
        "-7|-1|B6B2B1,-7|-4|8F8784,-1|-1|131212,20|-19|FFB078,20|-16|FFAB74,-7|5|1F1B1A,-16|-13|DB946C,-22|-10|A16D55,-7|2|9D9795,17|-13|FFB37A",
        0, 0.9, "巨人"
    )
    override val MainBaseArcher = ColorSchema.parse(
        290, 428, 1204, 693, "CD3873",
        "-10|-16|F959AB,2|-22|F44A98,-22|-16|E84A95,-13|-19|F64797,11|-16|F5438D,-7|-19|FC55A8,2|-16|F24C98,5|-16|F64491,-7|-22|F54696,-4|-19|FC56A9",
        0, 0.9, "弓箭手"
    )
    override val MainBaseMiner = ColorSchema.parse(
        290, 428, 1204, 693, "333333",
        "14|2|55585B,11|2|545658,17|2|5D6062,20|2|606466,8|-1|86644D,20|5|5B5F62,14|-1|B5A07C,5|-7|D39A60,8|-4|BD8154,17|11|CDCF97",
        0, 0.9, "掘地矿工"
    )
    override val MainBaseWizard = ColorSchema.parse(
        290, 428, 1204, 693, "FFFEFF",
        "14|-16|221E1B,-7|5|7CD8FD,5|-1|FBDBDE,11|-16|342C25,5|11|111010,-4|-22|8C8689,2|-22|0F1227,-4|-13|A1B0B9,14|-22|C69B92,-10|-4|74CCF8",
        0, 0.9, "法师"
    )
    override val MainBaseWallBreaker = ColorSchema.parse(
        290, 428, 1204, 693, "1C1816",
        "-1|2|453E3A,-4|5|3E4548,-1|8|3A3533,-7|-1|48596B,-4|-7|624F40,-4|8|3A3F46,-1|-4|BC8F6B,-7|2|566B7F,-1|-7|D09E7A,2|8|514946",
        0, 0.9, "炸弹人"
    )
    override val MainBasePekka = ColorSchema.parse(
        290, 428, 1204, 693, "111112",
        "-22|-22|7FB1ED,2|-16|A3C1E3,5|-16|9EBCE1,17|-19|35517F,11|-19|23355E,2|-13|142549,14|-19|2E4670,11|-13|1A2C54,5|-13|1C2D51,8|-13|203059",
        0, 0.9, "皮卡超人"
    )
    override val MainBaseBarbarian = ColorSchema.parse(
        290, 428, 1204, 693, "BDBDBD",
        "20|-7|EFD96F,-1|-7|808080,8|-4|9A9A9A,20|11|F2B837,20|-10|F6E56F,20|8|E7AE36,20|-1|BA9F3F,2|-1|868686,20|-4|DAC65F,17|-7|F9EEB7",
        0, 0.9, "野蛮人"
    )
    override val MainBaseElectroDragon = ColorSchema.parse(
        290, 428, 1204, 693, "151515",
        "2|-1|FEFEFE,-10|5|85A2B2,17|-7|2C5BB2,17|-16|2957A9,-4|-19|CEEDFD,2|-7|FCFCFC,20|-4|214799,14|-16|3165BC,17|-22|14235C,14|-1|8B8CA7",
        0, 0.9, "雷电飞龙"
    )
    override val MainBaseDragon = ColorSchema.parse(
        290, 428, 1204, 693, "FEFEFE",
        "-1|-1|C4CACF,2|-16|7B6CEB,-1|-19|9686FD,-1|-16|6F5DD1,-16|-16|7DCCF7,5|-16|614FBF,-13|-16|77C4F0,8|-13|796BDB,5|-7|DCDCE4,2|-13|6856C6",
        0, 0.9, "飞龙"
    )
    override val MainBaseBabyDragon = ColorSchema.parse(
        290, 428, 1204, 693, "59311C",
        "11|-1|C06542,8|-1|A55E42,2|-1|86542F,11|2|9E683C,5|-1|B46C5C,8|2|A3644A,2|2|905D27,17|5|A7F167,8|5|9D6346,5|2|A76E3F",
        0, 0.9, "飞龙宝宝"
    )
}
