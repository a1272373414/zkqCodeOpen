@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Main-base TRAINING-CARD features migrated from the original script
 * (cocfz-apk-test doc/decrypt/awcocx_main.lua, 函数275a branches "造XX").
 * Region/offset conversion: see docs/复用优化方案20260919.md 11.3
 *   region (x1,y1,x2,y2) -> (y1, 719-x2, y2, 719-x1); offset (dx,dy) -> (dy, -dx)
 * The original flow locates a troop by its own feature on the current picker page and
 * flips the page (左/中/右) when not found, so no troop order/position is assumed.
 */
interface IMainBaseTrainCardColors {
    val TrainCard亡灵: ColorSchema
    val TrainCard兵栏界面: ColorSchema
    val TrainCard冰人: ColorSchema
    val TrainCard冰冻: ColorSchema
    val TrainCard冰障: ColorSchema
    val TrainCard回溯: ColorSchema
    val TrainCard图腾: ColorSchema
    val TrainCard地震: ColorSchema
    val TrainCard复苏: ColorSchema
    val TrainCard天使: ColorSchema
    val TrainCard女巫: ColorSchema
    val TrainCard学徒: ColorSchema
    val TrainCard小偷: ColorSchema
    val TrainCard巨人: ColorSchema
    val TrainCard巨矛: ColorSchema
    val TrainCard废墟女巫: ColorSchema
    val TrainCard弓箭: ColorSchema
    val TrainCard弹跳: ColorSchema
    val TrainCard急速: ColorSchema
    val TrainCard愤怒法术: ColorSchema
    val TrainCard战球: ColorSchema
    val TrainCard战营: ColorSchema
    val TrainCard战车: ColorSchema
    val TrainCard投手: ColorSchema
    val TrainCard根骑: ColorSchema
    val TrainCard武神: ColorSchema
    val TrainCard毒药: ColorSchema
    val TrainCard气球: ColorSchema
    val TrainCard法师: ColorSchema
    val TrainCard泰坦: ColorSchema
    val TrainCard滚木: ColorSchema
    val TrainCard炸弹: ColorSchema
    val TrainCard烈焰: ColorSchema
    val TrainCard熔炉: ColorSchema
    val TrainCard狂暴: ColorSchema
    val TrainCard猎手: ColorSchema
    val TrainCard猎犬: ColorSchema
    val TrainCard疗伤: ColorSchema
    val TrainCard皮卡: ColorSchema
    val TrainCard石头: ColorSchema
    val TrainCard矿工: ColorSchema
    val TrainCard空中战车: ColorSchema
    val TrainCard蔓生: ColorSchema
    val TrainCard蝙蝠: ColorSchema
    val TrainCard超亡: ColorSchema
    val TrainCard超偷: ColorSchema
    val TrainCard超宝: ColorSchema
    val TrainCard超巨: ColorSchema
    val TrainCard超巫: ColorSchema
    val TrainCard超弓: ColorSchema
    val TrainCard超怪: ColorSchema
    val TrainCard超投: ColorSchema
    val TrainCard超武: ColorSchema
    val TrainCard超法: ColorSchema
    val TrainCard超炸: ColorSchema
    val TrainCard超犬: ColorSchema
    val TrainCard超猪: ColorSchema
    val TrainCard超球: ColorSchema
    val TrainCard超矿: ColorSchema
    val TrainCard超蛮: ColorSchema
    val TrainCard超龙: ColorSchema
    val TrainCard部队发射器: ColorSchema
    val TrainCard野猪: ColorSchema
    val TrainCard野蛮: ColorSchema
    val TrainCard钻机: ColorSchema
    val TrainCard镜像: ColorSchema
    val TrainCard陨石戈仑: ColorSchema
    val TrainCard隐形: ColorSchema
    val TrainCard雪怪: ColorSchema
    val TrainCard雷电: ColorSchema
    val TrainCard雷龙: ColorSchema
    val TrainCard飞艇: ColorSchema
    val TrainCard飞龙: ColorSchema
    val TrainCard骷髅: ColorSchema
    val TrainCard鲁伊: ColorSchema
    val TrainCard龙宝: ColorSchema
    val TrainCard龙骑: ColorSchema
}

