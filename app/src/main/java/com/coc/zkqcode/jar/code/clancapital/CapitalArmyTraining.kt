package com.coc.zkqcode.jar.code.clancapital

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.core.util.basic.ShowMessage
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
 * 偷袭骷髅打法（移植自源 函数29a），用于都城大厅等级 >= 8 且拥有「超级矿工」的账号。
 * - 有矿工分支：上半屏造超级矿工（10 轮），下半屏造野蛮人攻城槌（5）+ 超级野蛮人（5），
 *   并造 骷髅（2）/ 冰冻（2）/ 雷电（1）三种法术；
 * - 无矿工（野猪）分支：下半屏造攻城槌（10）+ 野猪偷袭队（10）+ 超级野蛮人（30），
 *   再补全套法术（骷髅2/冰冻2/雷电1/疗伤2/弹跳2）。
 * Count(n) 对应源 dczbdj 的最大点击次数 n，实际兵量由容量配额自动截断。
 */
private fun buildSkeletonPlan(hasMiner: Boolean): List<TrainStep> {
    val troops = if (hasMiner) listOf(
        TrainStep("超级矿工", MyColors.CapitalSuperMiner, TrainTarget.Count(10)),
        TrainStep("野蛮人攻城槌", MyColors.CapitalBattleRam, TrainTarget.Count(5)),
        TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Count(5))
    ) else listOf(
        TrainStep("野蛮人攻城槌", MyColors.CapitalBattleRam, TrainTarget.Count(10)),
        TrainStep("野猪突袭队", MyColors.CapitalHogRaider, TrainTarget.Count(10)),
        TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Count(30))
    )
    val spells = if (hasMiner) listOf(
        TrainStep("骷髅召唤法术", MyColors.CapitalSkeletonSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("冰霜法术", MyColors.CapitalFreezeSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("雷电法术", MyColors.CapitalLightningSpell, TrainTarget.Count(1), isSpell = true)
    ) else listOf(
        TrainStep("骷髅召唤法术", MyColors.CapitalSkeletonSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("冰霜法术", MyColors.CapitalFreezeSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("雷电法术", MyColors.CapitalLightningSpell, TrainTarget.Count(1), isSpell = true),
        TrainStep("疗伤法术", MyColors.CapitalHealingSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("弹跳法术", MyColors.CapitalJumpSpell, TrainTarget.Count(2), isSpell = true)
    )
    return troops + spells
}

/**
 * 偷袭默认打法（移植自源 函数28a），用于都城大厅等级 < 8 或无「超级矿工」的账号。
 * 兵种按源 函数28a 的优先级顺序造兵，法术统一在最后造。
 * 注：源对「攻城槌/超级野蛮人」「巨人/弓箭/法师」有互斥优先分支，本项目合并为顺序造兵，
 * 兵种均解锁时两种都会造（仍是填满军队，仅兵种组合略有差异）。
 */
private fun buildDefaultPlan(): List<TrainStep> {
    val troops = listOf(
        TrainStep("野蛮人攻城槌", MyColors.CapitalBattleRam, TrainTarget.Count(2)),
        TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Count(10)),
        TrainStep("超级巨人", MyColors.CapitalSuperGiant, TrainTarget.Count(3)),
        TrainStep("隐秘弓箭手", MyColors.CapitalSneakyArcher, TrainTarget.Count(3)),
        TrainStep("超级法师", MyColors.CapitalSuperWizard, TrainTarget.Count(3)),
        TrainStep("隐秘弓箭手", MyColors.CapitalSneakyArcher, TrainTarget.Count(3)),
        TrainStep("超级法师", MyColors.CapitalSuperWizard, TrainTarget.Count(60)),
        TrainStep("超级巨人", MyColors.CapitalSuperGiant, TrainTarget.Count(60)),
        TrainStep("隐秘弓箭手", MyColors.CapitalSneakyArcher, TrainTarget.Count(60)),
        TrainStep("超级野蛮人", MyColors.CapitalSuperBarbarian, TrainTarget.Count(60))
    )
    val spells = listOf(
        TrainStep("骷髅召唤法术", MyColors.CapitalSkeletonSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("冰霜法术", MyColors.CapitalFreezeSpell, TrainTarget.Count(2), isSpell = true),
        TrainStep("疗伤法术", MyColors.CapitalHealingSpell, TrainTarget.Count(3), isSpell = true),
        TrainStep("雷电法术", MyColors.CapitalLightningSpell, TrainTarget.Count(1), isSpell = true),
        TrainStep("弹跳法术", MyColors.CapitalJumpSpell, TrainTarget.Count(1), isSpell = true)
    )
    return troops + spells
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
    if (fallbackTotal <= 0) {
        ShowMessage("都城造兵：大厅等级无效，跳过")
        return false
    }

    // 源 函数30a + 函数29a：都城等级>=8 且拥有超级矿工 → 偷袭骷髅打法，否则偷袭默认打法
    val hasMiner = findTroop(MyColors.CapitalSuperMiner) != null
    val useSkeleton = capitalHallLevel >= 8 && hasMiner
    val plan = if (useSkeleton) {
        ShowMessage("都城造兵：采用偷袭骷髅打法（大厅等级 $capitalHallLevel，含超级矿工=$hasMiner）")
        buildSkeletonPlan(hasMiner)
    } else {
        ShowMessage("都城造兵：采用偷袭默认打法（大厅等级 $capitalHallLevel）")
        buildDefaultPlan()
    }
    ShowMessage("都城造兵：方案共 ${plan.size} 类单位，开始执行")

    for (step in plan) {
        val point = findTroop(step.schema)
        if (point == null) {
            // 该兵种未解锁 / 不在本都城可用，跳过（等价源 函数25a 判断兵种可用）
            ShowMessage("都城造兵：${step.name} 未找到，跳过")
            continue
        }
        val readout = if (step.isSpell) SPELL_READOUT else ARMY_READOUT
        val fallback = if (step.isSpell) CAPITAL_SPELL_CAPACITY_FALLBACK else fallbackTotal
        var taps = 0
        var lastUsed = readCapacity(readout)?.used ?: 0
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
            // 源 dczbdj：配额不足时点击无效（容量不增），立即停止该兵种，避免空点
            delayWithMultiplier(60)
            val after = readCapacity(readout)?.used ?: (lastUsed + 1)
            if (after <= lastUsed) break
            lastUsed = after
        }
        ShowMessage("都城造兵：${step.name} 已造 $taps 次（容量 ${readCapacity(readout)?.used ?: 0}/${readCapacity(readout)?.total ?: fallback}）")
    }

    if (findMultiColors(schema = MyColors.CapitalTrainConfirm) == null) {
        ShowMessage("都城造兵：未找到完成按钮，造兵可能失败")
        return false
    }
    TouchActions.tap(CONFIRM_TAP_X, CONFIRM_TAP_Y, delayTime = 1000)
    delayWithMultiplier(500)
    ShowMessage("都城造兵：已点完成，等待刷新")
    return true
}
