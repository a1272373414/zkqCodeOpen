package com.coc.zkqcode.jar.code.mainbase.upgrade

import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.buildings.upgrade.BuildButtonType
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

suspend fun mainBaseFindBuildButton(duration: Int = 500, type: BuildButtonType): Point? {
    val startTime = System.currentTimeMillis()
    val targetSchemas = when (type) {
        BuildButtonType.Tick -> listOf(MyColors.MainBaseBuildTick1, MyColors.MainBaseBuildTick2)
        BuildButtonType.Cross -> listOf(MyColors.MainBaseBuildCross1, MyColors.MainBaseBuildCross2)
    }

    while (System.currentTimeMillis() - startTime < duration) {
        val screenBuffer = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult ?: logAndRestart("failed to take screenshot at close advertisement")
        for (schema in targetSchemas) {
            val point = findMultiColors(schema = schema, byteBuffer = screenBuffer)
            if (point != null) return point
        }
        delayWithMultiplier(20)
    }
    return null
}

