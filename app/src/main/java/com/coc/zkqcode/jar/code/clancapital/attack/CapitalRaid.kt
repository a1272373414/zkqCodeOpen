package com.coc.zkqcode.jar.code.clancapital.attack

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.clancapital.trainCapitalArmy
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.getConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema

/**
 * Clan-capital RAID ENTRY, ported from the legacy freescript in four blocks (M4-③「都城入口拆块移植」).
 *
 * The legacy script implements "enter capital -> pick target -> attack -> deploy -> return to camp" as
 * one giant `repeat` block full of `goto` jumps (awcocx_main.lua, legacy lines L94279~L95400). Porting
 * it verbatim would copy that spaghetti into Kotlin, so it is split into four self-contained blocks:
 *
 *   B1 [isInClanCapital]        is the camera on a clan-capital screen?
 *   B2 [enterClanCapital] / [openCapitalRaidMap]
 *                               enter the capital and open the raid map (wait for 都城地图小船)
 *   B3 [pickCapitalTarget]      函数269a (count stars) + 函数270a (9-district grid): prefer 2 > 1 > 0 stars
 *   B4 [startCapitalBattle] / [finishCapitalBattle]
 *                               tap the district -> bottom attack -> army attack -> wait for the deploy
 *                               bar (give-up button) -> deploy (M4-②) -> return to camp
 *
 * [playCapitalRaid] orchestrates one full raid; `ClanCapitalScript.playClanCapital()` decides WHEN to run it.
 *
 * All coordinates come from the legacy script and use the project-wide 90° mapping
 * (portrait 720x1280 -> landscape 1280x720: x' = y, y' = 719 - x). Each block quotes the legacy line it
 * was ported from so it can be re-checked against the source.
 *
 * TODO(on-device): there is no capital screenshot in this project, so the taps / feature hits of B2-B4
 * have NOT been verified on a device yet.
 */

// ---------------------------------------------------------------------------------------------
// B1 - clan capital screen detection
// ---------------------------------------------------------------------------------------------

/**
 * True when the camera is on a clan-capital screen.
 *
 * The legacy script uses ONE feature for both 左下角回营 and 都城界面 (awcocx_main.lua L37875/L37877),
 * migrated as `MyColors.ClanCapitalEntry`. That button also exists in a normal village, so callers must
 * only ask this AFTER the village / training / battle questions - which is exactly where
 * [com.coc.zkqcode.jar.code.universal.detectCurrentScene] asks it.
 */
suspend fun isInClanCapital(): Boolean =
    findMultiColors(schema = MyColors.ClanCapitalEntry, increment = 1) != null

// ---------------------------------------------------------------------------------------------
// B2 - enter the capital / open the raid map
// ---------------------------------------------------------------------------------------------

/** Raid map button: legacy L94457 `taps(124, 1165)` -> landscape (1165, 595). */
private val CAPITAL_MAP_BUTTON = intArrayOf(1165, 595)

/**
 * Opens the capital map (tap the map button, then wait for 都城地图小船) - legacy L94457~L94477.
 *
 * @return true when 都城地图小船 shows up within [timeoutSeconds].
 */
suspend fun openCapitalRaidMap(timeoutSeconds: Int = 15): Boolean {
    TouchActions.tap(CAPITAL_MAP_BUTTON[0], CAPITAL_MAP_BUTTON[1], delayTime = 1500)
    val boat = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalRaidMapBoat),
        duration = timeoutSeconds * 1000,
        increment = 1
    )
    if (boat == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到都城地图小船，可能不在都城界面")
        return false
    }
    return true
}

/**
 * 源脚本 函数319a 在识别都城入口之前，会先把镜头拉到固定的位置与缩放，否则入口不在画面里。
 * 原序列（竖屏）：双指张开 touchDown(1,360,630)/touchDown(2,360,650)
 * → touchMoveEx(1,360,585)/touchMoveEx(2,360,695) → swipes(186,286 → 1200,1630)；
 * 坐标已按文档 11.3 的 90° 映射（realX=srcY, realY=720-srcX）换算到横屏。
 */
