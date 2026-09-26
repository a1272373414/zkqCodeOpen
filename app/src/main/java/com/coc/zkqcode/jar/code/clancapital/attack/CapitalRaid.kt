package com.coc.zkqcode.jar.code.clancapital.attack

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.topjohnwu.superuser.Shell
import com.coc.zkqcode.jar.code.clancapital.saveCapitalScreenshot
import com.coc.zkqcode.jar.code.clancapital.trainCapitalArmy
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.CameraState
import com.coc.zkqcode.jar.code.universal.GameScene
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsAll
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.recoverToMainScreen
import com.coc.zkqcode.jar.code.universal.sweepBlockingPopups
import com.coc.zkqcode.jar.code.universal.smalltools.enterMainBase
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
 * 都城入口定位（源脚本 函数319a，awcocx_main.lua L10894-10908）：
 * 源脚本通过全局镜头状态标记（已放大/已缩小画面）保证找入口时镜头处于确定缩放等级；
 * 无状态实现必须等价复刻：**先缩小到已知基准，再放大固定量**——否则镜头起始缩放随机，
 * 单次放大后等级不确定，热气球特征的偏移间距（9~42px，按确定等级标定）永远对不上。
 *
 * 流程：pinchIn 缩小到基准（与 zoomSmallMainBase 相同手势）→ 双指分步张开放大固定量
 * → 按源映射 swipes(186,286→1200,1630) 拖到海岸（终点越界交由系统钳制）。
 * 踩坑记录：手势两指相距仅20px 且瞬移会被判定为"单击"每轮选中建筑（已用分步张开修复）。
 */
