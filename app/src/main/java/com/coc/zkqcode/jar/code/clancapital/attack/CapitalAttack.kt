package com.coc.zkqcode.jar.code.clancapital.attack

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.clancapital.attack.CapitalTarget
import com.coc.zkqcode.jar.code.clancapital.saveCapitalScreenshot
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

/**
 * Clan-capital battle deploy, ported from the legacy script `函数31a`.
 *
 * Legacy flow per battle:
 *   1. Confirm a battle is running (the give-up button is on screen).
 *   2. Collect "deploy points" by matching the grass/terrain signatures (颜色js1a),
 *      scanning three regions with fallback (区域1 → 区域2 → 区域3)，源 函数31a.
 *   3. For every troop card in the deploy bar: select it, then keep tapping the deploy points
 *      until the card disappears (that troop is fully deployed).
 *   4. For every spell: select it, then tap the matching marker until the spell card disappears.
 *      - 骷髅法术 uses the low-HP enemy markers (残血)
 *      - 疗伤/弹跳/雷电 use the "sneak spell" markers (偷袭法术下兵点)
 *      - 冰冻 uses the "sneak freeze spell" markers (偷袭冰冻法术下兵点)，回退到偷袭法术下兵点
 *
 * Second round (源 主流程 函数39a): when the first scan yields < 2 points, swipe the view to
 * reveal more ground and re-scan, then also add the target building coordinate located by the
 * chosen sub-district (the "9-map" target-building locator) as a deploy point.
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

/** 法术投放方案（源 函数31a）：主投放点 primary，缺失时回退 fallback，再缺失用固定兜底点。 */
private data class SpellPlan(
    val schema: ColorSchema,
    val rounds: Int,
    val primary: List<ColorSchema>,
    val fallback: List<ColorSchema>? = null
)

/**
 * 法术投放点（源 函数31a）：
 * - 骷髅法术 → 残血目标（CapitalLowHpTargets）
 * - 疗伤/弹跳/雷电 → 偷袭法术下兵点（CapitalSneakSpellPoints）
 * - 冰冻 → 偷袭冰冻法术下兵点（CapitalSneakFreezePoints），回退到偷袭法术下兵点
 */
private val DEPLOY_SPELLS: List<SpellPlan> = listOf(
    SpellPlan(MyColors.CapitalDeployHealingSpell, 40, MyColors.CapitalSneakSpellPoints),
    SpellPlan(MyColors.CapitalDeployJumpSpell, 25, MyColors.CapitalSneakSpellPoints),
    SpellPlan(MyColors.CapitalDeploySkeletonSpell, 25, MyColors.CapitalLowHpTargets),
    SpellPlan(MyColors.CapitalDeployLightningSpell, 25, MyColors.CapitalSneakSpellPoints),
    SpellPlan(MyColors.CapitalDeployFreezeSpell, 25, MyColors.CapitalSneakFreezePoints, MyColors.CapitalSneakSpellPoints),
)

/** 源法术投放固定兜底点（竖屏 360,640 / 660,200 等映射到横屏），特征缺失时使用。 */
private val SPELL_FALLBACK_POINTS = listOf(
    Point(640, 359),  // 竖屏(360,640)
    Point(200, 59),   // 竖屏(660,200)
    Point(500, 400),
)

/** Legacy troop deploy loop is bounded to 20 rounds. */
private const val TROOP_ROUNDS = 20
private const val MAX_DEPLOY_POINTS = 10

/**
 * 收集下兵点：源 函数31a 三区域逐级回退扫描（区域1 → 区域2 → 区域3）。
 * 区域1 找到 >= 2 个点即停止；不足则扫区域2（累加），仍不足扫区域3（累加）；最多 10 个。
 */
