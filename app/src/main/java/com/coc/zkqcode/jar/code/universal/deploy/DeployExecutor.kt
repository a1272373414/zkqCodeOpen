package com.coc.zkqcode.jar.code.universal.deploy

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Deploy executor: a faithful port of the legacy source project's `放兵` / `函数327a` /
 * `函数142a` / `函数151a` / `函数147a..函数150a` (see `docs/下兵逻辑优化方案20260921.md` and
 * [DeployGeometry] for the coordinate conversion).
 *
 * Source recap:
 *  - `放兵(上下)`: for every troop, `taps(兵x, 兵y)` selects its card in the deployment bar and
 *    then `函数142a(上下, 兵种, 数量, ...)` places it.
 *  - `函数142a`: 四面 / 单面仿滑屏 -> `函数327a`; 单面真滑屏 with 数量 > 14 -> a real two-finger
 *    swipe; 单面真滑屏 with 数量 <= 14 -> `函数327a`; 单面中间单点 -> nothing (the caller uses
 *    `单面中间单点下兵` instead).
 *  - `函数327a`: per-type offsets + wave (see [DeployGeometry]), then the quadrant taps.
 *  - `放兵速度` is the tap spacing (default 25, lower bound 15).
 *
 * The one thing this project cannot copy is `数量`: the source reads it from its army config, we
 * have none, so [deployUntilGone] keeps cycling the quadrants with [DeploySettings.count] units
 * per round until the card disappears from the deployment bar. All the *placement* logic (line
 * splitting, per-type offsets, fan-out, swipe) is the source's own.
 */

/** Legacy switches to a real two-finger swipe only when a unit count exceeds 14. */
private const val REAL_SWIPE_THRESHOLD = 14

/** Safety cap on the rounds used by [deployUntilGone] while the card keeps being visible. */
private const val MAX_ROUNDS = 12

/** `rnd(2, 3)` of the legacy "extra taps" loops, inclusive. */
private fun randomExtraTaps(): Int = Random.nextInt(2, 4)

/** Max spells dumped in a single round in 四面 mode (the source casts its spells in one pass). */
private const val MAX_SPELLS_PER_ROUND = 8

/** Gap between two consecutive spell casts. */
private const val SPELL_CAST_GAP_MS = 800

/**
 * 单面法术时间轴, from the source's timed spell waves (all relative to `放完兵时间`):
 * 急速 `a55_/a56_/a57_` = 0/8/16 s, 狂暴 `a52_/a53_/a54_` = 2/9/18 s, 疗伤 `a58_/a59_` = 6/17 s,
 * 图腾 `图腾波数` = 0/8/15/22/28 s.
 *
 * This project detects "a spell" through one generic schema instead of a per-spell one, so a single
 * union table is used for the single-side modes.
 */
private val SPELL_TIMELINE_MS = intArrayOf(0, 8000, 15000, 22000, 28000)

/**
 * 图腾 波次 counter, mirroring the source's `图腾波数` (5 waves at 0/8/15/22/28 s in the source).
 *
 * It is battle-scoped rather than round-scoped because the deployment bar usually only shows the
 * totem card for ONE round at a time: [deployUntilGone] then returns with the card "gone" and the
 * next `deploySpecificTroops` pass re-discovers it at a different bar position. Counting rounds
 * inside a single [deployUntilGone] call therefore never gets past 波1.
 */
private var totemWave = 0

/** Spell targets rotate over the quadrants so consecutive casts do not stack up. */
private var spellSideIndex = 0

/** Index of the next [SPELL_TIMELINE_MS] slot to fire (single-side modes). */
private var spellWave = 0

/** 攻城机器 / 部落城堡援兵每局只放一次 (源 `已放援兵`). */
private var siegeDeployed = false

/** Resets the per-battle deploy state (图腾 wave, 法术 timeline, 攻城器); call once per battle. */
fun resetDeployState() {
    totemWave = 0
    spellWave = 0
    spellSideIndex = 0
    siegeDeployed = false
}

/** Next 图腾 wave (1-based; the inward pull saturates after the source's 5th wave). */
private fun nextTotemWave(): Int {
    totemWave++
    return totemWave
}

/** How many [SPELL_TIMELINE_MS] slots are due at [elapsedMs], consuming them. */
private fun dueSpellSlots(elapsedMs: Long): Int {
    var due = 0
    while (spellWave < SPELL_TIMELINE_MS.size && elapsedMs >= SPELL_TIMELINE_MS[spellWave]) {
        spellWave++
        due++
    }
    return due
}

