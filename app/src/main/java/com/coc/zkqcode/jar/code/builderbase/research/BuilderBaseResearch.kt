package com.coc.zkqcode.jar.code.builderbase.research

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.universal.buildings.BaseType
import com.coc.zkqcode.jar.code.universal.buildings.WorkerAndResearch
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.clickRightBottom
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema
import com.coc.zkqcode.jar.ui.schema.details.BuilderBaseTroops


suspend fun builderBaseResearch(): Boolean {
    ShowMessage.run("夜世界：开始研究兵种")
    if (getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.BUILDER_BASE_RESEARCH.key) && WorkerAndResearch.detectResearch(BaseType.Builder)) {
        ShowMessage.run("夜世界：已开启研究且检测到研究建筑，查找研究图标")
        val research = findMultiColors(schema = MyColors.ResearchIcon, increment = 1)
            ?: findMultiColors(schema = MyColors.ResearchIcon2, increment = 1)
        if (research != null) {
            ShowMessage("夜世界：找到研究图标，进入研究页")
            TouchActions.tap(research.x, research.y, delayTime = 600)
            TouchActions.tap(research.x, research.y + 130)//Open research tab
            val backArrow = findMultiColorsUntil(schemas = listOf(MyColors.BuilderResearchBackArrow), duration = 1000, increment = 1)
            if (backArrow != null) {
                ShowMessage("夜世界：研究页打开，开始检查可研究项")
                TouchActions.tap(backArrow.x, backArrow.y, delayTime = 600)
                builderBaseCheckAllResearch()
            } else {
                ShowMessage("夜世界：未找到研究返回箭头，跳过")
            }
        } else {
            ShowMessage("夜世界：未找到研究图标，跳过")
        }
    } else {
        ShowMessage("夜世界：未开启研究或无可研究建筑，跳过")
    }
    ShowMessage.run("夜世界：研究流程完成")
    return enterMainScreen()
}

suspend fun builderBaseCheckAllResearch() {
    for (i in BuilderBaseTroops.all.indices) {
        val troop = BuilderBaseTroops.all[i]
        if (getBooleanConfigRuntime(troop.key)) {
            ShowMessage.run("夜世界：检查兵种 ${troop.displayName}")
            val row = i / 6
            val col = i % 6

            val x1 = 310 + col * 140
            val y1 = if (row == 0) 460 else 600
            val x2 = x1 + 40
            val y2 = if (row == 0) 500 else 640

            val elixirIcon = findMultiColors(schema = ColorSchema.rescope(MyColors.BuilderResearchElixir, x1, y1, x2, y2), increment = 1)
            if (elixirIcon != null) {
                val resX1 = x1 - 100
                val insufficient = findMultiColors(schema = ColorSchema.rescope(MyColors.BuilderResearchInsufficientResources, resX1, y1, x1, y2), increment = 1)
                if (insufficient == null) {
                    ShowMessage("账号${InGamesVars.currentAccountNumber}，开始研究 ${troop.displayName}")
                    TouchActions.tap(elixirIcon.x, elixirIcon.y, delayTime = 500)
                    TouchActions.tap(955, 610, delayTime = 200)
                    clickRightBottom(2)
                    return
                } else {
                    ShowMessage.run("账号${InGamesVars.currentAccountNumber}，${troop.displayName} 资源不足")
                }
            }
        }
    }
    ShowMessage("夜世界：可研究项检查完毕")
}


