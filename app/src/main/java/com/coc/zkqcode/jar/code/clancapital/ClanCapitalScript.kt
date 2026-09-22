package com.coc.zkqcode.jar.code.clancapital

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.clancapital.attack.enterClanCapital
import com.coc.zkqcode.jar.code.clancapital.attack.isInClanCapital
import com.coc.zkqcode.jar.code.clancapital.attack.playCapitalRaid
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema
import java.util.Calendar

/**
 * Everything the clan capital does BESIDES the battle deploy itself, ported from the legacy freescript
 * in blocks (M4-③「都城入口拆块移植」):
 *
 *  - [isCapitalRaidTime]   is it raid time? (legacy awcocx_main.lua L93323~L93411)
 *  - [claimCapitalGold]    claim capital gold (legacy L92997~L93200)
 *  - [donateCapitalGold]   donate capital gold (legacy 函数268a L74528~L74760, simplified)
 *  - [startCapitalRaid]    start a raid (legacy L93493~L93529)
 *  - [playClanCapital]     orchestration: claim -> donate -> start raid -> fight
 *                          (entry / target picking / deploy live in attack/CapitalRaid.kt)
 *
 * Coordinates come from the legacy portrait 720x1280 buffers and use the 90° mapping to the landscape
 * 1280x720 screen (x' = y, y' = 719 - x).
 * TODO(on-device): there is no capital screenshot in this project, so none of these taps / feature hits
 * have been verified on a device yet.
 */

/** How many raids to play in a row (a raid weekend gives 5~6 attacks; the legacy exits on "no attacks left"). */
private const val MAX_RAID_BATTLES = 6

/**
 * Whether the clan-capital raid weekend is currently open - port of legacy L93323~L93411:
 * Friday after 15:03, all Saturday/Sunday, and Monday before 16:00 are "raid time"; anything else is not.
 *
 * @param now injectable for tests; defaults to the current time.
 */
fun isCapitalRaidTime(now: Calendar = Calendar.getInstance()): Boolean {
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)
    return when (now.get(Calendar.DAY_OF_WEEK)) {
        // Legacy: hour > 15 -> yes; hour == 15 and minute > 3 -> yes; otherwise no.
        Calendar.FRIDAY -> hour > 15 || (hour == 15 && minute > 3)
        Calendar.SATURDAY, Calendar.SUNDAY -> true
        // Legacy: hour >= 16 -> no, otherwise yes.
        Calendar.MONDAY -> hour < 16
        else -> false
    }
}

/**
 * Claims capital gold (legacy L92997~L93200):
 *   recentre the map -> find the mint and tap it -> tap the collect arrow -> collect per district ->
 *   close the coin panel.
 *
 * Difference from the legacy script: the legacy clears several tutorial popups (女提示 / 女思考提示)
 * with features this project has not migrated, so only the main path is kept. "Capital gold storage is
 * full" is still honoured, exactly like the source.
 *
 * @return true when at least one district was collected.
 */
suspend fun claimCapitalGold(): Boolean {
    SceneState.setFlowNode("都城-领都城币")
    if (!isInClanCapital() && !enterClanCapital()) return false
    // Legacy L93009 swipes(1, 186, 286, 1200, 1630, 300): fling the map back to its origin
    // (mapped to landscape and clamped to the screen).
    TouchActions.swipe(286, 533, 1279, 0, delayTime = 300)
    delayWithMultiplier(200)

    val workshop = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalMintWorkshop, MyColors.CapitalMintWorkshop2),
        duration = 3_000,
        increment = 1
    )
    if (workshop == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到铸币坊，跳过领都城币")
        return false
    }
    // Legacy L93019 taps(intX - 5, intY + 20): tap on the building body.
    TouchActions.tap(workshop.x - 5, workshop.y + 20, delayTime = 1500)

    // Legacy L93027~L93071: up to 10 rounds; tap the collect arrow (slightly left of it) and stop as
    // soon as the coin panel shows up.
    var onCoinScreen = false
    for (i in 1..10) {
        if (findMultiColors(schema = MyColors.CapitalCoinScreen, increment = 1) != null) {
            onCoinScreen = true
            break
        }
        val arrow = findMultiColors(schema = MyColors.CapitalCollectArrow, increment = 1)
        if (arrow != null) {
            TouchActions.tap(arrow.x - 100, arrow.y, delayTime = 2000)
        }
        delayWithMultiplier(300)
    }
    if (!onCoinScreen) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：没进到都城币界面")
        return false
    }

    if (findMultiColors(schema = MyColors.CapitalCoinFull, increment = 1) != null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：都城币已满，不收集")
        closeCoinScreen()
        return false
    }

    var collected = false
    // Legacy L93107 onwards: up to 4 rounds of "tap a district collect button -> confirm".
    for (i in 1..4) {
        val collect = findMultiColors(schema = MyColors.CapitalCollectButton, increment = 1) ?: break
        TouchActions.tap(collect.x, collect.y, delayTime = 1000)
        collected = true
        findMultiColors(schema = MyColors.CapitalCollectConfirm, increment = 1)?.let {
            TouchActions.tap(it.x, it.y, delayTime = 2000)
        }
    }
    closeCoinScreen()
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：领都城币${if (collected) "完成" else "没有可收的"}")
    return collected
}

