package com.coc.zkqcode.jar.code.universal.remove

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.yolo.DetectionResult
import com.coc.zkqcode.jar.code.universal.yolo.tiledYoloDetect
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.GameVersion
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil

suspend fun enterEditMode() {
    // Select the initial schema based on the game package version
    ShowMessage("清障：进入编辑模式")
    val initialSchema = if (InGamesVars.currentGameVersion == GameVersion.CN) {
        MyColors.CNEditBaseButton
    } else {
        MyColors.GlobalEditBaseButton
    }

    // Attempt to locate the initial edit button
    findMultiColorsUntil(schemas = listOf(initialSchema), duration = 1500)?.let {
        TouchActions.tap(it.x, it.y)
        ShowMessage("清障：已点击编辑按钮")
    } ?: return

    // Sequence of interactions to navigate through the edit menus
    // 1. Locate and click the specific Green Edit Button
    findMultiColorsUntil(schemas = listOf(MyColors.GreenEditBaseButton), duration = 1500)?.let {
        TouchActions.tap(it.x, it.y)
    }

    removeAllBuildings()
    ShowMessage("清障：编辑模式已进入，建筑已移出")
}

suspend fun removeAllBuildings() {
    ShowMessage("清障：开始移除全部建筑")
    findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes, MyColors.MiddleGreenConfirm), duration = 300)?.let { yesPoint ->
        TouchActions.tap(yesPoint.x, yesPoint.y)
    }
    // Locate "Remove All", confirm the action, and perform final layout taps
    findMultiColorsUntil(schemas = listOf(MyColors.EditModeRemoveAll, MyColors.EditModeRemoveAll2), duration = 1000)?.let {
        TouchActions.tap(it.x, it.y)
        ShowMessage("清障：已点击移除全部")

        // Re-confirm deletion
        findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 200)?.let { yesPoint ->
            TouchActions.tap(yesPoint.x, yesPoint.y)
        }

        // Post-action delays and fixed-coordinate taps to finalize state
        delayWithMultiplier(500)
        TouchActions.tap(1005, 265, delayTime = 500)
    }

}

suspend fun removeObstacles() {
    delayWithMultiplier(200)
    ShowMessage("清障：开始检测障碍物（YOLO remove-obstacle）")
    val obstacles = tiledYoloDetect(
        modelName = "remove-obstacle", callerTag = "BuilderBaseRemoveObstacles"
    )
    if (obstacles.isEmpty()) {
        ShowMessage("清障：未检测到障碍物（模型未加载或本屏无可移除障碍）")
    }
    obstacles.forEach { obstacle ->
        val box = obstacle.boundingBox
        val centerX = box.centerX().toInt()
        val centerY = box.centerY().toInt()
        ShowMessage("x: $centerX, y: $centerY")
        TouchActions.tap(centerX, centerY, delayTime = 120)
        // Tap confirmation/action button
        TouchActions.tap(616, 488, delayTime = 100)
        repeat(2) {
            TouchActions.tap(14, 558, delayTime = 100)
        }
    }
}