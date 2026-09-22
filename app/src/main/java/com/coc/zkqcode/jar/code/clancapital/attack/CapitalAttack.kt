package com.coc.zkqcode.jar.code.clancapital.attack

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

/**
 * Clan-capital battle deploy, ported from the legacy script `函数31a`.
 *
 * Legacy flow per battle:
 *   1. Confirm a battle is running (the give-up button is on screen).
 *   2. Collect "deploy points" by matching the grass/terrain signatures (颜色字符1a).
 *   3. For every troop card in the deploy bar: select it, then keep tapping the deploy points
 *      until the card disappears (that troop is fully deployed).
 *   4. For every spell: select it, then keep tapping the first low-HP enemy marker (残血)
 *      until the spell card disappears.
 *
 * NOTE: the deploy-bar cards are recognised by their battle-time colour features migrated into
 * [MyColors] (see colorpackage/clancapital/CapitalDeployColors.kt). Those features still need an
 * on-device pass in a real capital battle — no capital-battle screenshot exists offline.
 */

/** Troops to deploy, in the legacy script's order. */
private val DEPLOY_TROOPS: List<ColorSchema> = listOf(
    MyColors.CapitalDeploySuperMiner,
    MyColors.CapitalDeployBattleRam,
    MyColors.CapitalDeploySuperGiant,
    MyColors.CapitalDeploySneakyArcher,
    MyColors.CapitalDeploySuperBarbarian,
    MyColors.CapitalDeployHogRaider,
    MyColors.CapitalDeploySuperWizard,
)

/** Spells (with their per-spell round bound; legacy: 疗伤 40, the rest 25). */
private val DEPLOY_SPELLS: List<Pair<ColorSchema, Int>> = listOf(
    MyColors.CapitalDeployHealingSpell to 40,
    MyColors.CapitalDeployJumpSpell to 25,
    MyColors.CapitalDeploySkeletonSpell to 25,
    MyColors.CapitalDeployLightningSpell to 25,
    MyColors.CapitalDeployFreezeSpell to 25,
)

/** Legacy troop deploy loop is bounded to 20 rounds. */
private const val TROOP_ROUNDS = 20
private const val MAX_DEPLOY_POINTS = 10

/** Up to [MAX_DEPLOY_POINTS] distinct deploy points found from the terrain (grass) signatures. */
private suspend fun collectDeployPoints(): List<Point> {
    val points = ArrayList<Point>()
    for (schema in MyColors.CapitalDeployTerrain) {
        val p = findMultiColors(schema) ?: continue
        if (points.none { it.x == p.x && it.y == p.y }) points.add(p)
        if (points.size >= MAX_DEPLOY_POINTS) break
    }
    return points
}

/** Select the troop card, then tap the deploy points until the card disappears. */
private suspend fun deployTroop(points: List<Point>, schema: ColorSchema): Boolean {
    val card = findMultiColors(schema) ?: return false
    TouchActions.tap(card.x, card.y, delayTime = 150)
    repeat(TROOP_ROUNDS) {
        for (p in points) {
            TouchActions.tap(p.x, p.y, delayTime = 50)
            if (findMultiColors(schema) == null) return true
        }
    }
    return findMultiColors(schema) == null
}

/** Select the spell card, then tap the first low-HP enemy marker until the card disappears. */
private suspend fun deploySpell(schema: ColorSchema, rounds: Int): Boolean {
    val card = findMultiColors(schema) ?: return false
    TouchActions.tap(card.x, card.y, delayTime = 150)
    repeat(rounds) {
        for (hp in MyColors.CapitalLowHpTargets) {
            val target = findMultiColors(hp) ?: continue
            TouchActions.tap(target.x, target.y, delayTime = 800)
            break
        }
        delayWithMultiplier(50)
        if (findMultiColors(schema) == null) return true
    }
    return findMultiColors(schema) == null
}

/**
 * Deploy the whole capital army in the current battle (troops first, then spells).
 *
 * @return true when at least one troop/spell was deployed.
 */
suspend fun capitalDeployArmy(): Boolean {
    if (findMultiColors(MyColors.GiveUpButton) == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未检测到放弃按钮，可能不在战斗中")
        return false
    }
    val points = collectDeployPoints()
    if (points.isEmpty()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到下兵点")
        return false
    }
    var acted = false
    for (schema in DEPLOY_TROOPS) if (deployTroop(points, schema)) acted = true
    for ((schema, rounds) in DEPLOY_SPELLS) if (deploySpell(schema, rounds)) acted = true
    return acted
}