/**
 * Leaves the capital coin panel and returns to the capital map.
 *
 * The legacy L93189 taps the mint panel's own "done" button (feature 2621E3, not migrated here), so this
 * uses the already-verified 左下角回营/都城界面 button instead - same meaning ("leave the sub-screen and go
 * back to the capital map"), and the legacy flow returns to camp anyway right after.
 */
private suspend fun closeCoinScreen() {
    findMultiColors(schema = MyColors.BottomLeftReturnToCamp, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 1000)
    }
}

/**
 * Donates capital gold (simplified port of legacy 函数268a L74528~L74760):
 *   open the building list -> find the vault donate point -> spam "+" to max the amount -> confirm.
 *
 * Differences from the legacy script (both have fallbacks in the source, nothing invented here):
 *  1. the legacy first locates the vault with `findImage("都城宝库.png")`; image matching is not wired up
 *     in this project yet, so the source's own four colour fallbacks (CapitalDonatePoint~4) are used;
 *  2. the legacy HOLDS the "+" button to max the amount; TouchActions has no long-press API, so it taps
 *     repeatedly instead.
 */
suspend fun donateCapitalGold(): Boolean {
    SceneState.setFlowNode("都城-捐都城币")
    if (!isInClanCapital() && !enterClanCapital()) return false

    // Legacy L74600~L74606: open the building list by tapping (692,537) (landscape (537,27)).
    if (findMultiColors(schema = MyColors.CapitalBuildingListMain, increment = 1) == null) {
        TouchActions.tap(537, 27, delayTime = 500)
    }

    val point = findMultiColorsUntil(
        schemas = listOf(
            MyColors.CapitalDonatePoint,
            MyColors.CapitalDonatePoint2,
            MyColors.CapitalDonatePoint3,
            MyColors.CapitalDonatePoint4
        ),
        duration = 3_000,
        increment = 1
    )
    if (point == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到宝库捐币点")
        return false
    }
    TouchActions.tap(point.x, point.y, delayTime = 1200)

    // Legacy L74696: the donate amount "+" button (tap it up to the maximum).
    for (i in 1..20) {
        val plus = findMultiColors(schema = MyColors.CapitalDonatePlus, increment = 1) ?: break
        TouchActions.tap(plus.x, plus.y, delayTime = 100)
    }

    // Legacy L74678: donate confirmation.
    findMultiColors(schema = MyColors.CapitalDonateConfirm, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 500)
    }
    // Legacy L74670 taps(76,950): final confirmation (landscape (950,643)).
    TouchActions.tap(950, 643, delayTime = 1500)
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：捐都城币完成")
    return true
}

/**
 * Starts a raid (legacy L93493~L93529): skipped outside raid time and when the button is greyed out
 * (raid already running); otherwise taps the button and then the confirmation.
 */
suspend fun startCapitalRaid(): Boolean {
    SceneState.setFlowNode("都城-发起突袭")
    if (!isCapitalRaidTime()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：不在突袭时间段，跳过发起突袭")
        return false
    }
    if (!isInClanCapital() && !enterClanCapital()) return false
    if (findMultiColors(schema = MyColors.CapitalRaidStartGray, increment = 1) != null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：发起突袭按钮为灰色（突袭已开启）")
        return false
    }
    val start = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalRaidStartButton),
        duration = 3_000,
        increment = 1
    ) ?: return false
    TouchActions.tap(start.x, start.y, delayTime = 1000)
    val confirm = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalRaidStartConfirm),
        duration = 5_000,
        increment = 1
    ) ?: return false
    TouchActions.tap(confirm.x, confirm.y, delayTime = 1000)
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：已发起突袭")
    return true
}

/**
 * Clan-capital stage of the main loop (the legacy main loop's 领都城币 / 捐都城币 / 发起突袭 / 打突袭 blocks).
 *
 * Return convention matches [com.coc.zkqcode.jar.code.builderbase.playBuilderBase]: **false means "this
 * stage is done"**, and `runMainScript` ends the step block when it sees false.
 *
 * Every feature is driven by its own config switch (`play_raid` / `start_raid` / `claim_capital_gold` /
 * `donate_capital_gold`); with all four off nothing is tapped and the stage returns immediately.
 */
suspend fun playClanCapital(): Boolean {
    SceneState.setFlowNode("都城")
    val claimEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.CLAIM_CAPITAL_GOLD.key)
    val donateEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.DONATE_CAPITAL_GOLD.key)
    val playRaidEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.PLAY_RAID.key)
    val startRaidEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.START_RAID.key)
    if (!claimEnabled && !donateEnabled && !playRaidEnabled && !startRaidEnabled) return false

    if (!isInClanCapital() && !enterClanCapital()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：没能进入都城，跳过都城流程")
        return false
    }

    if (claimEnabled) claimCapitalGold()
    if (donateEnabled) donateCapitalGold()
    if (startRaidEnabled) startCapitalRaid()

    if (playRaidEnabled) {
        if (!isCapitalRaidTime()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：不在突袭时间段（周五15:03~周一16:00），跳过攻打都城")
        } else {
            var battles = 0
            while (battles < MAX_RAID_BATTLES && playCapitalRaid()) battles++
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：本次共打了${battles}场突袭")
        }
    }
    SceneState.setFlowNode("都城流程完成")
    return false
}
