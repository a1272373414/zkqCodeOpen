package com.coc.zkqcode.jar.code.builderbase.others

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.core.util.touchactions.TouchActions.pinchIn
import com.coc.zkqcode.core.util.touchactions.TouchActions.swipe
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.buildings.BaseType
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.buildings.WorkerAndResearch
import com.coc.zkqcode.jar.code.universal.clickRightBottom
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.checkMemoryFile
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory

suspend fun clickOttosOutPost(): Boolean {
    ShowMessage.run("夜世界：开始检测奥仔哨站")
    val storageKey = StorageKeys.withAccountNumber(StorageKeys.CLICK_OTTOS_POST, InGamesVars.currentAccountNumber)

    // Skip if already checked within 24 hours
    if (!checkMemoryFile(storageKey, 1440)) {
        ShowMessage.run("账号${InGamesVars.currentAccountNumber}，今日已检测奥仔哨站，暂不点击")
        return true
    }
    ShowMessage.run("账号${InGamesVars.currentAccountNumber}，准备检测奥仔哨站")
    val worker = WorkerAndResearch.detectWorkerNumber(BaseType.Builder)
    if (worker.total < 2) {
        ShowMessage.run("账号${InGamesVars.currentAccountNumber}，工人不足2，跳过奥仔哨站")
        writeMemory(storageKey, (System.currentTimeMillis() / 60_000).toString())
        return true
    }
    ShowMessage.run("账号${InGamesVars.currentAccountNumber}，工人=${worker.total}，开始滑动检测奥仔哨站箭头")
    pinchIn(141, 423, 1052, 352, 638, 365)
    delayWithMultiplier(200)
    repeat(3) {
        swipe(269, 139, 1021, 502, delayTime = 100)
    }
    swipe(269, 139, 1021, 502, delayTime = 800)
    repeat(5) {
        val arrow = findMultiColorsUntil(schemas = listOf(MyColors.OrangeTutorialArrow), duration = 500)
        if (arrow != null) {
            ShowMessage.run("账号${InGamesVars.currentAccountNumber}，发现奥仔哨站箭头，点击")
            TouchActions.tap(arrow.x + 20, arrow.y + 100)
            delayWithMultiplier(10)
            clickRightBottom(1)
            delayWithMultiplier(50)
        }
    }
    ShowMessage.run("账号${InGamesVars.currentAccountNumber}，奥仔哨站检测完成")
    writeMemory(storageKey, (System.currentTimeMillis() / 60_000).toString())
    return enterMainScreen()
}