/** The quadrants of one 面 ("上" = true / "下" = false). */
private fun pairFor(top: Boolean): List<DeploySide> =
    if (top) DeployGeometry.topSides else DeployGeometry.bottomSides

/** Legacy tap spacing: `sleep(放兵速度 + math.random(0, 5))`. */
private fun speedDelay(settings: DeploySettings): Int = settings.safeSpeed + Random.nextInt(0, 6)

/** The source's caller only drops on the 面 that owns `援兵位置` unless 下兵方式 is 四面. */
private fun usesTopPair(settings: DeploySettings): Boolean =
    settings.mode == DeployMode.FOUR_SIDES || settings.reinforcementSide.isTop

private fun usesBottomPair(settings: DeploySettings): Boolean =
    settings.mode == DeployMode.FOUR_SIDES || !settings.reinforcementSide.isTop

/** 函数147a/148a/149a/150a: one step of the quadrant line walk. */
private suspend fun tapSpread(
    side: DeploySide,
    index: Int,
    count: Int,
    type: DeployType,
    wave: Int,
    settings: DeploySettings
) {
    val p = DeployGeometry.spreadTap(side, index, count, type, wave)
    DeployDebug.log("铺线 ${side.label} ${type.label} 第${index + 1}/$count 步 @(${p.x},${p.y}) 速度=${settings.safeSpeed}")
    TouchActions.tap(p.x, p.y, delayTime = speedDelay(settings))
}

/** `中间 + 偏移` tap (炸弹 / 地震 / trailing extra taps). */
private suspend fun tapMiddle(
    side: DeploySide,
    type: DeployType,
    wave: Int,
    settings: DeploySettings
) {
    val p = DeployGeometry.middleTap(side, type, wave)
    DeployDebug.log("中间点 ${side.label} ${type.label} @(${p.x},${p.y}) 速度=${settings.safeSpeed}")
    TouchActions.tap(p.x, p.y, delayTime = speedDelay(settings))
}

/** `中间 + 偏移` tap fanned out by [n] more source pixels (地震's 80 / 160 rows). */
private suspend fun tapMiddleFan(
    side: DeploySide,
    type: DeployType,
    n: Double,
    wave: Int,
    settings: DeploySettings
) {
    val p = DeployGeometry.middleTapAtOffset(side, type, n, wave)
    DeployDebug.log("地震扇出 ${side.label} +$n @(${p.x},${p.y}) 速度=${settings.safeSpeed}")
    TouchActions.tap(p.x, p.y, delayTime = speedDelay(settings))
}

/**
 * 函数327a: place `count` units of [type] on one 面.
 *
 * 四面 -> `兵数/4` per quadrant of the 面 (both quadrants run in the same loop, like the source's
 * `函数147a(); 函数148a()` pairs); single side -> all of `兵数` on `援兵位置` only.
 */