private suspend fun zoomToCapitalShore() {
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：先缩小到基准再放大定位海岸")
    // 1. 双指捏合缩小到已知基准。
    // 踩坑：pinchIn 默认 duration 随机(300-400ms)且带抖动，每次缩小程度不同 →
    // 镜头落点不可复现，气球有时被拖出画面，特征永远对不上。必须固定时长并关闭抖动。
    TouchActions.pinchIn(141, 423, 1052, 352, 638, 365, duration = 350L, isJitter = false)
    delayWithMultiplier(200)
    // 2. 双指在不同位置按下（160px 间距），分步平滑张开放大固定量（源脚本 touchMoveEx 语义）
    TouchActions.touchDown(560f, 360f, 1)
    TouchActions.touchDown(720f, 360f, 2)
    delayWithMultiplier(80)
    TouchActions.touchMove(540f, 360f, 1, isJitter = false)
    TouchActions.touchMove(740f, 360f, 2, isJitter = false)
    delayWithMultiplier(60)
    TouchActions.touchMove(505f, 360f, 1, isJitter = false)
    TouchActions.touchMove(775f, 360f, 2, isJitter = false)
    delayWithMultiplier(60)
    TouchActions.touchMove(470f, 360f, 1, isJitter = false)
    TouchActions.touchMove(810f, 360f, 2, isJitter = false)
    delayWithMultiplier(80)
    TouchActions.releaseAllPointers()
    delayWithMultiplier(300)
    // 3. 源脚本 swipes(186,286 → 1200,1630,300) 的 90° 映射原样（终点越界按系统钳制处理）
    TouchActions.swipe(286, 719 - 186, 1630, 719 - 1200, 300)
    delayWithMultiplier(200)
    // 源 函数319a 放大后写 `_ENV["已缩小画面"] = false`：放大 + 平移已破坏"通用远景"，
    // 主世界后续功能必须重新缩小，不能沿用旧的"已缩小"状态。
    CameraState.markMainVillageZoomedIn()
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
    // 源脚本 函数319a 要求必须在主世界默认场景才能识别都城入口；
    // 若当前在夜世界/训练/其他子界面，先回到主世界。
    if (SceneState.currentScene != GameScene.MAIN_VILLAGE) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：当前不在主世界，先回主世界")
        saveCapitalScreenshot("capital_entry_pre_recover")
        // recoverToMainScreen 只保证回到游戏主界面，可能停在夜世界营地；
        // 若在夜世界，需要显式切回主世界。
        when (SceneState.currentScene) {
            GameScene.BUILDER_BASE -> {
                ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：当前在夜世界，执行回主世界")
                if (!enterMainBase()) {
                    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：从夜世界回主世界失败")
                    return false
                }
            }
            else -> {
                if (!recoverToMainScreen(30)) {
                    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：回主世界失败，无法进入都城")
                    return false
                }
            }
        }
        // 回主世界过程中可能误进都城（左下角回营特征与都城入口共用），再检查一次
        if (isInClanCapital()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：回主世界后已在都城")
            return true
        }
    }
    // 清理掉线/红x/通用弹窗（不能用盲点点击——会选中建筑弹出信息框遮挡入口，2026-09-26 踩坑）
    sweepBlockingPopups()
    if (isInClanCapital()) return true
    // 按源脚本顺序：先缩放归位镜头（热气球特征的偏移按归位后镜头标定），再找入口
    zoomToCapitalShore()
    var entry = findMultiColorsUntil(
        schemas = listOf(MyColors.CapitalClanEntryButton, MyColors.CapitalClanEntryButton2),
        duration = 4_000,
        increment = 1
    )
    if (entry == null) {
        // 第一级自愈：清掉 zoom 可能误触出的建筑菜单，重新缩放再找（不点任何位置，避免选中建筑）
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：缩放后未找到，清弹窗后重试")
        sweepBlockingPopups()
        zoomToCapitalShore()
        entry = findMultiColorsUntil(
            schemas = listOf(MyColors.CapitalClanEntryButton, MyColors.CapitalClanEntryButton2),
            duration = 4_000,
            increment = 1
        )
    }
    if (entry == null) {
        // 最后兜底：强制再走一次回主世界（若实际被场景误判困在夜世界，这一步正好点传送门切回）
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城入口：仍未找到，强制回主世界后重试")
        if (enterMainBase()) {
            if (isInClanCapital()) return true
            sweepBlockingPopups()
            zoomToCapitalShore()
            entry = findMultiColorsUntil(
                schemas = listOf(MyColors.CapitalClanEntryButton, MyColors.CapitalClanEntryButton2),
                duration = 4_000,
                increment = 1
            )
        }
    }
    if (entry == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到都城入口按钮，请确认主世界为默认场景")
        saveCapitalScreenshot("capital_entry_fail")
        return false
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：找到入口 @(${entry.x},${entry.y})，点击进入")
    // 直接点击原命中点：真机验证一击有效（命中点即热气球中部，可点击热区正确）。
    // 之前"命中点上移40px点气球中心"每次都未生效再回落原点，白白多花 11 秒，已移除。
    TouchActions.tap(entry.x, entry.y, delayTime = 1500)
    // 源脚本点击入口后等待，再用 左下角回营/都城界面 特征轮询判断是否已进城
    val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
    var retried = false
    while (System.currentTimeMillis() < deadline) {
        if (isInClanCapital()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：已进入都城")
            return true
        }
        // 8 秒无效果 → 可能点了建筑弹出信息菜单，先点左侧海面空白关闭菜单（海面不会选中建筑），
        // 再重试一次原命中点
        if (!retried && deadline - System.currentTimeMillis() < (timeoutSeconds - 8) * 1000L) {
            retried = true
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：入口点击未生效，关闭可能弹出的建筑菜单后重试")
            TouchActions.tap(300, 620, delayTime = 800)
            TouchActions.tap(entry.x, entry.y, delayTime = 1500)
        }
        delayWithMultiplier(500)
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：进入都城超时")
    saveCapitalScreenshot("capital_enter_timeout")
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

/**
 * Legacy 函数270a: 每个子城有【各自独立】的竖屏星星扫描区间 (位置Nx, y1)-(位置Nx+40, y2)。
 * 映射到横屏后 x = 竖屏 y 区间（下两表），y = 719-位置Nx 的 40px 带。
 * 踩坑（2026-09-27）：此前误写成所有子城统一 x∈[520,725] 的横带，导致三星子城
 * （星星横向分布在 x 664-759）只扫到两颗、3 星被误判为 2 星而去进攻不可打的子城。
 * 源区间与我们镜头还有 ~15px 偏差（第三颗星锚点在 x≈883 越过区间右缘 869），
 * 故两端各放宽 25px；不同子城靠各自的 40px 竖屏 y 带（横屏 x 无关）区分，放宽安全。
 */
private val DISTRICT_SCAN_X1 = intArrayOf(495, 666, 490, 359, 613, 781, 185, 450, 737)
private val DISTRICT_SCAN_X2 = intArrayOf(750, 894, 729, 589, 837, 1014, 435, 705, 955)
private const val DISTRICT_SPAN = 40

/** Legacy 函数269a/270a `ret` == 3: locked or somebody is already attacking (not attackable). */
private const val DISTRICT_UNAVAILABLE = -1

private const val LAND_H = 720

/**
 * Scans the 9 districts and picks one to attack. Port of 函数270a (L75166~L75332):
 *   1. locate the map with the 都城地图锚点 feature (legacy `intX`);
 *   2. per district: 函数269a counts stars (missing white star / blue lock => locked);
 *   3. pick order: 2 stars > 1 star > 0 stars; for 0 stars also ask "somebody is attacking".
 *
 * @param exclude 已尝试过且按钮为灰色（不可打）的子城序号（1-based），选星时跳过。
 * @return the best district, or null when nothing is attackable.
 */
suspend fun pickCapitalTarget(exclude: Set<Int> = emptySet()): CapitalTarget? {
    val anchor = findMultiColors(schema = MyColors.CapitalRaidMapAnchor, increment = 1)
    if (anchor == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未找到都城地图锚点，无法选择目标")
        return null
    }
    // 位置Nx is a portrait x, so the landscape tap y is 719 - 位置Nx; the anchor is already mapped, so
    // the district rows can be derived from anchor.y.
    val states = IntArray(DISTRICT_DX.size) { DISTRICT_UNAVAILABLE }
    for (i in DISTRICT_DX.indices) {
        if ((i + 1) in exclude) continue
        val tapY = anchor.y - DISTRICT_DX[i]
        val y1 = tapY - DISTRICT_SPAN
        if (y1 < 0 || tapY > LAND_H - 1) continue
        // 每个子城用自己的横屏 x 扫描区间（源脚本竖屏 y 区间映射）
        states[i] = readDistrictStars(DISTRICT_SCAN_X1[i], y1, DISTRICT_SCAN_X2[i], tapY)
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
    // 源函数269a 顺序：先白星（未命中=未解锁），再锁定蓝特征
    if (findMultiColors(schema = star, increment = 1) == null) return DISTRICT_UNAVAILABLE
    if (findMultiColors(schema = locked, increment = 1) != null) return DISTRICT_UNAVAILABLE

    // 源函数269a 用 findMultiColorAll 数白星数量 = 已得星数（0/1/2；>=3 即满星不可打）
    val stars = findMultiColorsAll(schema = MyColors.raidStarCount(x1, y1, x2, y2)).size
    // "有人进攻中"检查扩展到所有星数（源脚本只查 0 星，但实测 1-2 星子城在被部落成员
    // 补星时进攻按钮同样置灰，如都城之巅 32% 进度期间，2026-09-27）：
    // 提前判不可打，省去点击后才发现按钮为灰的一轮往返。
    val occupied1 = MyColors.raidOccupied(x1, y1, x2, y2)
    if (findMultiColors(schema = occupied1, increment = 1) != null) return DISTRICT_UNAVAILABLE
    val occupied2 = MyColors.raidOccupied2(x1, y1, x2, y2)
    if (findMultiColors(schema = occupied2, increment = 1) != null) return DISTRICT_UNAVAILABLE
    return stars
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
/** startCapitalBattle 的三种结果。 */
enum class CapitalBattleStart {
    /** 已进入战斗（放弃按钮出现）。 */
    STARTED,
    /** 已到达"要立即进攻吗？"确认界面（进攻前终点，未点进攻）。 */
    CONFIRM_REACHED,
    /** 该子城不可打（按钮条"进攻"按钮为灰色，已三星等），需换下一个子城。 */
    DISTRICT_UNUSABLE,
}

suspend fun startCapitalBattle(target: CapitalTarget): CapitalBattleStart {
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：进攻子城${target.mapIndex}（${target.stars}星）")
    // 流程：tap 子城 → 等底部白色按钮条弹出 → 判"进攻"按钮颜色：
    //   灰色 = 该子城已三星/不可打（用户 2026-09-27 指出）→ 返回键关闭后换下一个子城；
    //   非灰 = 可打 → 点进攻打开"要立即进攻吗？"确认界面 = 进攻前终点（按约定不点进攻）。
    TouchActions.tap(target.x, target.y, delayTime = 800)
    repeat(3) { attempt ->
        if (attempt > 0) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：底部按钮条未出现，重新点击子城（第${attempt + 1}次）")
            TouchActions.tap(target.x, target.y, delayTime = 1500)
        }
        // 1. 等底部白色按钮条弹出（最多 6 秒）
        val bar = findMultiColorsUntil(
            schemas = listOf(MyColors.CapitalDistrictBar),
            duration = 6_000,
            increment = 1
        )
        if (bar == null) return@repeat
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：底部按钮条已出现 @(${bar.x},${bar.y})")
        // 2. "进攻"按钮灰色 = 子城不可打（已三星或正被部落成员进攻）→ 点空白处关按钮条
        //    （返回键会退出整个都城地图视图导致地图锚点丢失，2026-09-27 踩坑），让调用方换子城
        if (findMultiColors(schema = MyColors.CapitalDistrictAttackGray, increment = 1) != null) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：子城${target.mapIndex}进攻按钮为灰色（已三星或正被部落成员进攻），换下一个子城")
            saveCapitalScreenshot("capital_district_gray")
            TouchActions.tap(640, 180, delayTime = 800)
            return CapitalBattleStart.DISTRICT_UNUSABLE
        }
        // 3. 非灰 = 可打 → 点击底部进攻按钮打开确认界面（源脚本 taps(78,754) → 横屏 (754,641)）
        TouchActions.tap(BOTTOM_ATTACK_BUTTON[0], BOTTOM_ATTACK_BUTTON[1], delayTime = 800)
        // The "storage limit reached" popup covers the battlefield (legacy L94633 taps it away).
        findMultiColors(schema = MyColors.CapitalRaidStorageFull, increment = 1)?.let {
            TouchActions.tap(it.x, it.y, delayTime = 1000)
        }
        // 4. 轮询 15 秒：先检测"要立即进攻吗？"确认弹窗，再检测放弃按钮。
        val deadline = System.currentTimeMillis() + 15_000L
        while (System.currentTimeMillis() < deadline) {
            if (findMultiColors(schema = MyColors.CapitalAttackConfirm, increment = 1) != null) {
                // 到达确认弹窗 = 进攻前流程终点。按约定【不点进攻按钮】，识别按钮状态后返回键关闭。
                val ready = findMultiColors(schema = MyColors.CapitalAttackConfirmReady, increment = 1) != null
                val gray = findMultiColors(schema = MyColors.CapitalAttackConfirmGray, increment = 1) != null
                val state = when {
                    ready -> "进攻按钮可用（部队已满）"
                    gray -> "进攻按钮灰色（部队未满，不可进攻）"
                    else -> "进攻按钮状态未识别"
                }
                ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：已到达开始进攻确认界面（$state），按约定不点进攻，按返回键关闭")
                saveCapitalScreenshot("capital_attack_confirm")
                pressBack()
                delayWithMultiplier(1200)
                return CapitalBattleStart.CONFIRM_REACHED
            }
            if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) != null) return CapitalBattleStart.STARTED
            delayWithMultiplier(500)
        }
    }
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未进入战斗（重试3次未出现确认弹窗/放弃按钮），按返回键关闭")
    saveCapitalScreenshot("capital_battle_not_started")
    pressBack()
    delayWithMultiplier(1200)
    return CapitalBattleStart.DISTRICT_UNUSABLE
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

/**
 * 用返回键关闭"既没有红 x、也不是通用对话框"的游戏内弹窗（例如"要立即进攻吗？"确认框）。
 * 通过 root shell 发 keyevent，失败时静默忽略。
 */
private fun pressBack() {
    runCatching { Shell.cmd("input keyevent 4").exec() }
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
    // 关键节点截图存档：每号每周仅 5 次突袭机会，进场即留存
    saveCapitalScreenshot("capital_enter")

    // M4-③ §9.7(5)：攻打前若开启「自动配兵」，先在突袭地图层补齐都城军队（源脚本「首次造兵」逻辑）。
    // prepareCapitalArmy 内部会点「编辑都城军队」再按等级配兵；军队已满时函数会自动空转返回，无需额外判定。
    // 注意：坐标/面板判定仍待真机验证（离线无都城战斗截图），故默认关闭，由配置开关控制。
    if (getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.TRAIN_CAPITAL_ARMY.key)) {
        val level = runCatching {
            getConfigRuntime(Schema.MAIN_BASE_SETTINGS.CAPITAL_HALL_LEVEL.key).trim().toIntOrNull() ?: 10
        }.getOrDefault(10)
        prepareCapitalArmy(level)
    }

    // 依次尝试子城：星数初筛选出的子城若"进攻"按钮为灰色（已三星/不可打），返回键换下一个
    val tried = mutableSetOf<Int>()
    var started = CapitalBattleStart.DISTRICT_UNUSABLE
    var battleTarget: CapitalTarget? = null
    while (true) {
        // 换子城重选时镜头可能已偏离（点子城/关按钮条会平移镜头）→ 重开都城地图恢复锚点
        var candidate = pickCapitalTarget(tried)
        if (candidate == null && tried.isNotEmpty()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：地图锚点丢失，重开都城地图")
            if (!openCapitalRaidMap()) break
            candidate = pickCapitalTarget(tried) ?: break
        }
        if (candidate == null) break
        saveCapitalScreenshot("capital_target")
        started = startCapitalBattle(candidate)
        if (started != CapitalBattleStart.DISTRICT_UNUSABLE) {
            battleTarget = candidate
            break
        }
        tried.add(candidate.mapIndex)
        if (tried.size >= DISTRICT_DX.size) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：所有子城均不可打（已三星/锁定/进攻中）")
            break
        }
    }
    if (started != CapitalBattleStart.STARTED || battleTarget == null) return false

    // Deploy (M4-②: terrain scan + 12 troops / 5 spells)，把本次子城目标坐标交给第二轮做 9 地图定位补点
    val deployed = capitalDeployArmy(battleTarget)
    if (!deployed) ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：本场没有放出任何兵/法术")

    finishCapitalBattle()
    return true
}
