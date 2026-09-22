@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IFeatureColors {
    val TrainTroops: ColorSchema
    val RebuildBuilderBase: ColorSchema
    val UpgradeToTH6: ColorSchema
    val OrangeTutorialArrow: ColorSchema
    val ResearchIcon: ColorSchema
    val ResearchIcon2: ColorSchema
    val WhiteNumberColor: ColorSchema
    val MiddleGreenConfirm: ColorSchema

    // Migrated from the legacy freescript UI lookup table (函数275a)
    val BottomLeftReturnToCamp: ColorSchema
    val ReturnToCampMain: ColorSchema
    val ReturnToCampMain2: ColorSchema
    val ReturnToCampMain3: ColorSchema
    val ReturnToCampMain4: ColorSchema
    val ReturnToCampMain5: ColorSchema
    val ReturnToCampBuilder: ColorSchema
    val CommonDialog: ColorSchema
}

object FeatureColors : IFeatureColors {
    override val TrainTroops = ColorSchema.parse(
        17, 483, 88, 559, "A2BFF1", "-7|15|D3DFFF,-13|7|87FFFF,-13|-5|60F8FF,-10|-11|5CF2FF,-3|-11|4CD3FF,10|-10|C9AC97,17|-14|FFFFFF,17|-3|FAFF78,15|0|F9FF6E", 0, 0.9
    )
    override val RebuildBuilderBase = ColorSchema.parse(
        580, 501, 706, 625, "DAE4F3", "4|-10|E2E1ED,12|-16|D3D2E1,14|-18|D2D1E0,10|-3|DCEAFF,23|2|5684F0,24|2|5583F2,32|5|365AA7,38|13|6187F0,42|15|6D88EB", 0, 0.9, "重建夜世界帆船"
    )
    override val UpgradeToTH6 = ColorSchema.parse(
        700, 189, 725, 221, "1919FF", "2|-6|1919FF,2|-6|1919FF,6|0|1919FF,8|2|1919FF,11|4|1919FF,7|8|1919FF,0|7|1919FF,0|5|1919FF,1|1|1919FF", 0, 0.9, "需要将大本营升至6级"
    )
    override val OrangeTutorialArrow = ColorSchema.parse(
        513, 415, 843, 668, "22ADFD", "6|0|20B5FC,12|0|20BAFD,17|0|20B9FD,23|0|1FAFFD,0|23|58EAF1,6|23|51E7F2,12|23|4FE4F1,17|23|4DE3F2,23|23|4CE0F0", 0, 0.9, "教程橙色箭头"
    )
    override val ResearchIcon = ColorSchema.parse(
        374, 18, 900, 62, "F900D1", "3|0|F004A4,7|0|ED0599,10|0|E80691,13|0|E00587,0|8|F001AD,3|8|4BBFD1,7|8|59E2F2,10|8|FFFFFF,13|8|FFFFFF", 0, 0.9,
    )
    override val ResearchIcon2 = ColorSchema.parse(
        374, 18, 900, 62, "F700CE", "3|0|EF0094,7|0|E8028E,10|0|E40389,13|0|DB0380,32|-2|FAF8F7,30|7|B9AFA5,7|7|62D8E7,10|7|FFFFFF,13|7|FFFFFF", 0, 0.9
    )
    override val WhiteNumberColor = ColorSchema.parse(
        338, 102, 966, 560, "FFFFFF", "-1|1|FFFFFF,-1|2|FFFFFF,-1|3|FFFFFF,-1|4|FFFFFF,7|5|FFFFFF,7|4|FFFFFF,7|3|FFFFFF,7|2|FFFFFF,7|1|FFFFFF", 0, 0.99,
    )
    override val MiddleGreenConfirm = ColorSchema.parse(
        546, 427, 743, 501, "75F4D6", "39|0|75F4D6,79|0|75F4D6,118|0|74F4D6,157|0|74F4D6,0|37|20BE6F,39|37|20BE6F,79|37|20BE6F,118|37|20BE6F,157|37|1FBE6F", 0, 0.9, "中绿确认按钮"
    )

