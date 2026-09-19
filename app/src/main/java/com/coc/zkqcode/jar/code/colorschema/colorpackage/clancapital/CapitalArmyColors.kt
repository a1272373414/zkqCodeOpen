@file:Suppress("PropertyName")

package com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * UI elements of the clan capital army screen (independent from main village / builder base).
 *
 * Migrated from the legacy freescript and rotated to this project's 1280x720 landscape space
 * (x' = y, y' = 719 - x, offset (dx, dy) -> (dy, -dx)), then verified against real screenshots.
 */
interface ICapitalArmyColors {
    /** Green "train / confirm" button of the army screen. */
    val CapitalTrainConfirm: ColorSchema
}

object CapitalArmyColors : ICapitalArmyColors {
    // Legacy: detection area (368,747)-(460,938), main "7DF2D5", click (408,848).
    // Rotated: area (747,259)-(938,351), click (844,301). Verified on 都城-配兵01..07:
    // the button bbox is (761,269)-(928,334) and its centre is (844,301) on every shot.
    override val CapitalTrainConfirm = ColorSchema.parse(
        747, 259, 938, 351, "7DF2D5",
        "0|46|31B46B,-68|-1|7FF3D7,-70|43|30BA71,65|-5|84F5DC,65|41|2FBE75",
        0, 0.9, "都城训练确认"
    )
}