private suspend fun zoomToCapitalShore() {
    TouchActions.touchDown(630f, 360f, 1)
    TouchActions.touchDown(650f, 360f, 2)
    TouchActions.touchMove(585f, 360f, 1)
    TouchActions.touchMove(695f, 360f, 2)
    TouchActions.releaseAllPointers()
    delayWithMultiplier(300)
    TouchActions.swipe(286, 719 - 186, 1630, 719 - 1200, 300)
    delayWithMultiplier(200)
}

/**
 * Enters the clan capital: returns immediately when already there, otherwise taps the capital entry
 * button (legacy 函数319a - the button on the clan screen that leads to the capital) and waits.
 *
 * NOTE: the legacy "enter capital" step lives inside its clan-hopping flow (L94231~L94279); this block
 * only keeps "tap the entry, then wait for the capital screen". Without an entry feature on screen it
 * returns false and the caller decides whether to go back to the main village first.
 */
suspend fun enterClanCapital(timeoutSeconds: Int = 20): Boolean {
    if (isInClanCapital()) return true
    zoomToCapitalShore()
    val entry = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalClanEntryButton, MyColors.CapitalClanEntryButton2),
        duration = 3_000,
        increment = 1
    )
    if (entry == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到都城入口按钮")
        return false
    }
    TouchActions.tap(entry.x, entry.y, delayTime = 1500)
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    while (System.currentTimeMillis() < deadline) {
        if (isInClanCapital()) return true
        delayWithMultiplier(500)
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：进入都城超时")
    return false
}

// ---------------------------------------------------------------------------------------------
// B3 - pick a district (legacy 函数269a + 函数270a)
// ---------------------------------------------------------------------------------------------

/**
 * One attackable district on the raid map.
 *
 * @param mapIndex 1..9, the legacy 可打地图 index (1 = 都城之颠 ... 9 = 哥布林矿)
 * @param stars    stars already taken here (0..2); the higher the better
 * @param x,y      landscape tap point (the legacy computes 地图x/地图y in portrait, then maps them)
 */
data class CapitalTarget(val mapIndex: Int, val stars: Int, val x: Int, val y: Int)

/** Legacy 函数270a: offset of each district from the map anchor (portrait x). */
private val DISTRICT_DX = intArrayOf(24, -117, -190, -322, -355, -286, -439, -480, -477)

/** Legacy 函数270a: the "portrait y" tapped for each district (it becomes the landscape x). */
private val DISTRICT_TAP_X = intArrayOf(607, 781, 613, 470, 702, 909, 311, 564, 847)

/** Legacy 函数269a scans 竖屏 (位置Nx, 520)-(位置Nx+40, 725); in landscape that is x in [520,725] and a 40px y band. */
private const val STAR_REGION_X1 = 520
private const val STAR_REGION_X2 = 725
private const val DISTRICT_SPAN = 40

/**
 * Legacy 函数269a counts stars with `findMultiColorAll` (return every hit). This project has no
 * "return all matches" API, so the district rect is split into 3 bands along landscape y and each band
 * is probed once - one hit per band = one star. Same ordering, and >=3 naturally means "not attackable".
 */
private const val MAX_STARS = 3

/** Legacy 函数269a/270a `ret` == 3: locked or somebody is already attacking (not attackable). */
private const val DISTRICT_UNAVAILABLE = -1

private const val LAND_H = 720

/**
 * Scans the 9 districts and picks one to attack. Port of 函数270a (L75166~L75332):
 *   1. locate the map with the 都城地图锚点 feature (legacy `intX`);
 *   2. per district: 函数269a counts stars (missing white star / blue lock => locked);
 *   3. pick order: 2 stars > 1 star > 0 stars; for 0 stars also ask "somebody is attacking".
 *
 * @return the best district, or null when nothing is attackable.
 */
