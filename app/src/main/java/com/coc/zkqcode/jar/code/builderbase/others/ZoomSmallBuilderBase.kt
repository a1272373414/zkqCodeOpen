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
        // 同样取 180：120 时移动仅 60ms，会被判成"点击"而选中地图上的建筑
        swipe(981, 485, 0, 0, delayTime = 180)
    }
    delayWithMultiplier(200)
    // 注意：swipe 是"按下 → 停顿 (delayTime×0.7×倍率) → 移动"。
    //   · delayTime 太大（如 800 → 停顿 560ms）会被判成"长按拖动建筑"；
    //   · 太小（如 120 → 移动仅 60ms）又会被判成"点击"，点在地图上会选中建筑弹"信息"面板。
    // 取 180（停顿 ~126ms、移动 ~90ms）落在两者之间。
    swipe(981, 485, 519, 278, delayTime = 180)
    if (isForBuild) {
        ShowMessage("夜世界：缩小完成，进入建造摆放")
        clickRightBottom(1)
        swipe(575, 430, 575, 710, delayTime = 180)
    }
    delayWithMultiplier(200)
}