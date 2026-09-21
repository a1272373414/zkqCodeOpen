package com.coc.zkqcode.jar.code.mainbase.attack

import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.mainbase.others.zoomSmallMainBase
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.deploy.DeployDebug
import com.coc.zkqcode.jar.code.universal.deploy.DeployGeometry
import com.coc.zkqcode.jar.code.universal.deploy.DeploySettings
import com.coc.zkqcode.jar.code.universal.deploy.DeploySide
import com.coc.zkqcode.jar.code.universal.deploy.DeployType
import com.coc.zkqcode.jar.code.universal.deploy.deploySiegeOrReinforcement
import com.coc.zkqcode.jar.code.universal.deploy.deployUntilGone
import com.coc.zkqcode.jar.code.universal.deploy.readDeploySettings
import com.coc.zkqcode.jar.code.universal.deploy.releaseSpells
import com.coc.zkqcode.jar.code.universal.deploy.resetDeployState
import com.coc.zkqcode.jar.code.universal.deploy.swipeDeploymentBar

/** Outer deploy passes; each pass re-reads the bar, so 活动兵 joining mid-battle are picked up. */
private const val DEPLOY_ROUNDS = 6

/** Deployment-bar pages to walk after the current one is empty (源 `兵种显示` 有 左/中/右 三态). */
private const val MAX_BAR_PAGES = 3

/**
 * Main-world troop deployment, following the legacy source project's model
 * (`放兵` / `函数142a` / `函数327a`) with the source's own coordinates converted by
 * [DeployGeometry] instead of a hand-calibrated ring.
 *
 * Behaviour:
 *  - The four legacy 下兵方式 modes (四面 / 单面仿滑屏 / 单面真滑屏 / 单面中间单点) and 援兵位置 are
 *    read from the schema config (`deploy_mode` / `deploy_side`), mirroring the source's
 *    `活鱼下兵方式` / `援兵位置`.
 *  - Units are placed on the quadrant deploy LINES: 函数327a walks each line from 开始 to 结束 in
 *    `兵数` even steps and shifts every tap by the per-type offset.
 *  - Each troop family is deployed until it disappears from the deployment bar. The legacy code
 *    knows an exact count from its army config; this project does not, so [deployUntilGone] cycles
 *    the quadrants until the bar no longer shows the unit.
 *  - Spells follow the mode: 四面 dumps them all in one round (源 `放兵`), single-side releases them
 *    on the source's timeline ([releaseSpells]).
 *  - Heroes are still tapped in the bar and dropped at the quadrant middle.
 */
suspend fun mainBaseDeployTroops() {
    // Record the start time of the battle
    zoomSmallMainBase(isForAttack = true)
    // Per-battle deploy state (图腾 wave / 法术 timeline) starts from scratch.
    resetDeployState()
    val settings = readDeploySettings()
    DeployDebug.log("开始下兵：方式=${settings.mode.label} 援兵=${settings.reinforcementSide.label} 速度=${settings.safeSpeed}")
    // Stand-in for the source's `放完兵时间`: the moment this battle's deployment starts.
    val deployStartMs = System.currentTimeMillis()

    repeat(DEPLOY_ROUNDS) {
        // Dismiss the event reward popup if present (destruction milestones can trigger it while deploying)
        if (handleRewardPopup()) delayWithMultiplier(800)
        deployCurrentBarPage(settings)
        // 法术: 四面 一轮内全部放完；单面 按源 间隔 时间轴释放
        releaseSpells(settings, System.currentTimeMillis() - deployStartMs) {
            findInBar(MyColors.SpellColorAtDeploymentBar, MyColors.ReviveSpellAtDeploymentBar)
        }
        // 当前页的兵都放完后左滑翻页，把后面页面的兵也放掉（源 函数135a/136a）
        deployRemainingBarPages(settings)
    }
}

/** Runs deploy passes in order; true when at least one of them found something in the bar. */
private suspend fun anyDeployed(vararg passes: suspend () -> Boolean): Boolean {
    var found = false
    for (pass in passes) {
        if (pass()) found = true
    }
    return found
}

