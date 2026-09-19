@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

interface IClanCapitalTutorialColors {
    val CapitalOldMan: ColorSchema

    // Migrated from the legacy freescript UI lookup table (函数275a)
    val ClanCapitalEntry: ColorSchema
}

object ClanCapitalTutorialColors : IClanCapitalTutorialColors {
    override val CapitalOldMan = ColorSchema.parse(
        110, 397, 256, 692, "17406F", "29|0|887D7D,58|0|858289,87|0|8F8E95,116|0|323D61,0|147|7C7F83,29|147|9DA3AE,58|147|9FA6B1,87|147|A7B0BE,116|147|0F2749", 0, 0.9, "都城老头"
    )

    // --- Migrated from the legacy freescript UI lookup table (函数275a) ---
    // The legacy script matches on the 720x1280 portrait framebuffer while this project
    // matches on 1280x720 landscape screenshots, so every region and offset point was
    // rotated by 90 degrees:  x' = y, y' = 719 - x,  offset (dx, dy) -> (dy, -dx).
    // Clan capital entry (same feature as FeatureColors.BottomLeftReturnToCamp)
    override val ClanCapitalEntry = ColorSchema.parse(
        23, 541, 178, 693, "7F9DFF-101010",
        "5|0|7FA4FF-101010,27|-7|728AE8-101010,19|-32|2548B7-101010,5|-34|2849C1-101010,18|-41|3B58CC-101010,-17|-16|1F38AD-101010,14|14|7093D5-101010",
        0, 0.9, "都城界面"
    )
}