    // --- Migrated from the legacy freescript UI lookup table (函数275a) ---
    // The legacy script matches on the 720x1280 portrait framebuffer while this project
    // matches on 1280x720 landscape screenshots, so every region and offset point was
    // rotated by 90 degrees:  x' = y, y' = 719 - x,  offset (dx, dy) -> (dy, -dx).
    // Generic popup dialog. In the current game the dialog is a wood panel with a bright
    // gold top edge (fdf3a0) sitting on a brown body (935c20); the legacy light-blue colors
    // (CAFFFD/67afdd) no longer exist, so this is re-derived from the real screenshots.
    // Colors are BGR-ordered: fdf3a0 -> "a0f3fd", 935c20 -> "205c93".
    override val CommonDialog = ColorSchema.parse(
        150, 180, 1130, 360, "a0f3fd",
        "0|1|205c93,0|3|1c5487,-3|-2|a0f3fd,3|-2|a0f3fd,0|-1|9ef0fd",
        0, 0.85, "通用对话框"
    )
    // Return-to-camp button at the lower left（都城界面左下角"回营"：米色圆底 + 棕红"回营"字）
    override val BottomLeftReturnToCamp = ColorSchema.parse(
        0, 480, 220, 660, "8EC0F1-101010",
        "-12|0|8EC0F1-101010,6|0|85BCEF-101010,18|0|8EBFF1-101010,24|0|8DBEF0-101010,-12|-6|3C67B7-101010,12|-6|3967B6-101010,24|-6|3966B5-101010",
        0, 0.9, "左下角回营"
    )
    // Main village return-to-camp confirmation
    override val ReturnToCampMain = ColorSchema.parse(
        573, 63, 712, 163, "59c888-101010",
        "6|4|59c888-101010,10|-1|59c888-101010,-1|7|000000-101010,5|10|59c888-101010,2|13|59c888-101010,7|15|000000-101010",
        0, 0.9, "回营主"
    )
    // Fallback variant 2 (the legacy script tries variants in order)
    override val ReturnToCampMain2 = ColorSchema.parse(
        450, 277, 846, 594, "B92688-101010",
        "4|-6|D50BAB-101010,9|-3|D20BA8-101010,13|0|AC1778-101010,7|5|B90983-101010,8|10|00A1E4-101010,19|2|008ED9-101010,-7|1|00A5E7-101010",
        0, 0.9, "回营主"
    )
    // Fallback variant 3 (the legacy script tries variants in order)
    override val ReturnToCampMain3 = ColorSchema.parse(
        521, 537, 761, 686, "2CCD84",
        "-4|-30|82E4B7,-119|-43|84E4B9,-121|12|25C373,99|10|29C87C,96|-43|84E4B9,-12|11|27C577,-17|-44|84E4B9",
        0, 0.9, "回营主"
    )
    // Fallback variant 4 (the legacy script tries variants in order)
    override val ReturnToCampMain4 = ColorSchema.parse(
        511, 542, 771, 714, "84e4b9-101010",
        "-106|-4|84e4b9-101010,110|-5|84e4b9-101010,51|-2|84e4b9-101010,-51|-6|84e4b9-101010,-103|46|27c577-101010,-33|43|2ccc83-101010,33|42|2ccd84-101010,96|42|2ccd84-101010,104|59|030303-101010,34|59|030303-101010,-38|60|060606-101010,-86|61|060606-101010",
        0, 0.9, "回营主"
    )
    // Fallback variant 5 (the legacy script tries variants in order)
    override val ReturnToCampMain5 = ColorSchema.parse(
        537, 486, 732, 684, "1FBB6C-101010",
        "4|-44|8CFAE2-101010,-69|8|1FBC6F-101010,-66|-42|89F9E1-101010,68|6|1FBB6D-101010,68|-46|8FFBE4-101010,66|-23|66F0CC-101010,-55|-19|5FEBC2-101010,-81|93|000000-101010,-35|94|020202-101010,19|94|010101-101010,70|94|000000-101010",
        0, 0.9, "回营主"
    )
    // Builder base return-to-camp confirmation
    override val ReturnToCampBuilder = ColorSchema.parse(
        835, 357, 941, 560, "47D9FF-101010",
        "4|3|03B9F0-101010,2|-3|49E2FF-101010,7|-2|0DD0FF-101010,4|-6|4AF2FF-101010,9|-5|19E4FF-101010,7|-9|4CFFFF-101010,11|-8|2FFBFE-101010,16|-6|3CFCFF-101010,-4|-12|49F0FF-101010",
        0, 0.9, "回营夜"
    )
}
