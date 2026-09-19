package com.coc.zkqcode.jar.code.mainbase.herohall

import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.core.util.touchactions.TouchActions.swipe
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.mainbase.others.zoomSmallMainBase
import com.coc.zkqcode.jar.code.universal.clickRightBottom
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.yolo.tiledYoloDetect

/**
 * Pan steps applied between two detection passes, each one is (fromX, fromY, toX, toY).
 *
 * The hero hall is not at a fixed place: depending on the base layout it can be off-screen
 * at the default camera position, so after zooming out we walk this pattern and re-scan at
 * every stop.
 */
private val HERO_HALL_PAN_STEPS = listOf(
    intArrayOf(900, 130, 0, 720),    // move the map down-left
    intArrayOf(200, 620, 1000, 120), // move the map up-right
    intArrayOf(1000, 400, 200, 400), // move the map left
    intArrayOf(300, 400, 1050, 400), // move the map right
    intArrayOf(700, 620, 700, 100)   // move the map up
)

/**
 * Runs [action] with the hero hall open, then returns to the main screen.
 *
 * The hero hall may sit inside a dense cluster of buildings and be partially occluded, so a
 * fixed screen position cannot be used to find it. Instead we zoom the map out (see
 * [zoomSmallMainBase]) and then pan around, re-running the building detector at every stop.
 */
suspend fun withHeroHall(action: suspend () -> Unit): Boolean {
    clickRightBottom(1)
    zoomSmallMainBase()

    if (scanAndTapHeroHall()) {
        action()
        return enterMainScreen()
    }

    // Not visible at the default camera position: pan around and re-scan.
    for (step in HERO_HALL_PAN_STEPS) {
        swipe(step[0], step[1], step[2], step[3])
        delayWithMultiplier(200)
        if (scanAndTapHeroHall()) {
            action()
            return enterMainScreen()
        }
    }

    return enterMainScreen()
}

/**
 * Runs one detection pass and tries every candidate box until the hero hall really opens.
 *
 * Inside a building cluster the detector can return neighbouring buildings as well, so every
 * box is verified by tapping it and looking for the hero hall marker. When a wrong building
 * was opened the panel has to be closed again, otherwise the next tap would land on the panel
 * instead of the map.
 */
private suspend fun scanAndTapHeroHall(): Boolean {
    val scan = tiledYoloDetect(
        modelName = "building-detect", callerTag = "FindHeroHall", classIndex = 3
    )
    for (detection in scan) {
        val box = detection.boundingBox
        if (openHeroHall(box.centerX().toInt(), box.centerY().toInt())) return true
        // A neighbouring building was opened instead (dense cluster / partial occlusion).
        // Close whatever popped up before trying the next candidate.
        clickRightBottom(1)
        delayWithMultiplier(150)
    }
    return false
}

suspend fun openHeroHall(x: Int, y: Int): Boolean {
    TouchActions.tap(x, y, delayTime = 300)
    val openHeroHallIcon = findMultiColorsUntil(
        schemas = listOf(MyColors.OpenHeroHall), duration = 500
    )
    if (openHeroHallIcon != null) {
        TouchActions.tap(openHeroHallIcon.x, openHeroHallIcon.y, delayTime = 600)
        return true
    }
    return false
}