suspend fun pickCapitalTarget(): CapitalTarget? {
    val anchor = findMultiColors(schema = MyColors.CapitalRaidMapAnchor, increment = 1)
    if (anchor == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到都城地图锚点，无法选择目标")
        return null
    }
    // 位置Nx is a portrait x, so the landscape tap y is 719 - 位置Nx; the anchor is already mapped, so
    // the district rows can be derived from anchor.y.
    val states = IntArray(DISTRICT_DX.size) { DISTRICT_UNAVAILABLE }
    for (i in DISTRICT_DX.indices) {
        val tapY = anchor.y - DISTRICT_DX[i]
        val y1 = tapY - DISTRICT_SPAN
        if (y1 < 0 || tapY > LAND_H - 1) continue
        states[i] = readDistrictStars(STAR_REGION_X1, y1, STAR_REGION_X2, tapY)
    }
    for (wanted in 2 downTo 0) {
        for (i in states.indices) {
            if (states[i] != wanted) continue
            val target = CapitalTarget(
                mapIndex = i + 1,
                stars = wanted,
                x = DISTRICT_TAP_X[i],
                y = anchor.y - DISTRICT_DX[i]
            )
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：选中子城${target.mapIndex}（${wanted}星）")
            return target
        }
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：没有可打的子城（都未解锁/都有人在进攻）")
    return null
}

/**
 * 函数269a equivalent: how many stars a district already has.
 *
 * @return 0..2 when attackable, [DISTRICT_UNAVAILABLE] when locked or being attacked by somebody else.
 */
private suspend fun readDistrictStars(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    val star = MyColors.raidStar(x1, y1, x2, y2)
    val locked = MyColors.raidLocked(x1, y1, x2, y2)
    if (findMultiColors(schema = locked, increment = 1) != null) return DISTRICT_UNAVAILABLE
    if (findMultiColors(schema = star, increment = 1) == null) return DISTRICT_UNAVAILABLE

    var stars = 0
    val height = y2 - y1
    for (band in 0 until MAX_STARS) {
        val bandY1 = y1 + height * band / MAX_STARS
        val bandY2 = y1 + height * (band + 1) / MAX_STARS
        val countSchema = MyColors.raidStarCount(x1, bandY1, x2, bandY2)
        if (findMultiColors(schema = countSchema, increment = 1) != null) stars++
    }
    if (stars == 0) {
        // The legacy script only asks "somebody is attacking" when no star was counted
        // (函数269a's `elseif v then` branch).
        val occupied1 = MyColors.raidOccupied(x1, y1, x2, y2)
        if (findMultiColors(schema = occupied1, increment = 1) != null) return DISTRICT_UNAVAILABLE
        val occupied2 = MyColors.raidOccupied2(x1, y1, x2, y2)
        if (findMultiColors(schema = occupied2, increment = 1) != null) return DISTRICT_UNAVAILABLE
    }
    return stars.coerceAtMost(2)
}

// ---------------------------------------------------------------------------------------------
// B4 - attack -> deploy -> return to camp
// ---------------------------------------------------------------------------------------------

/** Bottom attack button: legacy L94607 `taps(78, 754)` -> landscape (754, 641). */
private val BOTTOM_ATTACK_BUTTON = intArrayOf(754, 641)

/** Army attack button: legacy L94629 `taps(305, 1054)` -> landscape (1054, 414). */
private val ARMY_ATTACK_BUTTON = intArrayOf(1054, 414)

/** "Edit capital army" (shown first when the army is empty): legacy L94615 -> landscape (1053, 497). */
private val EDIT_CAPITAL_ARMY_BUTTON = intArrayOf(1053, 497)

/** How long to wait for the battle to start (legacy L94685 waits 15s in 15 one-second rounds). */
private const val BATTLE_ENTER_TIMEOUT_MS = 30_000

/**
 * Starts the battle for [target] (legacy L94599~L94749).
 *
 * The legacy script decides whether the attack buttons are usable with `cmpColorEx` (comparing several
 * absolute pixels), an API this project does not have. So instead it taps the known points in order and
 * uses "did the give-up button appear?" as the real proof that the battle started:
 *   tap district -> bottom attack -> army attack -> dismiss the "storage limit" popup -> wait for give-up.
 */
suspend fun startCapitalBattle(target: CapitalTarget): Boolean {
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：进攻子城${target.mapIndex}（${target.stars}星）")
    TouchActions.tap(target.x, target.y, delayTime = 800)
    TouchActions.tap(BOTTOM_ATTACK_BUTTON[0], BOTTOM_ATTACK_BUTTON[1], delayTime = 600)
    TouchActions.tap(ARMY_ATTACK_BUTTON[0], ARMY_ATTACK_BUTTON[1], delayTime = 1000)
    // The "storage limit reached" popup covers the battlefield (legacy L94633 taps it away).
    findMultiColors(schema = MyColors.CapitalRaidStorageFull, increment = 1)?.let {
        TouchActions.tap(it.x, it.y, delayTime = 1000)
    }
    val giveUp = findMultiColorsUntil(
        schemas = listOf(MyColors.GiveUpButton),
        duration = BATTLE_ENTER_TIMEOUT_MS,
        increment = 1
    )
    if (giveUp == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未进入战斗（没等到放弃按钮）")
        return false
    }
    return true
}

/**
 * When the capital army is empty the game shows "edit capital army" first: tap it and hand over to
 * [trainCapitalArmy].
 *
 * @param capitalHallLevel only used as the fallback for the capacity OCR.
 */
suspend fun prepareCapitalArmy(capitalHallLevel: Int): Boolean {
    TouchActions.tap(EDIT_CAPITAL_ARMY_BUTTON[0], EDIT_CAPITAL_ARMY_BUTTON[1], delayTime = 1500)
    return trainCapitalArmy(capitalHallLevel)
}

/**
 * Waits for the battle to end and returns to the capital map (legacy L94715 onwards: wait for the
 * give-up button to disappear, then tap 左下角回营).
 *
 * @return true when the give-up button disappeared (battle finished).
 */
suspend fun finishCapitalBattle(timeoutSeconds: Int = 180): Boolean {
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    while (System.currentTimeMillis() < deadline) {
        if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) == null) {
            backToCapitalMap()
            return true
        }
        delayWithMultiplier(1500)
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：等待战斗结束超时，直接回营")
    backToCapitalMap()
    return false
}

