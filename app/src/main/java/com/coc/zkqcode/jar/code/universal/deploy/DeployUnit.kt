package com.coc.zkqcode.jar.code.universal.deploy

import com.coc.zkqcode.jar.code.colorschema.ColorSchema

/**
 * One deployable unit (a troop, hero, spell or siege machine) for the shared deploy pipeline.
 *
 * @param name   Chinese display name used only for logging/toast (legacy `兵种`).
 * @param type   Deploy category, decides the inward offset applied to the drop point.
 * @param count  How many to place; `0` means "unknown" (use until-gone / sweep).
 * @param schema Optional color schema used to detect the unit in the deployment bar.
 *               `null` means the unit is always deployed (no presence check).
 */
data class DeployUnit(
    val name: String,
    val type: DeployType = DeployType.TROOP,
    val count: Int = 0,
    val schema: ColorSchema? = null
)
