package com.coc.zkqcode.jar.code.mainbase.attack

import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.checkMemoryFile
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory
import com.coc.zkqcode.jar.code.mainbase.MainBaseArmyRecognizer
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.GameScene
import com.coc.zkqcode.jar.code.universal.detectCurrentScene
import com.coc.zkqcode.jar.code.universal.waitForScene
import com.coc.zkqcode.jar.ui.schema.Schema

/**
 * Training-page category tabs, calibrated on-device (emulator-5556, 1280x720) on
 * 2026-09-20. Each coordinate taps the corresponding queue row / tab slot to open its
 * selection panel; the switch is verified by the category's signature troop schema
 * (see [switchTrainingTab]). Reused from the legacy freescript idea of a `兵种显示`
 * state machine: we track which tab we are on and verify the switch by checking the
 * category's signature troop schema.
 *
 * Confirmed working: troops(891,234) / spells(797,420) / siege(1126,423) open their
 * panels; (219,139) closes the panel. The trash buttons (DeleteAll1/2/3) only render
 * when that queue is non-empty, so a NOT-FOUND there simply means the queue is empty.
 */
private val TAB_TROOPS = 891 to 234
private val TAB_SPELLS = 797 to 420
private val TAB_SIEGE = 1126 to 423

/**
 * Taps a training-page category tab and verifies the switch succeeded by looking for
 * the category's signature schema. On failure it logs a warning (observability) instead
 * of silently mis-clicking; the caller decides whether to continue.
 *
 * @return true if the expected category became visible.
 */
private suspend fun switchTrainingTab(
    x: Int, y: Int, verifySchema: ColorSchema?, tabName: String
): Boolean {
    TouchActions.tap(x, y, delayTime = 800)
    if (verifySchema == null) return true
    val ok = findMultiColorsUntil(schemas = listOf(verifySchema), duration = 800) != null
    if (!ok) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，切换分页【$tabName】可能失败（未找到特征）")
    }
    return ok
}

suspend fun mainBaseTrainTroops(): Boolean {
    val isAttackEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.AUTO_ATTACK.key)
    val isManualTrainEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.MANUAL_TRAINING.key)
    if (!isAttackEnabled || isManualTrainEnabled) return true
    val storageKey = StorageKeys.withAccountNumber(StorageKeys.MAIN_BASE_TRAIN_TROOPS, InGamesVars.currentAccountNumber)

    if (checkMemoryFile(storageKey, 1440)) {
    ShowMessage("账号${InGamesVars.currentAccountNumber}，准备训练部队")
    GlobalVars.absorbEdge = 1

    // Phase 1 (reused from legacy freescript): log where we are before acting, so the
    // user always knows the current page / flow node instead of guessing.
    SceneState.setScene(detectCurrentScene())
    SceneState.setFlowNode("练兵-开始")

    // Open training menu
    var point = findMultiColorsUntil(schemas = listOf(MyColors.TrainTroops), duration = 1500)
    if (point != null) {
        TouchActions.tap(point.x, point.y, delayTime = 500)
    } else {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，训练部队失败")
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    }

    // Verify training page (unified wait loop: sweeps popups + logs the scene each tick).
    SceneState.setFlowNode("练兵-打开训练页")
    if (!waitForScene(GameScene.TRAINING_PAGE, 5)) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，未找到训练标志")
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    }


        // Clean Queue 1
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll1), duration = 1000)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Train Troops Tab (圣水兵) — verify the colored barbarian card appears.
        SceneState.setFlowNode("练兵-圣水兵")
        switchTrainingTab(TAB_TROOPS.first, TAB_TROOPS.second, MyColors.TrainBarbarian, "圣水兵")

        // 兵种识别接入：识别当前部队配置（14 槽位）中的兵种并回报，用于核对/日志
        runCatching {
            val army = MainBaseArmyRecognizer.recognizeTroopNames()
            if (army.isNotEmpty()) {
                ShowMessage("账号${InGamesVars.currentAccountNumber}，当前部队：${army.joinToString("、")}")
            }
        }

        for (i in 1..8) {
            // Priority training check
            val dragonPoint = findMultiColorsUntil(schemas = listOf(MyColors.TrainDragon, MyColors.TrainDragon2), duration = 100)
            if (dragonPoint != null) {
                repeat(25) { TouchActions.tap(dragonPoint.x, dragonPoint.y, delayTime = 40) }
                break
            }
            findMultiColors(schema = MyColors.TrainGiant)?.let { p ->
                repeat(5) { TouchActions.tap(p.x, p.y, delayTime = 40) }
            }
            findMultiColors(schema = MyColors.TrainArcher)?.let { p ->
                repeat(40) { TouchActions.tap(p.x, p.y, delayTime = 40) }
            }
            findMultiColors(schema = MyColors.TrainBarbarian)?.let { p ->
                repeat(40) { TouchActions.tap(p.x, p.y, delayTime = 40) }
            }

            if (findMultiColors(schema = MyColors.GrayBarbarian) != null) break
            delayWithMultiplier(300)
        }

        // Close tab and Clean Queue 2
        TouchActions.tap(219, 139, delayTime = 1000)
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll2), duration = 500)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Spell Tab (法术) — verify the lightning spell card appears.
        SceneState.setFlowNode("练兵-法术")
        switchTrainingTab(TAB_SPELLS.first, TAB_SPELLS.second, MyColors.TrainLighteningSpell, "法术")
        if (findMultiColorsUntil(schemas = listOf(MyColors.TrainLighteningSpell), duration = 500) != null) {
            // Optimized sequence of taps for lightning spells
            val spellCoords = listOf(351 to 621, 351 to 621, 351 to 621, 220 to 499, 91 to 494, 91 to 494, 91 to 494, 91 to 494)
            for (coord in spellCoords) {
                TouchActions.tap(coord.first, coord.second, delayTime = 50)
            }
        }

        // Close tab and Clean Queue 3
        TouchActions.tap(219, 139, delayTime = 1000)
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll3), duration = 500)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Siege Machines Tab (攻城机器) — verify the siege machine card appears.
        SceneState.setFlowNode("练兵-攻城机器")
        switchTrainingTab(TAB_SIEGE.first, TAB_SIEGE.second, MyColors.TrainSiegeMachine, "攻城机器")
        if (findMultiColorsUntil(schemas = listOf(MyColors.TrainSiegeMachine), duration = 500) != null) {
            val siegeCoords = listOf(1047 to 543, 610 to 535, 364 to 536, 138 to 541)
            for (coord in siegeCoords) {
                TouchActions.tap(coord.first, coord.second, delayTime = 50)
            }
        }

        // Final Close
        TouchActions.tap(219, 139, delayTime = 1000)
        TouchActions.tap(1232, 65, delayTime = 300)

        writeMemory(storageKey, (System.currentTimeMillis() / 60_000).toString())
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    } else {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，该账号今日已练兵")
        return true
    }
}