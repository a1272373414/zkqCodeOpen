package com.coc.zkqcode.jar.code.builderbase

import com.coc.zkqcode.jar.code.builderbase.attack.builderBaseAttack
import com.coc.zkqcode.jar.code.builderbase.attack.builderBaseTrainWithConditions
import com.coc.zkqcode.jar.code.builderbase.others.builderBaseRemoveObstacles
import com.coc.zkqcode.jar.code.builderbase.others.clickOttosOutPost
import com.coc.zkqcode.jar.code.builderbase.precheck.claimAchievement
import com.coc.zkqcode.jar.code.builderbase.research.builderBaseResearch
import com.coc.zkqcode.jar.code.builderbase.resources.collectBuilderBaseResources
import com.coc.zkqcode.jar.code.universal.buildings.BaseType
import com.coc.zkqcode.jar.code.universal.buildings.upgrade.upgradeBuildings
import com.coc.zkqcode.jar.code.universal.buildings.walls.upgradeWalls
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.jar.code.universal.smalltools.enterBuilderBase
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.ui.schema.Schema

suspend fun playBuilderBase(): Boolean {
    ShowMessage.run("夜世界：开始执行 playBuilderBase")
    if (!claimAchievement()) {
        ShowMessage.run("夜世界：领取成就失败，结束流程")
        return false
    }
    val noBuilderBase = getBooleanConfigRuntime(Schema.BUILDER_BASE_SETTINGS.NO_BUILDER_BASE.key)
    if (noBuilderBase) {
        ShowMessage.run("夜世界：未开启夜世界，跳过")
        return true//if no builder base, then directly return.
    }
    if (!enterBuilderBase(true)) {
        ShowMessage.run("夜世界：进入夜世界失败，跳过")
        return true
    }
    ShowMessage.run("夜世界：开始收集资源")
    if (!collectBuilderBaseResources()) return false
    if (!enterBuilderBase(false)) return true
    ShowMessage.run("夜世界：检测奥仔哨站")
    if (!clickOttosOutPost()) return false
    ShowMessage.run("夜世界：开始移除障碍物")
    if (!builderBaseRemoveObstacles()) return false
    if (!enterBuilderBase(false)) return true
    ShowMessage.run("夜世界：升级城墙")
    if (!upgradeWalls(BaseType.Builder)) return false
    ShowMessage.run("夜世界：升级建筑")
    if (!upgradeBuildings(BaseType.Builder)) return false
    ShowMessage.run("夜世界：研究兵种")
    if (!builderBaseResearch()) return false
    if (!enterBuilderBase(false)) return true
    ShowMessage.run("夜世界：练兵")
    if (!builderBaseTrainWithConditions()) return false
    ShowMessage.run("夜世界：开始对战")
    if (!builderBaseAttack()) return false
    ShowMessage.run("夜世界：playBuilderBase 执行完成")
    return true
}