object MainBaseTrainCardColors : IMainBaseTrainCardColors {
    override val TrainCard亡灵 = ColorSchema.parse(
        23, 429, 1279, 693, "B07B2D",
        "-4|-15|FAC66A,-5|-48|FFDB85,24|-38|FFF7BD,22|-1|D2A968,30|-11|F2C782,35|-23|FFFFFF,35|-34|FFFFDA,42|-14|B17E31",
        0, 0.9, "亡灵训练卡片(造亡灵)"
    )
    override val TrainCard兵栏界面 = ColorSchema.parse(
        74, 429, 497, 469, "CC883F",
        "0|-5|F0CA87,5|1|FAFFFF,5|-4|FAFFFF,10|-1|D08E42,10|-7|F0CA87,13|1|C8803C,-2|2|C47839,11|-10|F0CA87,-1|-10|F0CA87",
        0, 0.9, "兵栏界面训练卡片(造兵栏界面)"
    )
    override val TrainCard冰人 = ColorSchema.parse(
        23, 429, 1279, 693, "FFFF8D",
        "6|0|FFFF8B,-3|6|E7EDED,7|9|E8ECEC,14|-9|D4D6D8,23|8|F3D984,30|5|DDCB7D,-25|-23|F2C689,-20|-26|FFDFC3",
        0, 0.9, "冰人训练卡片(造冰人)"
    )
    override val TrainCard冰冻 = ColorSchema.parse(
        23, 429, 1279, 693, "FBDC91",
        "-8|-12|FCDC86,10|-15|FBDB87,18|-2|FFFFFF,23|-29|FFFF71,26|-38|FFFFF1,-25|-26|FFF547,-20|-34|FFF34D,-30|-25|FFF546",
        0, 0.9, "冰冻训练卡片(造冰冻)"
    )
    override val TrainCard冰障 = ColorSchema.parse(
        23, 429, 1279, 693, "D98D87",
        "-3|6|CE7C79,-37|-39|FF9D7E,-25|-31|FF8A74,25|6|FF8968,35|3|FF6B57,24|-8|FFB89E,29|-33|FF7C60,1|-58|8FFFFF,-6|-24|C2ADA4",
        0, 0.9, "冰障训练卡片(造冰障)"
    )
    override val TrainCard回溯 = ColorSchema.parse(
        23, 429, 1279, 693, "6C4EC2",
        "1|-22|7957F8,15|-25|6D4FFF,-1|-31|D7ABE6,39|-4|DDC6FF,44|-13|FFFFFF,35|-28|EDC1ED,28|-34|D8CEE1,16|-32|C495D5",
        0, 0.9, "回溯训练卡片(造回溯)"
    )
    override val TrainCard图腾 = ColorSchema.parse(
        23, 429, 1279, 693, "7A6A65",
        "5|4|716762,13|3|8B6755,15|12|82604F,0|-17|D3BC9F,0|-28|D2BC9E,14|-13|CAB292,14|-29|D1B997,27|-35|AC6C62,-17|-31|AE6F65,2|-41|2D5CAE",
        0, 0.9, "图腾训练卡片(造图腾)"
    )
    override val TrainCard地震 = ColorSchema.parse(
        23, 429, 1279, 693, "6393C7",
        "-3|-6|6A9CCF,-5|-18|8199B5,-5|-25|6E8AA9,14|-19|577194,13|-33|607B9C,38|-2|343F47,36|-28|333C45,-5|-32|4D6E8E",
        0, 0.9, "地震训练卡片(造地震)"
    )
    override val TrainCard复苏 = ColorSchema.parse(
        23, 429, 1279, 693, "4ED0FF",
        "7|3|48C2FF,-4|8|59788F,17|9|708EA5,22|-9|FBFFFF,12|-17|B1C1D1,0|-30|9AA4AB,12|-30|BEDAF0,32|-30|C4D3DF,3|-5|4ACBFF",
        0, 0.9, "复苏训练卡片(造复苏)"
    )
    override val TrainCard天使 = ColorSchema.parse(
        23, 429, 1279, 693, "C1E0FF",
        "-2|-26|A0C2FB,-5|-39|EBF3FF,-12|12|707EB2,-16|2|95AEEC,-17|-31|D3DDF4,-13|-45|EDF2FF,-38|-22|CAD3EA,20|10|96B2FC,31|11|F9FCFF,38|17|4C6DA1",
        0, 0.9, "天使训练卡片(造天使)"
    )
    override val TrainCard女巫 = ColorSchema.parse(
        23, 429, 1279, 693, "716071",
        "-5|-13|E957FF,7|-13|713733,26|14|C5B2DD,33|5|C3B3EE,30|-10|E558FF,31|-12|E855FF,29|-18|D06F71",
        0, 0.9, "女巫训练卡片(造女巫)"
    )
    override val TrainCard学徒 = ColorSchema.parse(
        23, 429, 1279, 693, "FF5BDA",
        "17|-5|FF59D6,35|-9|FF57D0,20|4|882199,28|8|312F7B,8|13|434690,16|31|B4DEFF,28|25|BDE6FF,29|10|37317F",
        0, 0.9, "学徒训练卡片(造学徒)"
    )
    override val TrainCard小偷 = ColorSchema.parse(
        23, 429, 1279, 693, "2A735A",
        "-7|-15|52A37A,-12|-36|3C767E,7|-49|9BFEBF,16|-53|73FAC7,17|-45|6CEAB9,31|-4|A1E7FB,21|-33|268056,24|-44|78F3C4,-27|-29|48698C",
        0, 0.9, "小偷训练卡片(造小偷)"
    )
    override val TrainCard巨人 = ColorSchema.parse(
        23, 429, 1279, 693, "90CAFF",
        "-5|-12|81B9FF,-17|-18|739DFD,-31|4|7097DD,-36|-32|6995EA,-34|-47|2B7CFB,-35|-65|90CAFF,3|-30|82B5FF,4|-37|628CEB",
        0, 0.9, "巨人训练卡片(造巨人)"
    )
    override val TrainCard巨矛 = ColorSchema.parse(
        23, 429, 1279, 693, "EEAFFF",
        "-3|-5|F1ADFF,-23|-32|707472,11|-24|939594,18|-27|706464,-41|-73|6F655D,-49|-76|3A2E57,-22|-77|9B9E99,-12|-77|FFFFFF,15|-51|FFD7FF",
        0, 0.9, "巨矛训练卡片(造巨矛)"
    )
    override val TrainCard废墟女巫 = ColorSchema.parse(
        23, 429, 1279, 693, "FFFFFF",
        "41|-5|FFFFFF,31|-3|57FFFF,26|-11|AB6596,-29|-19|D27BA5,-9|19|BABBFF,4|26|C0C2EB,44|14|E9BBB2,21|8|B8B8FF",
        0, 0.9, "废墟女巫训练卡片(造废墟女巫)"
    )
    override val TrainCard弓箭 = ColorSchema.parse(
        23, 429, 1279, 693, "748ADC",
        "24|-1|7B96EF,35|-1|2D1C5C,40|-6|7036CA,0|-10|48578B,1|-28|753AC7,14|-47|743AC9,11|-60|A554FB",
        0, 0.9, "弓箭训练卡片(造弓箭)"
    )
    override val TrainCard弹跳 = ColorSchema.parse(
        23, 429, 1279, 693, "14CE53",
        "13|3|16E260,11|-5|20FB90,35|-5|42FFDC,0|-27|318D5F,19|-22|EFFFFF,25|-26|45EDC5,35|-32|60FADC,5|-37|379665",
        0, 0.9, "弹跳训练卡片(造弹跳)"
    )
    override val TrainCard急速 = ColorSchema.parse(
        23, 429, 1279, 693, "B97BFF",
        "-6|-3|B97BFF,-1|-11|A85FFF,-6|-13|DEA2FF,13|-3|9E55F6,33|-2|AE5FD6,27|-9|FBEEFF,13|-36|A593E2",
        0, 0.9, "急速训练卡片(造急速)"
    )
    override val TrainCard愤怒法术 = ColorSchema.parse(
        23, 429, 1279, 693, "DE5BE1",
        "9|8|C76AF7,8|25|C968FE,-2|34|912ABE,-18|23|3C43F6,-2|7|A63CD4,-1|0|DF5DE2,-5|-21|5C718D,37|-2|8736E3",
        0, 0.9, "愤怒法术训练卡片(造愤怒法术)"
    )
    override val TrainCard战球 = ColorSchema.parse(
        23, 429, 1279, 693, "B8FFFF",
        "0|1|CCFFFF,-3|25|98E6FF,-15|7|7DD2FC,0|7|A0D5E2,-45|34|DFB065,-18|13|DEAE64,-21|10|DEAE64,-21|13|DEAE64,-60|22|DEAF65,-60|25|DEAF65,-57|22|DEAF65,-12|7|92FFFF",
        0, 0.9, "战球训练卡片(造战球, 2026-09-21 由真实卡片重新派生)"
    )
    override val TrainCard战营 = ColorSchema.parse(
        23, 429, 1279, 693, "3F87BB",
        "5|6|4088BC,8|8|787979,13|-11|7E807F,-9|-8|7F8183,-12|-40|393EDB,-11|-69|3E44E7,8|-33|3B40DB,5|-44|11125A,5|-55|101150,13|-78|3E44ED",
        0, 0.9, "战营训练卡片(造战营)"
    )
    override val TrainCard战车 = ColorSchema.parse(
        23, 429, 1279, 693, "21375D",
        "-19|-57|10254F,-19|-60|112853,-16|-15|1B2D59,-16|-51|0E1454,-1|0|20375E,-22|3|27355A,-22|-57|284483,-22|-60|315198,-16|-54|111865,-19|-54|122953,-4|0|1E3460,-19|-36|31416E",
        0, 0.9, "战车训练卡片(造战车, 2026-09-21 由真实卡片重新派生)"
    )
    override val TrainCard投手 = ColorSchema.parse(
        23, 429, 1279, 693, "F67073",
        "-6|-19|F46D6E,0|-26|FC8EA3,36|-7|E780A7,42|-15|E57D9C,39|-26|EF8093,-19|-49|EA82AC,-5|-48|F8787A,5|-56|FF919F",
        0, 0.9, "投手训练卡片(造投手)"
    )
    override val TrainCard根骑 = ColorSchema.parse(
        23, 429, 1279, 693, "506BD2",
        "5|7|4B5089,28|10|8CAFFF,27|-16|2E51C5,19|-19|688AFF,-7|-25|6F5F81,-3|-32|59507B,-11|-39|1A1A30,19|-26|6487FF",
        0, 0.9, "根骑训练卡片(造根骑)"
    )
    override val TrainCard武神 = ColorSchema.parse(
        23, 429, 1279, 693, "768FDC",
        "5|-3|819FF3,0|-23|1235A2,10|-25|7A9CF2,19|-21|7698EB,37|8|88AFFF,42|19|112585,49|14|1349D6",
        0, 0.9, "武神训练卡片(造武神)"
    )
    override val TrainCard毒药 = ColorSchema.parse(
        23, 429, 1279, 693, "1E8CFF",
        "5|4|1A86FF,16|3|1055F6,29|-10|4BE9FF,33|-3|0E6EF2,31|-17|67EAFF,30|-24|1FB9FA,16|-26|B8C8F2,-1|-26|AABEF3",
        0, 0.9, "毒药训练卡片(造毒药)"
    )
    override val TrainCard气球 = ColorSchema.parse(
        23, 429, 1279, 693, "93A7B9",
        "-9|-30|395B9C,-4|-32|3662C0,7|-31|4188FF,8|-29|3F82FF,10|-35|5C5266,24|-38|0D1168,27|-10|616B81,-43|-38|8986EC,-27|-39|3C3575",
        0, 0.9, "气球训练卡片(造气球)"
    )
    override val TrainCard法师 = ColorSchema.parse(
        23, 429, 1279, 693, "8A9EDE",
        "10|6|A9C5FF,10|-4|9BB7FA,34|11|283152,47|-2|B07C3F,46|-20|B48041,-10|-18|D4D2F6,-5|-46|8A878A,14|-34|42548D",
        0, 0.9, "法师训练卡片(造法师)"
    )
    override val TrainCard泰坦 = ColorSchema.parse(
        23, 429, 1279, 693, "383A53",
        "12|-1|3B3A57,3|-14|FFFFFF,16|-17|FFFFFF,-28|-3|AC9BBC,-28|-7|B2A4C4,-32|-15|FFFFFF,-9|-24|444660,-9|-32|E9DEDC,2|-35|E8DFDD,-6|-42|FFE0CF",
        0, 0.9, "泰坦训练卡片(造泰坦)"
    )
    override val TrainCard滚木 = ColorSchema.parse(
        23, 429, 1279, 693, "6E5751",
        "6|-6|68544E,13|-12|81726F,37|3|263192,80|-11|242B79,120|-13|2F45D1,156|-13|928E8C,115|44|4F72B4,125|78|496BA8,149|69|888382,158|55|202020,191|77|5F5C5B,105|102|466FB4",
        0, 0.9, "滚木训练卡片(造滚木)"
    )
    override val TrainCard炸弹 = ColorSchema.parse(
        23, 429, 1279, 693, "B2CEDD",
        "-2|-7|0D0F10,0|-13|0E1010,6|-18|BBD7E5,-2|23|6E6F68,-2|14|0D0D0D,21|-1|0D0D0E,25|-6|0F1111,21|-13|C1DDEA,29|-21|608AC5,-20|3|2E3135,-18|22|40474C",
        0, 0.9, "炸弹训练卡片(造炸弹)"
    )
    override val TrainCard烈焰 = ColorSchema.parse(
        23, 429, 1279, 693, "5167A2",
        "2|-10|4C70BF,-35|-11|8B7A73,82|-24|4268B5,75|-49|5590F3,22|-72|0D0D0D,16|-72|3A48CE,-78|-110|524C53,-43|-111|3C388D,45|-10|243251",
        0, 0.9, "烈焰训练卡片(造烈焰)"
    )
    override val TrainCard熔炉 = ColorSchema.parse(
        23, 429, 1279, 693, "17244B",
        "6|4|192F77,19|6|1A2C79,16|-6|34B8F4,18|-5|35BDF6,23|-8|24AAED,19|-8|F4FFFF,4|-11|2EC1F4,7|-7|2ABCF3,8|-10|F4FFFF",
        0, 0.9, "熔炉训练卡片(造熔炉)"
    )
    override val TrainCard狂暴 = ColorSchema.parse(
        23, 429, 1279, 693, "901F61",
        "5|-1|952763,21|-2|F649A9,27|6|FE8CEE,-17|-25|D6AEB5,-4|-21|773250,-3|-31|BA898A,23|-18|BE3377",
        0, 0.9, "狂暴训练卡片(造狂暴)"
    )
    override val TrainCard猎手 = ColorSchema.parse(
        23, 429, 1279, 693, "C1FFFF",
        "-48|-33|38EAFF,-45|-36|3CF1FF,-48|-30|47E8FF,-54|-27|83B7FF,-3|0|C2FFFF,3|-3|BFFFFF,3|0|CAFFFF,0|-27|39EDFF,0|-24|31E6FF,-45|-33|7DFBFF,-45|-27|71C9D7,24|27|FFE594",
        0, 0.9, "猎手训练卡片(造猎手, 2026-09-21 由真实卡片重新派生)"
    )
    override val TrainCard猎犬 = ColorSchema.parse(
        23, 429, 1279, 693, "202435",
        "-9|-7|383539,28|-2|484D59,37|-2|707F8E,46|-1|4B658B,-14|-29|837777,13|-30|2B2D37,19|-47|FFFFFF,46|-18|7389A2,53|-36|112475",
        0, 0.9, "猎犬训练卡片(造猎犬)"
    )
    override val TrainCard疗伤 = ColorSchema.parse(
        23, 429, 1279, 693, "63B7E4",
        "-2|-9|4B96D3,-2|-18|7AA2C1,-5|-24|76AACA,12|-4|96EEFF,27|-12|DCF7F7,23|-23|BAC8D7,18|-34|F0FFFF,45|-11|98EDFE,6|-33|94A6B4",
        0, 0.9, "疗伤训练卡片(造疗伤)"
    )
    override val TrainCard皮卡 = ColorSchema.parse(
        23, 429, 1279, 693, "1F1619",
        "-4|-7|955C5A,8|-1|9F7665,17|-5|504E4D,5|-24|BC937B,-18|-22|E59F6F,-34|-22|A83176,38|-20|A22A6C,30|-19|683A2D",
        0, 0.9, "皮卡训练卡片(造皮卡)"
    )
    override val TrainCard石头 = ColorSchema.parse(
        23, 429, 1279, 693, "FF96FF",
        "6|2|FFCBFF,23|-2|A7C2DA,48|17|B3CEE9,52|6|FFD4FF,58|-8|DAF6FF,49|-5|B0CCE5,4|-33|AFB4B9,28|-33|C4C8C7",
        0, 0.9, "石头训练卡片(造石头)"
    )
    override val TrainCard矿工 = ColorSchema.parse(
        23, 429, 1279, 693, "88ACFF",
        "9|-2|B3DDFF,-8|-19|37407A,27|-4|FFC08F,-12|-54|64625E,-18|-54|5E5B58,-15|-60|AFDCE1,15|-45|7F7975,14|-22|6E8CF9",
        0, 0.9, "矿工训练卡片(造矿工)"
    )
    override val TrainCard空中战车 = ColorSchema.parse(
        23, 429, 1279, 693, "466ECD",
        "-8|-4|272C38,-15|-4|8A736E,50|-2|5A8AF8,56|-20|527ED1,62|-23|C1BDC3,74|-34|FFFFFF,-3|-58|404872,-43|-109|544E88,-32|-131|3D3E85,-2|-118|3258A4,28|-129|424177,31|-115|4D6CFF",
        0, 0.9, "空中战车训练卡片(造空中战车)"
    )
    override val TrainCard蔓生 = ColorSchema.parse(
        23, 429, 1279, 693, "31AC82",
        "10|4|33C388,0|-6|94FFFF,12|-5|9EFFFF,22|-11|6BFFF1,26|-12|71FAB5,27|-19|58A59C,19|-28|DFD2AE,10|-30|AAECEC",
        0, 0.9, "蔓生训练卡片(造蔓生)"
    )
    override val TrainCard蝙蝠 = ColorSchema.parse(
        23, 429, 1279, 693, "E2C0E2",
        "-4|-16|E1C0CA,-5|-32|D7B0B8,16|-12|60273A,11|-4|752E48,44|-2|B389AF,54|-1|7D4D75,59|-32|734C70,43|-31|D4A6E9,26|-37|947DC5",
        0, 0.9, "蝙蝠训练卡片(造蝙蝠)"
    )
    override val TrainCard超亡 = ColorSchema.parse(
        23, 429, 1279, 693, "DCC35F",
        "9|-1|BEA63E,25|-5|51BEFF,29|-20|4BBEFF,30|-25|8365A4,36|-26|8D75B3,-5|-22|D0E7FF,-11|-32|E9D186",
        0, 0.9, "超亡训练卡片(造超亡)"
    )
    override val TrainCard超偷 = ColorSchema.parse(
        23, 429, 1279, 693, "40DACD",
        "13|0|63FEF2,31|-10|3DC2AF,37|-20|41C2AC,2|-27|44C7E4,-7|-53|60E4CC,-9|-47|346DD3,38|-46|3A65C1,28|-58|4CF7DB,-18|-59|304362",
        0, 0.9, "超偷训练卡片(造超偷)"
    )
    override val TrainCard超宝 = ColorSchema.parse(
        23, 429, 1279, 693, "5FE2B6",
        "0|-8|A2FFFC,-3|-20|74FFE5,-14|-27|327B64,-19|-26|4E8265,-26|-43|B78B81,-28|18|433433,-31|6|FFFFFF,-44|15|6D4F44,-35|-32|3B404C",
        0, 0.9, "超宝训练卡片(造超宝)"
    )
    override val TrainCard超巨 = ColorSchema.parse(
        23, 429, 1279, 693, "2089FF",
        "6|5|AFDCFF,13|10|7EADFF,17|-1|1D81FF,26|-17|9CDEFF,18|-16|B1E5FF,4|-11|97C7FF,-20|-16|7AA7FF,-19|-36|1E4FB4",
        0, 0.9, "超巨训练卡片(造超巨)"
    )
    override val TrainCard超巫 = ColorSchema.parse(
        23, 429, 1279, 693, "F2DAFF",
        "8|-3|EDD1FF,0|-12|FF89FF,-5|-20|52C3FF,-32|-13|FF76FF,-31|-28|A9507F,-36|-38|B7547C,-11|-38|78FFFF,-7|-45|9FFFFF,-13|-49|FF7EB0",
        0, 0.9, "超巫训练卡片(造超巫)"
    )
    override val TrainCard超弓 = ColorSchema.parse(
        23, 429, 1279, 693, "ADC1FF",
        "0|-5|B8CBFF,8|-7|3AA6FF,12|-16|FFFFFF,43|-11|FA99FF,45|-19|C365FF,51|-18|4D3EB5,-16|-40|D47DFF,-21|-26|FFFFFF",
        0, 0.9, "超弓训练卡片(造超弓)"
    )
    override val TrainCard超怪 = ColorSchema.parse(
        23, 429, 1279, 693, "4F8AA9",
        "4|5|4F82A1,5|-7|D0685C,11|1|EB7263,-16|22|64B1DB,-14|-11|5F82A1,-32|-24|E88AD8,15|25|76CEFF,23|29|67C3F4,19|1|EE7365",
        0, 0.9, "超怪训练卡片(造超怪)"
    )
    override val TrainCard超投 = ColorSchema.parse(
        23, 429, 1279, 693, "B4533E",
        "17|6|CC616E,31|10|B35344,37|4|E0798C,43|10|D4627A,20|-21|3F88E2,-17|-36|BD0D8E,-6|-37|CE5D58,-17|-17|A34554,25|16|658599",
        0, 0.9, "超投训练卡片(造超投)"
    )
    override val TrainCard超武 = ColorSchema.parse(
        23, 429, 1279, 693, "8FADFC",
        "6|-15|88A4F4,12|-24|8AA5FF,26|-4|1B41C3,32|-8|2346C2,32|-17|1839AC,37|-27|1D44B7,-10|-58|849EE5,-14|-62|637DC5,14|-64|1E47C6,10|-56|B3D7FF",
        0, 0.9, "超武训练卡片(造超武)"
    )
    override val TrainCard超法 = ColorSchema.parse(
        23, 429, 1279, 693, "4C6AAA",
        "4|3|5272B2,17|9|7ECAFF,35|12|121E45,44|12|2B5DCE,1|-22|4E5570,3|-32|81FFFF,6|-30|7FFFFF,22|-33|545D7F,33|-19|7CFFFF",
        0, 0.9, "超法训练卡片(造超法)"
    )
    override val TrainCard超炸 = ColorSchema.parse(
        23, 429, 1279, 693, "ABC6D7",
        "-6|-2|86A0B4,-23|1|EEF5FF,-23|-12|EDE5F1,-21|-30|6C819E,5|-32|3D4143,17|-37|121213,17|-46|18191A,27|-1|254AA6,33|-12|649CFF",
        0, 0.9, "超炸训练卡片(造超炸)"
    )
    override val TrainCard超犬 = ColorSchema.parse(
        23, 429, 1279, 693, "E7E764",
        "5|1|FFFF73,8|3|FFFF73,23|-18|CD6E32,19|-22|D77F45,6|-13|FEFFFF,-6|13|8D797A,4|19|8E8181,-5|19|4E291E,36|10|FFFF72",
        0, 0.9, "超犬训练卡片(造超犬)"
    )
    override val TrainCard超猪 = ColorSchema.parse(
        23, 429, 1279, 693, "455A91",
        "3|-16|29313C,-9|-7|293340,-13|-18|485B8B,-11|-28|CEFFFF,-11|-38|C3FFFF,-16|-43|6182BC,4|-47|7095D8,15|-30|6081E4",
        0, 0.9, "超猪训练卡片(造超猪)"
    )
    override val TrainCard超球 = ColorSchema.parse(
        23, 429, 1279, 693, "1E2024",
        "-1|-12|24262B,31|-4|384252,46|-5|5A6579,48|1|5E6B7F,26|29|1650C0,30|26|1952B5,30|38|468DFF,-21|-16|171717,-25|-22|493F3C",
        0, 0.9, "超球训练卡片(造超球)"
    )
    override val TrainCard超矿 = ColorSchema.parse(
        23, 429, 1279, 693, "A9C6FF",
        "14|-5|9BBAFF,6|-7|1E5381,9|-15|779AEB,3|-19|8FAFF9,-23|-32|153E81,2|-43|3EACFF,13|-44|49A6FF,17|-34|405C83",
        0, 0.9, "超矿训练卡片(造超矿)"
    )
    override val TrainCard超蛮 = ColorSchema.parse(
        23, 429, 1279, 693, "111322",
        "12|4|0E101C,22|-4|83F9FF,26|-1|74F8FF,41|7|787378,35|-2|4B507D,27|-9|86BCFF,-2|-50|418DC2,-4|-54|46A2DE,-1|-35|3D406E",
        0, 0.9, "超蛮训练卡片(造超蛮)"
    )
    override val TrainCard超龙 = ColorSchema.parse(
        23, 429, 1279, 693, "FDFFFF",
        "12|18|5DD4FF,11|24|D3F5FF,18|11|D8FFFF,22|-2|142750,10|-31|4B5E7B,-34|-16|2562C1,-35|-35|5283C4,37|21|63C1F6,4|28|8AE2FF",
        0, 0.9, "超龙训练卡片(造超龙)"
    )
    override val TrainCard部队发射器 = ColorSchema.parse(
        23, 429, 1279, 693, "5353A2",
        "-2|-10|365E9F,-3|-16|3C66AA,17|9|4C443E,14|-4|584E47,40|34|242C84,49|7|3E4BD6,41|-48|4757FA,50|-45|F2FFFF,58|-36|4C5CFF,67|-38|F9FFFF,66|-46|4958FC",
        0, 0.9, "部队发射器训练卡片(造部队发射器)"
    )
    override val TrainCard野猪 = ColorSchema.parse(
        23, 429, 1279, 693, "8C92FE",
        "-7|-9|101225,8|-6|161A2D,13|-13|F5FFFF,-15|-32|454767,-9|-38|3D4C7A,-11|-52|343E61,-4|-58|3D4C7A,27|-26|99AADD",
        0, 0.9, "野猪训练卡片(造野猪)"
    )
    override val TrainCard野蛮 = ColorSchema.parse(
        23, 429, 1279, 693, "699AC3",
        "0|-7|58DDFF,-8|0|7BA1BB,-8|-7|54D2FF,-9|6|32368C,-18|3|121535,-7|-21|6595FD,4|-21|6DA7FF,-9|-35|43C6FF,1|-35|3DB7F5",
        0, 0.9, "野蛮训练卡片(造野蛮)"
    )
    override val TrainCard钻机 = ColorSchema.parse(
        23, 429, 1279, 693, "546CA2",
        "7|-12|2A317F,-16|-62|494949,-11|-82|5F5C5B,-18|-130|4F5253,45|-66|768AFF,66|-98|4C4F50,89|-63|A29CFF,83|-49|263155,78|-41|0F1039,103|-28|424347,105|-19|858384",
        0, 0.9, "钻机训练卡片(造钻机)"
    )
    override val TrainCard镜像 = ColorSchema.parse(
        23, 429, 1279, 693, "E2DA29",
        "10|-7|E8EE33,14|-6|ECFA38,30|-12|ECFA2E,31|-6|EBF92D,29|-23|FFFFED,17|-34|B7B2A1,-8|-29|C69B6D,4|-29|B58D67",
        0, 0.9, "镜像训练卡片(造镜像)"
    )
    override val TrainCard陨石戈仑 = ColorSchema.parse(
        23, 429, 1279, 693, "758399",
        "-9|-3|7C8BA4,-9|-24|FFFFE5,-4|-22|FFFFE7,21|2|788195,26|-8|6D758B,31|-15|7686A0,24|-20|FFFFDE,20|-17|FFFFF1,14|-29|343548",
        0, 0.9, "陨石戈仑训练卡片(造陨石戈仑)"
    )
    override val TrainCard隐形 = ColorSchema.parse(
        23, 429, 1279, 693, "A5F266",
        "7|2|AEF56B,15|-2|9DED5A,14|-19|AAC777,17|-26|D8DF96,-1|-26|B0C476,36|-19|B8D7A5,27|-26|ECD7E8,17|-44|9EB69F",
        0, 0.9, "隐形训练卡片(造隐形)"
    )
    override val TrainCard雪怪 = ColorSchema.parse(
        23, 429, 1279, 693, "E3CAC1",
        "16|2|6D454C,23|-7|DFCAC3,-18|-51|FFFAF3,-14|-59|AB6A6A,0|-53|FFFCF6,2|-62|B87073,10|-59|B36D6D,17|-58|FFFAF5",
        0, 0.9, "雪怪训练卡片(造雪怪)"
    )
    override val TrainCard雷电 = ColorSchema.parse(
        23, 429, 1279, 693, "FF2F0E",
        "4|-5|FF6D0E,6|-12|FF850E,3|-27|FFDA39,3|-45|FFDA3B,17|-14|FFD944,35|-28|FFF166,37|-42|FFE430,32|-53|FFFF18",
        0, 0.9, "雷电训练卡片(造雷电)"
    )
    override val TrainCard雷龙 = ColorSchema.parse(
        23, 429, 1279, 693, "A15D2E",
        "14|-2|904C26,15|11|4A244D,19|9|4A2656,27|15|B49A44,-9|-24|8E5E38,-14|-31|FFFFAA,-9|-43|F2A755",
        0, 0.9, "雷龙训练卡片(造雷龙)"
    )
    override val TrainCard飞艇 = ColorSchema.parse(
        23, 429, 1279, 693, "AF8B6A",
        "-4|-13|4A517A,33|-7|3438A2,56|-11|282F92,20|-56|41D0FF,-9|-61|594A40,44|-99|5F5AD9,93|-112|5D66FF,133|-90|5A6BFF,87|-97|4B5BFE,145|-56|3E3E42,109|-2|1D267C,94|33|62575C",
        0, 0.9, "飞艇训练卡片(造飞艇)"
    )
    override val TrainCard飞龙 = ColorSchema.parse(
        23, 429, 1279, 693, "7A46FF",
        "6|6|6444C1,6|-4|77384A,-2|-15|E06373,7|-39|F7828C,-4|-46|FF9496,-42|-43|8A788C,-38|-40|E5687C,-4|-4|A366FF",
        0, 0.9, "飞龙训练卡片(造飞龙)"
    )
    override val TrainCard骷髅 = ColorSchema.parse(
        23, 429, 1279, 693, "0DC20D",
        "3|0|0ED10D,24|-9|18A326,-30|-24|23D443,-48|-30|2CF22C,-3|0|0EBA0D,27|-6|17B639,0|3|0DB610,9|-18|24A829,-33|-27|3ED34C,-30|-48|3EDA52,3|3|0DC30E,-6|3|0EA50D",
        0, 0.9, "骷髅训练卡片(造骷髅, 2026-09-21 由真实卡片重新派生)"
    )
    override val TrainCard鲁伊 = ColorSchema.parse(
        23, 429, 1279, 693, "4976CC",
        "13|-4|5692F8,23|-8|61A1FF,22|29|396CEF,27|34|4D8CFF,23|14|162A87,6|-15|5C86AB,20|-19|6392B9,50|-5|5997C4",
        0, 0.9, "鲁伊训练卡片(造鲁伊)"
    )
    override val TrainCard龙宝 = ColorSchema.parse(
        23, 429, 1279, 693, "4E5FC4",
        "11|5|495FC9,-3|12|2B6739,6|14|174628,21|13|44AAAE,25|4|113B20,-23|-52|7EF8A1,-15|-42|5BE39B,3|-43|BCFBFF",
        0, 0.9, "龙宝训练卡片(造龙宝)"
    )
    override val TrainCard龙骑 = ColorSchema.parse(
        23, 429, 1279, 693, "121725",
        "10|-3|78838F,5|-12|111212,18|-17|111212,9|-14|111111,22|-11|DAF1F7,24|-4|4750E6,41|-13|0E1C53,49|-7|0E1F55,55|1|0E2270",
        0, 0.9, "龙骑训练卡片(造龙骑)"
    )
}