private suspend fun collectDeployPoints(): List<Point> {
    val points = ArrayList<Point>()
    for (zone in listOf(
        MyColors.CapitalDeployTerrainZone1,
        MyColors.CapitalDeployTerrainZone2,
        MyColors.CapitalDeployTerrain
    )) {
        for (schema in zone) {
            val p = findMultiColors(schema) ?: continue
            if (points.none { it.x == p.x && it.y == p.y }) points.add(p)
            if (points.size >= MAX_DEPLOY_POINTS) return points
        }
        if (points.size >= 2) return points
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

/** Select the spell card, then tap the matching marker (per legacy spell type) until it disappears. */
private suspend fun deploySpell(plan: SpellPlan): Boolean {
    val card = findMultiColors(plan.schema) ?: return false
    TouchActions.tap(card.x, card.y, delayTime = 150)
    repeat(plan.rounds) {
        var tapped = false
        // 主投放点（源：骷髅→残血，其余→偷袭法术下兵点）
        for (hp in plan.primary) {
            val target = findMultiColors(hp) ?: continue
            TouchActions.tap(target.x, target.y, delayTime = 800)
            tapped = true
            break
        }
        // 主投放点缺失时回退（源：冰冻→偷袭冰冻法术下兵点）
        if (!tapped) {
            plan.fallback?.let { fb ->
                for (hp in fb) {
                    val target = findMultiColors(hp) ?: continue
                    TouchActions.tap(target.x, target.y, delayTime = 800)
                    tapped = true
                    break
                }
            }
        }
        // 仍缺失则用固定兜底点（源 疗伤/雷电/弹跳补点 360,640 等映射到横屏）
        if (!tapped) {
            for (p in SPELL_FALLBACK_POINTS) {
                TouchActions.tap(p.x, p.y, delayTime = 800)
                tapped = true
                break
            }
        }
        delayWithMultiplier(50)
        if (findMultiColors(plan.schema) == null) return true
    }
    return findMultiColors(plan.schema) == null
}

/** 滑动视野（源 主流程第二轮 swipes(1,160,976,710,158,300) 的横屏映射），露出更多可下兵区域。 */
private suspend fun swipeCapitalView() {
    // 竖屏(160,976)→(710,158) 旋转 90° → 横屏(976,559)→(158,9)；实际为避免触及顶部边缘(y=9)误触通知栏，
    // 终点取 (300,250) 的安全位置，滑动幅度已足以改变视野、露出更多可下兵区域
    TouchActions.swipe(976, 559, 300, 250, delayTime = 400)
    delayWithMultiplier(400)
}

/**
 * Deploy the whole capital army in the current battle (troops first, then spells).
 *
 * @param target 本次选择的子城目标（含横屏坐标 x,y），用于第二轮「9 地图定位目标建筑」补点。
 * @return true when at least one troop/spell was deployed.
 */
suspend fun capitalDeployArmy(target: CapitalTarget? = null): Boolean {
    if (findMultiColors(MyColors.GiveUpButton) == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未检测到放弃按钮，可能不在战斗中")
        return false
    }
    // 关键节点截图存档（测试期宝贵，每号每周仅 5 次机会，务必留存）
    saveCapitalScreenshot("capital_battle_start")

    // 第一轮：三区域逐级回退扫描下兵点
    var points = collectDeployPoints()
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第一轮找到下兵点 ${points.size} 个")

    // 第二轮（源 函数31a/主流程）：第一轮点不足时滑动视野重扫，并用 9 地图定位目标建筑坐标
    if (points.size < 2) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第一轮下兵点不足，进入第二轮（滑动视野重扫 + 9地图目标建筑）")
        swipeCapitalView()
        // 第二轮扫描结果为可变列表，便于按 9 地图定位追加目标建筑放兵点
        val second = collectDeployPoints().toMutableList()
        // 9 地图定位：把本次选择的子城目标建筑坐标（横屏 x,y）加入放兵点
        target?.let { t ->
            if (second.none { it.x == t.x && it.y == t.y }) second.add(Point(t.x, t.y))
        }
        if (second.isNotEmpty()) {
            points = second
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第二轮找到下兵点 ${points.size} 个")
        } else {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第二轮仍无下兵点")
        }
    }

    if (points.isEmpty()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：始终未找到下兵点，结束本场进攻")
        return false
    }

    var acted = false
    for (schema in DEPLOY_TROOPS) if (deployTroop(points, schema)) {
        acted = true
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：兵种已下场 ${schema.name}")
    }
    for (plan in DEPLOY_SPELLS) if (deploySpell(plan)) {
        acted = true
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：法术已投放 ${plan.schema.name}")
    }
    saveCapitalScreenshot("capital_battle_deployed")
    return acted
}