/** Everything this project can recognize on the CURRENT deployment-bar page. */
private suspend fun deployCurrentBarPage(settings: DeploySettings): Boolean = anyDeployed(
    { deploySpecificTroops(settings) },
    { deployHeroes(settings) },
    { deployGenericTroops(settings) },
    { deploySiegeAndReinforcement(settings) }
)

/**
 * 源 `函数135a` / `函数136a` 的部署栏翻页：当前页的兵放完后左滑翻到下一页继续放；翻到没有可放的
 * 兵就右滑翻回第一页（下一轮从第一页重新开始）。
 */
private suspend fun deployRemainingBarPages(settings: DeploySettings) {
    var pages = 0
    while (pages < MAX_BAR_PAGES) {
        pages++
        swipeDeploymentBar(forward = true)
        if (!deployCurrentBarPage(settings)) break
    }
    repeat(pages) { swipeDeploymentBar(forward = false) }
    if (pages > 1) DeployDebug.log("部署栏翻页 $pages 页完成，已滑回第一页")
}

/**
 * 源 `函数156a`: 攻城机器（源 `器列表` 9 种 → `找机器`）或部落城堡援兵（`找援兵`），
 * 落在 `援兵位置` 所在象限的 `中间`。
 */
private suspend fun deploySiegeAndReinforcement(settings: DeploySettings): Boolean =
    deploySiegeOrReinforcement(
        settings = settings,
        side = settings.reinforcementSide,
        findInBar = {
            findInBar(
                MyColors.SiegeChariot, MyColors.SiegeChariot2,
                MyColors.SiegeAirship, MyColors.SiegeAirship2,
                MyColors.SiegeWarBall, MyColors.SiegeWarBall2,
                MyColors.SiegeBarracks, MyColors.SiegeBarracks2,
                MyColors.SiegeLogLauncher, MyColors.SiegeLogLauncher2,
                MyColors.SiegeFlameThrower, MyColors.SiegeFlameThrower2,
                MyColors.SiegeDrill, MyColors.SiegeDrill2,
                MyColors.SiegeTroopLauncher, MyColors.SiegeTroopLauncher2,
                MyColors.SiegeSkyChariot, MyColors.SiegeSkyChariot2,
                MyColors.ClanCastleTroop, MyColors.ClanCastleTroop2, MyColors.ClanCastleTroop3,
                MyColors.ClanCastleTroop4, MyColors.ClanCastleTroop5
            )
        }
    )

/**
 * Deploys a troop family. If any of its color variants is in the bar, tap across the quadrants
 * (函数327a) until the whole family is gone.
 */
private suspend fun deployFamily(
    settings: DeploySettings,
    type: DeployType,
    vararg schemas: ColorSchema
): Boolean {
    val hit = findInBar(*schemas) ?: return false
    DeployDebug.log("发现 ${type.label} 家族：${schemas.joinToString("/") { it.name ?: "未命名" }} @(${hit.x},${hit.y})")
    deployUntilGone(type, settings, findInBar = { findInBar(*schemas) })
    return true
}

private suspend fun deploySpecificTroops(settings: DeploySettings): Boolean = anyDeployed(
    {
        deployFamily(
            settings, DeployType.TROOP,
            MyColors.DragonAtDeploymentBar, MyColors.DragonAtDeploymentBar2, MyColors.DragonAtDeploymentBar3
        )
    },
    { deployFamily(settings, DeployType.TROOP, MyColors.GiantAtDeploymentBar, MyColors.GiantAtDeploymentBar2) },
    { deployFamily(settings, DeployType.TROOP, MyColors.BarbarianAtDeploymentBar, MyColors.BarbarianAtDeploymentBar2) },
    {
        deployFamily(
            settings, DeployType.TROOP,
            MyColors.ArcherAtDeploymentBar, MyColors.ArcherAtDeploymentBar2,
            MyColors.ArcherAtDeploymentBar3, MyColors.ArcherAtDeploymentBar4
        )
    },
    { deployFamily(settings, DeployType.TROOP, MyColors.PrinceAtDeploymentBar) },
    { deployFamily(settings, DeployType.TROOP, MyColors.DragonRiderAtDeploymentBar) },
    // 图腾 has its own deploy type: the source `函数327a` pulls it inward by `a` instead of 0, so
    // it must NOT go through DeployType.TROOP (which lands right on the deploy ring).
    { deployFamily(settings, DeployType.TOTEM, MyColors.TotemAtDeploymentBar) }
)

