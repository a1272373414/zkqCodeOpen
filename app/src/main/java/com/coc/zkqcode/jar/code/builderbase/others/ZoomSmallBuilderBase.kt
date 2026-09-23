package com.coc.zkqcode.jar.code.builderbase.others

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchIn
import com.coc.zkqcode.core.util.touchactions.TouchActions.swipe
import com.coc.zkqcode.jar.code.universal.clickRightBottom

suspend fun zoomSmallBuilderBase(isForBuild: Boolean = false) {
    ShowMessage("夜世界：缩小基地地图${if (isForBuild) "（建造模式）" else ""}")
    pinchIn(141, 423, 1052, 352, 638, 365)
    delayWithMultiplier(200)
    repeat(2) {
        swipe(981, 485, 0, 0, delayTime = 120)
    }
    delayWithMultiplier(200)
    swipe(981, 485, 519, 278, delayTime = 800)
    if (isForBuild) {
        ShowMessage("夜世界：缩小完成，进入建造摆放")
        clickRightBottom(1)
        swipe(575, 430, 575, 710, 800)
    }
    delayWithMultiplier(200)
}