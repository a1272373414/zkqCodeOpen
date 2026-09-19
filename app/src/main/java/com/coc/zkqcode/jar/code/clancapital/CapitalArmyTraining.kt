package com.coc.zkqcode.jar.code.clancapital

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.recognizer.PixelFontOcr

/**
 * Trains the clan capital army on the clan capital army screen.
 *
 * The clan capital is a standalone system and must not be mixed up with the main village or the
 * builder base: it has its own roster (17 troops + 7 spells, see the `Capital*` entries in
 * [MyColors]) and its own capacity that grows with the capital hall level.
 *
 * The capacity is always read from the screen ("used/total") and the level table below is only
 * a fallback, because a level 9/10 capital is not necessarily at max capacity.
 *
 * Troops that are not unlocked simply do not appear on the army screen, so they are skipped and
 * the same plan also works for partially upgraded capitals.
 */

/** Fallback army capacity per capital hall level. */
fun capitalArmyCapacity(capitalHallLevel: Int): Int = when {
    capitalHallLevel <= 1 -> 0
    capitalHallLevel == 2 -> 50
    capitalHallLevel == 3 -> 80
    capitalHallLevel == 4 -> 110
    capitalHallLevel == 5 -> 140
    capitalHallLevel == 6 -> 170
    capitalHallLevel == 7 -> 200
    capitalHallLevel == 8 -> 225
    else -> 250
}

// "used/total" readouts, verified on real 1280x720 capital army screenshots.
// The spell readout shows the spell slots (7 on the observed capitals).
private val ARMY_READOUT = intArrayOf(172, 88, 272, 108)
private val SPELL_READOUT = intArrayOf(323, 89, 379, 108)

/** Fallback spell slot count, used only when the spell readout cannot be read. */
private const val CAPITAL_SPELL_CAPACITY_FALLBACK = 7

// Green confirm button, verified on 都城-配兵01..07 (bbox 761,269-928,334).
private const val CONFIRM_TAP_X = 844
private const val CONFIRM_TAP_Y = 301

// The troop list scrolls horizontally; both swipes run along this row.
private const val LIST_SCROLL_Y = 540

private data class Capacity(val used: Int, val total: Int)

/**
 * Reads a "used/total" readout. The '/' glyph is not part of the pixel font, so the text comes
 * back like "10?250" and we simply take the last two numbers.
 */
private suspend fun readCapacity(region: IntArray): Capacity? {
    val text = PixelFontOcr.recognizeDigits(region[0], region[1], region[2], region[3])
        ?: return null
    val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
    if (numbers.size < 2) return null
    return Capacity(numbers[numbers.size - 2], numbers[numbers.size - 1])
}

/** Reads "used/total" of the capital army (housing space). */
suspend fun readCapitalArmyCapacity(): Pair<Int, Int>? =
    readCapacity(ARMY_READOUT)?.let { it.used to it.total }

/** Reads "used/total" of the capital spell slots. */
suspend fun readCapitalSpellSlots(): Pair<Int, Int>? =
    readCapacity(SPELL_READOUT)?.let { it.used to it.total }

private sealed class TrainTarget {
    data class Count(val taps: Int) : TrainTarget()
    /** Keep tapping until used/total reaches this fraction of the capacity. */
    data class UntilFraction(val fraction: Double) : TrainTarget()
    /** Keep tapping until the army is full. */
    object Fill : TrainTarget()
}

private class TrainStep(
    val name: String,
    val schema: ColorSchema,
    val target: TrainTarget,
    /** Spells are counted against the spell readout instead of the army readout. */
    val isSpell: Boolean = false
)

/**
 * Spell composition, shared by every capital hall level.
 * Each entry is the number of that spell to train; the spell capacity read from the screen
 * stops the sequence early when there is no room left.
 */
private fun spellPlan(): List<TrainStep> = listOf(
    TrainStep("骷髅召唤法术", MyColors.CapitalSkeletonSpell, TrainTarget.Count(2), isSpell = true),
    TrainStep("冰霜法术", MyColors.CapitalFreezeSpell, TrainTarget.Count(3), isSpell = true),
    TrainStep("雷电法术", MyColors.CapitalLightningSpell, TrainTarget.Count(1), isSpell = true),
    TrainStep("弹跳法术", MyColors.CapitalJumpSpell, TrainTarget.Count(1), isSpell = true),
    TrainStep("疗伤法术", MyColors.CapitalHealingSpell, TrainTarget.Count(3), isSpell = true)
)