private suspend fun deployGenericTroops(settings: DeploySettings): Boolean {
    // Remaining (unnamed) troops, including the 活动兵 that can join mid-battle.
    return deployFamily(
        settings, DeployType.TROOP,
        MyColors.TroopColorAtDeploymentBar, MyColors.SuperTroopColorAtDeploymentBar, MyColors.SpecialTroopColorAtDeploymentBar
    )
}

private suspend fun deployHeroes(settings: DeploySettings): Boolean {
    var heroFound = false
    // One entry per HERO (not per schema): the primary schema first, then optional fallback variants.
    // Grouping avoids treating two schemas of the SAME hero as two separate heroes — previously the
    // legacy fallback variants kept matching an already-deployed hero and wasted 4 retries each.
    val heroGroups = listOf(
        listOf(MyColors.KingBarbarian),
        listOf(MyColors.QueenArcher, MyColors.QueenArcher2, MyColors.QueenArcher3, MyColors.QueenArcherLegacy),
        listOf(MyColors.MinionPrince, MyColors.MinionPrince2, MyColors.MinionPrinceLegacy),
        listOf(MyColors.GrandWarden, MyColors.GrandWarden2, MyColors.GrandWarden3, MyColors.GrandWarden4, MyColors.GrandWardenLegacy),
        listOf(MyColors.RoyalChampion, MyColors.RoyalChampion2),
        listOf(MyColors.DragonDuke)
    )
    // Hero drop candidates: the four quadrant middles (源 `taps(象限中间)`), tried in order so that
    // when a hero cannot be placed at one spot the retry uses a DIFFERENT one (heroes fail to be
    // placed more often than troops).
    val drops = listOf(
        DeploySide.TOP_LEFT, DeploySide.TOP_RIGHT, DeploySide.BOTTOM_LEFT, DeploySide.BOTTOM_RIGHT
    ).map { DeployGeometry.middleTap(it, DeployType.TROOP) }
    for (group in heroGroups) {
        var deployed = false
        for (hero in group) {
            // Stop once this hero has been placed, so we do not re-try its fallback variants.
            if (deployed) break
            var hit: Point? = findMultiColors(schema = hero) ?: continue
            var tries = 0
            while (hit != null && tries < 2) {
                tries++
                DeployDebug.log("英雄 ${hero.name ?: "未命名"} @(${hit.x},${hit.y}) 第${tries}轮（选中后依次试 ${drops.size} 个落点）")
                // Legacy 函数162a/163a: tap the hero's bar card (it gets a white selection border),
                // then tap a deploy point. The game does NOT support drag-and-drop, and a single
                // placement tap can land on a spot the hero cannot be placed at, so try every
                // candidate point after ONE selection — the first valid one places the hero.
                TouchActions.tap(hit.x, hit.y, delayTime = 400)
                delayWithMultiplier(600)
                for (drop in drops) {
                    TouchActions.tap(drop.x, drop.y, delayTime = 400)
                }
                hit = findMultiColors(schema = hero)
            }
            DeployDebug.log("英雄 ${hero.name ?: "未命名"} 结束，仍在栏中=${hit != null}")
            if (hit == null) {
                deployed = true
                heroFound = true
            }
        }
    }
    return heroFound
}

/**
 * Returns the bar position of the first schema visible in the deployment bar, or null. Captures
 * the screen once so the whole family is checked against a single frame.
 */
private suspend fun findInBar(vararg schemas: ColorSchema): Point? {
    val screen = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult ?: return null
    for (schema in schemas) {
        val point = findMultiColors(schema = schema, byteBuffer = screen)
        if (point != null) return point
    }
    return null
}
