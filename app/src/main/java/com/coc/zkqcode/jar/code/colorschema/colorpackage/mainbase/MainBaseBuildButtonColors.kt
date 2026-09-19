@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * Build/upgrade button markers for the main village.
 *
 * Previously these were hardcoded inline inside MainBaseFindBuildingButton.kt; they live here
 * now so that every color definition of the project stays inside the colorschema package.
 */
interface IMainBaseBuildButtonColors {
    val MainBaseBuildTick1: ColorSchema
    val MainBaseBuildTick2: ColorSchema
    val MainBaseBuildCross1: ColorSchema
    val MainBaseBuildCross2: ColorSchema
}

object MainBaseBuildButtonColors : IMainBaseBuildButtonColors {
    // Green tick shown on a buildable / upgradeable building - variant 1
    override val MainBaseBuildTick1 = ColorSchema.parse(
        105, 60, 1115, 680, "FFFFFF",
        "-2|-8|58FFE8,-5|-8|58FFE8,-3|-7|54FFE7,-1|-7|54FFE7,-6|4|14B745,-5|5|14B948,2|6|15BF51,4|6|15BF51,6|4|14B745",
        0, 0.95, "主世界绿色勾勾1"
    )
    // Green tick shown on a buildable / upgradeable building - variant 2
    override val MainBaseBuildTick2 = ColorSchema.parse(
        105, 60, 1115, 680, "EBEFF5",
        "-3|-8|4EECDB,-1|-9|4FEBDC,-2|-8|4EECDB,-7|2|15B448,-5|4|13AD43,-3|7|15B956,1|7|15B956,4|6|13B249,6|5|13AE44",
        0, 0.93, "主世界绿色勾勾2"
    )
    // Red cross shown when the resource cost cannot be paid - variant 1
    override val MainBaseBuildCross1 = ColorSchema.parse(
        105, 60, 1115, 680, "FFFFFF",
        "-6|-1|8884F8,-6|-4|8A84FC,-3|-6|8B84FF,6|-6|8B84FF,7|5|0F0DC7,5|8|0E0DCA,0|8|0E0DCA,-4|8|0E0DCA,-6|7|0E0DC6",
        0, 0.92, "主世界红色叉叉1"
    )
    // Red cross shown when the resource cost cannot be paid - variant 2
    override val MainBaseBuildCross2 = ColorSchema.parse(
        105, 60, 1115, 680, "FFFFFF",
        "-6|0|8784F5,-7|-7|8C84FF,0|-7|8B84FF,8|-7|877EFF,7|-2|8884F9,7|4|0F0DCB,5|8|0E0DC9,-2|8|0E0DC9,-7|7|0E0DC6",
        0, 0.92, "主世界红色叉叉2"
    )
}