suspend fun deployFunction327a(
    top: Boolean,
    count: Int,
    type: DeployType,
    settings: DeploySettings,
    wave: Int = 0
) {
    val fourSides = settings.mode == DeployMode.FOUR_SIDES
    val pair = pairFor(top)
    val targets = if (fourSides) pair else listOf(settings.reinforcementSide)
    val perQuadrant = if (fourSides) (count / 4).coerceAtLeast(1) else count.coerceAtLeast(1)
    DeployDebug.log(
        "函数327a 面=${if (top) "上" else "下"} 类型=${type.label} 兵数=$count 每象限=$perQuadrant " +
            "象限=${targets.joinToString("/") { it.label }} 波=${wave.coerceAtLeast(1)}"
    )

    when (type) {
        // 源: 炸弹 —— 先 下兵个数/2 轮，停 1 秒，再 下兵个数/2 + 2 轮（都是 中间点）
        DeployType.BOMB -> if (perQuadrant <= REAL_SWIPE_THRESHOLD) {
            repeat(perQuadrant / 2) {
                targets.forEach { tapMiddle(it, type, wave, settings) }
            }
            delayWithMultiplier(1000)
            repeat(perQuadrant / 2 + 2) {
                targets.forEach { tapMiddle(it, type, wave, settings) }
            }
        } else {
            repeat(perQuadrant) { i ->
                targets.forEach { tapSpread(it, i, perQuadrant, type, wave, settings) }
            }
        }

        // 源: 地震 —— 四面时 <=14 用 中间点，否则铺线；单面时 >=5 扇形撒 3 排，否则中间点连点 4 次
        DeployType.EARTHQUAKE -> if (fourSides) {
            if (perQuadrant <= REAL_SWIPE_THRESHOLD) {
                repeat(perQuadrant) { targets.forEach { tapMiddle(it, type, wave, settings) } }
            } else {
                repeat(perQuadrant) { i ->
                    targets.forEach { tapSpread(it, i, perQuadrant, type, wave, settings) }
                }
            }
        } else {
            val side = settings.reinforcementSide
            if (perQuadrant >= 5) {
                repeat(4) { tapMiddle(side, type, wave, settings) }
                repeat(4) { tapMiddleFan(side, type, 80.0, wave, settings) }
                repeat(4) { tapMiddleFan(side, type, 160.0, wave, settings) }
            } else {
                repeat(4) { tapMiddle(side, type, wave, settings) }
            }
        }

        else -> repeat(perQuadrant) { i ->
            targets.forEach { tapSpread(it, i, perQuadrant, type, wave, settings) }
        }
    }

    // 源: 单面（且非 地震/图腾）末尾额外随机补 2~3 次 中间点
    if (!fourSides && type != DeployType.EARTHQUAKE && type != DeployType.TOTEM) {
        repeat(randomExtraTaps()) { targets.forEach { tapMiddle(it, type, wave, settings) } }
    }
    // 源: 「下」面的 四面 分支末尾无条件再随机补 2~3 轮（左右各一次）
    if (!top && fourSides) {
        repeat(randomExtraTaps()) { pair.forEach { tapMiddle(it, type, wave, settings) } }
    }
}

/**
 * 函数142a: dispatch for one 面.
 *
 * @param findInBar used by the real-swipe branch, which re-checks the deployment bar after every
 *                  stroke exactly like the source does with `findMultiColor`.
 */
suspend fun deployFunction142a(
    top: Boolean,
    type: DeployType,
    count: Int,
    settings: DeploySettings,
    wave: Int = 0,
    findInBar: (suspend () -> Point?)? = null
) {
    delayWithMultiplier(139)

    if (settings.mode == DeployMode.SINGLE_REAL_SWIPE && count > REAL_SWIPE_THRESHOLD) {
        val side = settings.reinforcementSide
        if (side in pairFor(top)) realSwipe(side, count, settings, findInBar)
        return
    }
    // 源 142a 在「单面中间单点」模式下什么都不做：那种模式由 单面中间单点下兵 负责
    if (settings.mode == DeployMode.SINGLE_CENTER_TAP) return

    deployFunction327a(top, count, type, settings, wave)
}

/**
 * 函数142a「单面真滑屏」branch: hold two fingers on the quadrant middle and sweep them between
 * 开始 and 结束, re-checking the bar after each stroke.
 */
private suspend fun realSwipe(
    side: DeploySide,
    count: Int,
    settings: DeploySettings,
    findInBar: (suspend () -> Point?)?
) {
    val s = DeployGeometry.start(side)
    val e = DeployGeometry.end(side)
    val m = DeployGeometry.middle(side)
    val hold = DeployGeometry.holdPoint(side)
    DeployDebug.log("真滑屏 ${side.label} x$count 开始(${s.x},${s.y}) 结束(${e.x},${e.y}) 中间(${m.x},${m.y})")

    TouchActions.touchDown(hold.x.toFloat(), hold.y.toFloat(), 1)
    TouchActions.touchDown(m.x.toFloat(), m.y.toFloat(), 2)
    try {
        delayWithMultiplier(432)
        repeat((count / 2).coerceAtLeast(1)) {
            val d = 300 + Random.nextInt(-5, 6)
            TouchActions.moveSmoothly(hold.x.toFloat(), hold.y.toFloat(), s.x.toFloat(), s.y.toFloat(), d, 1)
            TouchActions.moveSmoothly(m.x.toFloat(), m.y.toFloat(), e.x.toFloat(), e.y.toFloat(), d, 2)
            TouchActions.moveSmoothly(s.x.toFloat(), s.y.toFloat(), hold.x.toFloat(), hold.y.toFloat(), d, 1)
            TouchActions.moveSmoothly(e.x.toFloat(), e.y.toFloat(), m.x.toFloat(), m.y.toFloat(), d, 2)
            if (findInBar != null && findInBar() == null) return
        }
    } finally {
        withContext(NonCancellable) {
            TouchActions.touchUp(1)
            TouchActions.touchUp(2)
        }
        delayWithMultiplier(300)
    }
}