/**
 * Default plans per capital hall level:
 * - <= 8 : half hog raiders, half battle rams, super barbarians fill the rest
 * - 9    : 10 super miners, 1 mountain golem, 6 hog raiders, super barbarians fill the rest
 * - 10   : 10 super miners, battle rams fill the rest
 */
private fun planFor(capitalHallLevel: Int): List<TrainStep> {
    val troops = when {
        capitalHallLevel <= 8 -> listOf(
            TrainStep("野猪突袭队", MyColors.CapitalHogRaider, TrainTarget.UntilFraction(0.5)),
            TrainStep("野蛮人攻城槌", MyColors.CapitalBattleRam, TrainTarget.UntilFraction(1.0)),
            TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Fill)
        )
        capitalHallLevel == 9 -> listOf(
            TrainStep("超级矿工", MyColors.CapitalSuperMiner, TrainTarget.Count(10)),
            TrainStep("高山戈仑", MyColors.CapitalMountainGolem, TrainTarget.Count(1)),
            TrainStep("野猪突袭队", MyColors.CapitalHogRaider, TrainTarget.Count(6)),
            TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Fill)
        )
        else -> listOf(
            TrainStep("超级矿工", MyColors.CapitalSuperMiner, TrainTarget.Count(10)),
            TrainStep("野蛮人攻城槌", MyColors.CapitalBattleRam, TrainTarget.Fill)
        )
    }
    // Troops first, then scroll back for the spells (same order as the legacy script).
    return troops + spellPlan()
}

/** Swipes the troop list one screen to the right or back to the left. */
private suspend fun scrollTroopList(toRight: Boolean) {
    if (toRight) {
        TouchActions.swipe(292, LIST_SCROLL_Y, 1100, LIST_SCROLL_Y, delayTime = 425)
    } else {
        TouchActions.swipe(1100, LIST_SCROLL_Y, 292, LIST_SCROLL_Y, delayTime = 425)
    }
}

/** Finds a troop icon, scrolling the list when it is not on the visible screen. */
private suspend fun findTroop(schema: ColorSchema): Point? {
    findMultiColors(schema = schema)?.let { return it }
    scrollTroopList(toRight = true)
    delayWithMultiplier(200)
    findMultiColors(schema = schema)?.let { return it }
    scrollTroopList(toRight = false)
    delayWithMultiplier(200)
    return findMultiColors(schema = schema)
}

/**
 * Trains the capital army for [capitalHallLevel] and confirms it.
 *
 * @return true when the training was confirmed, false when nothing could be confirmed.
 */
suspend fun trainCapitalArmy(capitalHallLevel: Int, maxTapsPerStep: Int = 120): Boolean {
    val fallbackTotal = capitalArmyCapacity(capitalHallLevel)
    if (fallbackTotal <= 0) return false

    for (step in planFor(capitalHallLevel)) {
        val point = findTroop(step.schema)
        if (point == null) {
            // Not unlocked / not available on this capital -> skip this troop.
            continue
        }
        val readout = if (step.isSpell) SPELL_READOUT else ARMY_READOUT
        val fallback = if (step.isSpell) CAPITAL_SPELL_CAPACITY_FALLBACK else fallbackTotal
        var taps = 0
        while (taps < maxTapsPerStep) {
            val capacity = readCapacity(readout)
            val total = capacity?.total ?: fallback
            val used = capacity?.used ?: 0
            val done = when (step.target) {
                is TrainTarget.Count -> taps >= step.target.taps
                is TrainTarget.UntilFraction -> used.toDouble() / total.toDouble() >= step.target.fraction
                is TrainTarget.Fill -> used >= total
            }
            if (done) break
            TouchActions.tap(point.x, point.y, delayTime = 50)
            taps++
        }
    }

    if (findMultiColors(schema = MyColors.CapitalTrainConfirm) == null) return false
    TouchActions.tap(CONFIRM_TAP_X, CONFIRM_TAP_Y, delayTime = 1000)
    delayWithMultiplier(500)
    return true
}
