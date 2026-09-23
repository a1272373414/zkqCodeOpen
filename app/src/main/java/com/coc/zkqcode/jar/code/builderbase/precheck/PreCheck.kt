package com.coc.zkqcode.jar.code.builderbase.precheck

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema


    suspend fun claimAchievement(): Boolean {
        ShowMessage("夜世界：开始检测成就奖励")
        val isClaimAchievement = getBooleanConfigRuntime(
            Schema.MAIN_BASE_SETTINGS.CLAIM_ACHIEVEMENT_GEMS.key
        )
        if (isClaimAchievement) {
            ShowMessage("夜世界：已开启领取成就，开始检测")
            val point = findMultiColors(schema = MyColors.Achievement)
            if (point != null) {
                ShowMessage("夜世界：发现可领取成就，领取中")
                claimAchievementHelper()
                if (!enterMainScreen()) return false
            } else {
                ShowMessage("夜世界：当前无可领取成就")
            }
        } else {
            ShowMessage("夜世界：未开启领取成就，跳过")
        }
        ShowMessage("夜世界：成就检测完成")
        return true
    }

    private suspend fun claimAchievementHelper() {
        TouchActions.tap(51, 45, delayTime = 2000)
        val point = findMultiColorsUntil(schemas = listOf(MyColors.ClaimAchievement), duration = 200)
        if (point != null)
            TouchActions.tap(point.x, point.y)
    }