/**
 * 单面中间单点下兵(位置, 兵x, 兵y, 数量): the caller has already selected the unit in the bar
 * (`taps(兵x, 兵y)`), so this only taps `中间` `数量` times.
 */
suspend fun deployCenterTaps(
    side: DeploySide,
    count: Int,
    settings: DeploySettings,
    type: DeployType = DeployType.TROOP,
    wave: Int = 0
) {
    delayWithMultiplier(100)
    repeat(count.coerceAtLeast(1)) {
        val p = DeployGeometry.middleTap(side, type, wave)
        DeployDebug.log("单面中间单点 ${side.label} @(${p.x},${p.y})")
        TouchActions.tap(p.x, p.y, delayTime = 50)
    }
    delayWithMultiplier(100)
}

/**
 * Places one unit family until its card disappears from the deployment bar.
 *
 * Before every round the unit is selected by tapping its card at the position returned by
 * [findInBar] - the source does the same (`taps(兵x, 兵y)` in `放兵`), and without it a battlefield
 * tap does nothing.
 *
 * 图腾 waves are counted per BATTLE (see [totemWave]): every deployed batch advances the wave, so the
 * totems creep toward the battlefield centre like the source's `放图腾` (5 timed waves) does, even
 * though the deployment bar usually only shows the totem card for a single round at a time.
 *
 * @param findInBar returns the unit's position in the deployment bar, or null when absent.
 * @return true when the unit was fully deployed.
 */
suspend fun deployUntilGone(
    type: DeployType,
    settings: DeploySettings,
    findInBar: suspend () -> Point?,
    count: Int = settings.count,
    wave: Int = 0
): Boolean {
    var hit: Point? = findInBar() ?: return true

    var round = 0
    DeployDebug.log("开始放 ${type.label}（${settings.mode.label}/${settings.reinforcementSide.label} 每轮兵数=$count）")
    while (hit != null && round < MAX_ROUNDS) {
        round++
        // 源 `放图腾` 用波次推进（图腾波数 = 0/8/15/22/28 秒 共 5 波）。图腾的波次按整局累计：
        // 部署栏往往只在一轮内认得出图腾卡，下次再由 deploySpecificTroops 重新发现（栏位会移动），
        // 若只按单次调用的轮次算就永远停在波1。其它兵种与波次无关。
        val roundWave = if (type == DeployType.TOTEM) nextTotemWave() else wave + round
        // 源 放兵: 先在部署栏点中这个兵种，否则战场上的点击不生效
        TouchActions.tap(hit.x, hit.y, delayTime = 200)
        DeployDebug.log("选中 ${type.label} @(${hit.x},${hit.y}) 第${round}轮 波=$roundWave")

        if (settings.mode == DeployMode.SINGLE_CENTER_TAP) {
            deployCenterTaps(settings.reinforcementSide, count, settings, type, roundWave)
        } else {
            if (usesTopPair(settings)) {
                deployFunction142a(top = true, type, count, settings, roundWave, findInBar)
            }
            if (usesBottomPair(settings)) {
                // 源用 函数132a 在两批之间把部署栏切到另一面（这里只按放兵速度等一下）
                delayWithMultiplier(100)
                deployFunction142a(top = false, type, count, settings, roundWave, findInBar)
            }
        }

        hit = findInBar()
        DeployDebug.log("${type.label} 第${round}轮结束，仍在栏中=${hit != null}")
    }

    if (hit != null) {
        ShowMessage("下兵：${type.label} 达到最大轮次($MAX_ROUNDS)仍未放完")
        return false
    }
    DeployDebug.log("${type.label} 已放完（共${round}轮）")
    return true
}

/**
 * Casts a single [type] spell on [side]: selects the spell card in the bar, then taps
 * `中间 + 偏移` (the source's single-point spell placement).
 *
 * @return true when a spell was cast.
 */
