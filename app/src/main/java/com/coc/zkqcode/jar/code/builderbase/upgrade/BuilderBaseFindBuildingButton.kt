package com.coc.zkqcode.jar.code.builderbase.upgrade

import android.graphics.Point
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.buildings.upgrade.BuildButtonType
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

suspend fun builderBaseFindBuildButton(duration: Int = 500, type: BuildButtonType): Point? {
    val startTime = System.currentTimeMillis()
    val targetSchemas = when (type) {
        BuildButtonType.Tick -> listOf(
            MyColors.BuilderBaseBuildTick1, MyColors.BuilderBaseBuildTick2, MyColors.BuilderBaseBuildTick3,
            MyColors.BuilderBaseBuildTick4, MyColors.BuilderBaseBuildTick5, MyColors.BuilderBaseBuildTick6,
            MyColors.BuilderBaseBuildTick7, MyColors.BuilderBaseBuildTick8, MyColors.BuilderBaseBuildTick9,
            MyColors.BuilderBaseBuildTick10
        )
        BuildButtonType.Cross -> listOf(
            MyColors.BuilderBaseBuildCross1, MyColors.BuilderBaseBuildCross2, MyColors.BuilderBaseBuildCross3,
            MyColors.BuilderBaseBuildCross4, MyColors.BuilderBaseBuildCross5, MyColors.BuilderBaseBuildCross6,
            MyColors.BuilderBaseBuildCross7, MyColors.BuilderBaseBuildCross8, MyColors.BuilderBaseBuildCross9,
            MyColors.BuilderBaseBuildCross10
        )
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