/** Taps 左下角回营/都城界面 to go back to the capital map (same feature as [MyColors.ClanCapitalEntry]). */
private suspend fun backToCapitalMap() {
    val camp = findMultiColorsUntil(
        schemas = listOf(MyColors.BottomLeftReturnToCamp),
        duration = 5_000,
        increment = 1
    ) ?: return
    TouchActions.tap(camp.x, camp.y, delayTime = 2000)
}

// ---------------------------------------------------------------------------------------------
// Orchestration: one full raid (open map -> pick target -> attack -> deploy -> return)
// ---------------------------------------------------------------------------------------------

/**
 * Plays one clan-capital raid. Pre-condition: the caller already checked that it IS raid time and that
 * the player is inside the capital.
 *
 * @return true when the raid was played (not necessarily a 3-star), false when there was no attackable
 *         district or the battle could not be started.
 */
suspend fun playCapitalRaid(): Boolean {
    if (findMultiColors(schema = MyColors.CapitalRaidAttackCountUsed, increment = 1) != null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：进攻次数已用完")
        return false
    }
    if (!openCapitalRaidMap()) return false

    // M4-③ §9.7(5)：攻打前若开启「自动配兵」，先在突袭地图层补齐都城军队（源脚本「首次造兵」逻辑）。
    // prepareCapitalArmy 内部会点「编辑都城军队」再按等级配兵；军队已满时函数会自动空转返回，无需额外判定。
    // 注意：坐标/面板判定仍待真机验证（离线无都城战斗截图），故默认关闭，由配置开关控制。
    if (getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.TRAIN_CAPITAL_ARMY.key)) {
        val level = runCatching {
            getConfigRuntime(Schema.MAIN_BASE_SETTINGS.CAPITAL_HALL_LEVEL.key).trim().toIntOrNull() ?: 10
        }.getOrDefault(10)
        prepareCapitalArmy(level)
    }

    val target = pickCapitalTarget() ?: return false
    if (!startCapitalBattle(target)) return false

    // Deploy (M4-②: terrain scan + 12 troops / 5 spells)
    val deployed = capitalDeployArmy()
    if (!deployed) ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：本场没有放出任何兵/法术")

    finishCapitalBattle()
    return true
}
