package com.coc.zkqcode.jar.code.mainbase.others

import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchIn
import com.coc.zkqcode.core.util.touchactions.TouchActions.swipe
import com.coc.zkqcode.jar.code.universal.clickRightBottom

suspend fun zoomSmallMainBase(isForBuild: Boolean = false, isForAttack: Boolean = false) {
    clickRightBottom(1)
    // 这些 swipe 的起点都落在村庄建筑上。swipe 是"按下 → 停顿 (delayTime×0.7×倍率) → 移动"：
    //   · 停顿过长（默认 210~280ms）→ 被判"长按拖动建筑"；
    //   · 停顿过短（120 → 移动仅 60ms）→ 被判"点击"，点到建筑会弹"信息"面板。
    // 统一取 180（停顿 ~126ms、移动 ~90ms），落在两者之间。
    swipe(200, 500, 950, -500, delayTime = 180)
    clickRightBottom(1)
    delayWithMultiplier(50)
    pinchIn(141, 423, 1052, 352, 638, 365)
    delayWithMultiplier(200)
    if (isForAttack) {
        // Pan the map all the way to a fixed extreme edge so the deploy boundary line lands at a
        // deterministic screen position (fixed deploy coordinates rely on this). Extra swipes are
        // harmless once the map has reached its limit.
        repeat(4) {
            swipe(911, 134, 0, 720, delayTime = 180)
        }
    } else {
        swipe(218, 523, 939, 162, delayTime = 180)
    }
    if (isForBuild) {
        delayWithMultiplier(200)
        clickRightBottom(1)
        swipe(690, 550, 590, 710, delayTime = 180)
    }
}