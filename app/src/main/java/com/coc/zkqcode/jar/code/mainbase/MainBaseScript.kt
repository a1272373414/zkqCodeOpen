package com.coc.zkqcode.jar.code.mainbase


import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.jar.code.mainbase.attack.mainBaseAttack
import com.coc.zkqcode.jar.code.mainbase.attack.mainBaseTrainTroops
import com.coc.zkqcode.jar.code.mainbase.clan.joinClan
import com.coc.zkqcode.jar.code.mainbase.clan.donateToClan
import com.coc.zkqcode.jar.code.mainbase.clan.requestReinforcements
import com.coc.zkqcode.jar.code.mainbase.herohall.placeHeroBanners
import com.coc.zkqcode.jar.code.mainbase.others.mainBaseCheckTutorials
import com.coc.zkqcode.jar.code.mainbase.others.mainBaseRemoveObstacles
import com.coc.zkqcode.jar.code.mainbase.others.zoomSmallMainBase
import com.coc.zkqcode.jar.code.mainbase.research.mainBaseResearch
import com.coc.zkqcode.jar.code.universal.buildings.BaseType
import com.coc.zkqcode.jar.code.universal.buildings.upgrade.upgradeBuildings
import com.coc.zkqcode.jar.code.universal.buildings.walls.upgradeWalls
import com.coc.zkqcode.jar.code.universal.clickRightBottom
import com.coc.zkqcode.jar.code.universal.smalltools.enterMainBase
import com.coc.zkqcode.jar.code.universal.SceneState

suspend fun playMainBase(): Boolean {
    SceneState.setFlowNode("进入主世界")
    if (!enterMainBase()) return false

    SceneState.setFlowNode("缩小主世界")
    zoomSmallMainBase()
    SceneState.setFlowNode("训练部队")
    if (!mainBaseTrainTroops()) return false
    SceneState.setFlowNode("进攻搜索")
    if (!mainBaseAttack()) return false
    SceneState.setFlowNode("清理障碍物")
    if (!mainBaseRemoveObstacles()) return false
    SceneState.setFlowNode("加入部落")
    if (!joinClan()) return false
    SceneState.setFlowNode("请求援军")
    if (!requestReinforcements()) return false
    SceneState.setFlowNode("捐赠部落")
    if (!donateToClan()) return false
    SceneState.setFlowNode("升级城墙")
    if (!upgradeWalls(BaseType.Main)) return false
    SceneState.setFlowNode("升级建筑")
    if (!upgradeBuildings(BaseType.Main)) return false
    SceneState.setFlowNode("实验室研究")
    if (!mainBaseResearch()) return false
    SceneState.setFlowNode("放置英雄旗")
    if (!placeHeroBanners()) return false
    SceneState.setFlowNode("检查教程")
    if (!mainBaseCheckTutorials()) return false
//    if (!upgradeGears()) return false
    SceneState.setFlowNode("主世界流程完成")
    return true
}