suspend fun castSpell(
    side: DeploySide,
    type: DeployType,
    settings: DeploySettings,
    findInBar: suspend () -> Point?
): Boolean {
    val hit = findInBar() ?: return false
    TouchActions.tap(hit.x, hit.y, delayTime = 250)
    delayWithMultiplier(400)

    val p = DeployGeometry.middleTap(side, type)
    TouchActions.tap(p.x, p.y, delayTime = settings.safeSpeed + 300)
    DeployDebug.log("部署 ${type.label} x1 ${side.label} @(${p.x},${p.y})")
    return true
}

/**
 * 法术释放, mode-dependent exactly like the source:
 *
 * - 四面: EVERY spell is dumped within one round - the source's `放兵` casts its whole spell list
 *   (地震/弹跳/狂暴/镜像/蔓生/骷髅/蝙蝠) in a single pass;
 * - single-side modes: spells follow [SPELL_TIMELINE_MS], the way the source's `已放第N波急速/
 *   狂暴/疗伤` and `图腾波数` waves do after `放完兵时间`.
 *
 * Casts rotate over the four quadrants ([spellSideIndex]) so consecutive spells do not stack.
 *
 * @param elapsedMs ms since the army started deploying; stands in for the source's `放完兵时间`.
 * @return how many spells were cast.
 */
suspend fun releaseSpells(
    settings: DeploySettings,
    elapsedMs: Long,
    findInBar: suspend () -> Point?
): Int {
    val budget = if (settings.mode == DeployMode.FOUR_SIDES) {
        MAX_SPELLS_PER_ROUND
    } else {
        dueSpellSlots(elapsedMs)
    }

    val sides = DeployGeometry.topSides + DeployGeometry.bottomSides
    var cast = 0
    repeat(budget) {
        val side = sides[spellSideIndex % sides.size]
        if (!castSpell(side, DeployType.SPELL, settings, findInBar)) return cast
        spellSideIndex++
        cast++
        DeployDebug.log("法术 第${cast}个（方式=${settings.mode.label} 已过${elapsedMs / 1000}s）")
        delayWithMultiplier(SPELL_CAST_GAP_MS)
    }
    return cast
}

/**
 * 源 `函数156a`: 放出攻城机器（`器列表` / `找机器`）或部落城堡援兵（`找援兵`），每局只放一次
 * （源 `已放援兵`）。
 *
 * 先在部署栏点中它的卡片（源 `taps(102, dY)`：栏内固定一行 + 找到的位置），再点该象限的
 * `中间` —— 攻城器/援兵是"放下即生效"，不铺线。
 *
 * @param side 落点象限，源用 `援兵位置`。
 * @return true when the unit was dropped.
 */
suspend fun deploySiegeOrReinforcement(
    settings: DeploySettings,
    side: DeploySide,
    findInBar: suspend () -> Point?
): Boolean {
    if (siegeDeployed) return false
    val hit = findInBar() ?: return false
    DeployDebug.log("攻城器/援兵 @(${hit.x},${hit.y}) → ${side.label} 中间")
    TouchActions.tap(hit.x, hit.y, delayTime = 200)
    delayWithMultiplier(100)
    val p = DeployGeometry.middleTap(side, DeployType.TROOP)
    TouchActions.tap(p.x, p.y, delayTime = settings.safeSpeed + 50)
    siegeDeployed = true
    return true
}

/**
 * 部署栏翻页 (源 `函数135a` / `函数136a` 的"长滑")：当前页的兵种都放完后左滑，让部署栏显示后面
 * 的兵种；[forward] = false 时右滑翻回。
 */
suspend fun swipeDeploymentBar(forward: Boolean) {
    val s = DeployGeometry.barSwipe(forward)
    DeployDebug.log("部署栏${if (forward) "左" else "右"}滑翻页 (${s[0]},${s[1]})->(${s[2]},${s[3]})")
    TouchActions.swipe(s[0], s[1], s[2], s[3], delayTime = 500, isJitter = false)
    delayWithMultiplier(400)
}

/**
 * Convenience presence check: returns the tap point if [schema] is visible in the deployment bar,
 * otherwise null. Thin wrapper so callers share one logging path.
 */
suspend fun selectInDeploymentBar(schema: ColorSchema, name: String? = null): Point? {
    val hit = findMultiColors(schema = schema)
    if (hit != null) {
        ShowMessage("部署栏命中：${name ?: schema.name ?: "未命名"} @ ${hit.x},${hit.y}")
    }
    return hit
}
