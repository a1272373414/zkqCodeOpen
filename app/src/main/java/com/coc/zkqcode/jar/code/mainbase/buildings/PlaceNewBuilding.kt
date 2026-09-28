package com.coc.zkqcode.jar.code.mainbase.buildings

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 放置新建筑（T38，源 函数213a L31359 / 函数215a L31502；**宝石秒升级不做**，见用户 2026-09-28 决定）。
 *
 * 源流程：开商店 → 选建筑（按名 v）→ 拖到地图空位 → 点对号创建（函数215a 点 `intX+4,intY+4` 的对号）。
 * 本项目用 [ChineseTextReader.locate] 文字定位「商店」与建筑名，选完后点地图中心区域作为落点（屏幕基准
 * 1280×720 的 (640,640)），再点「确定/对」确认。精确落点坐标、对号/叉号图标待 T30 真机复标。
 *
 * 主开关 [StorageKeys.PLACE_BUILDING_ENABLED] 未开或本账号未配置目标建筑（[StorageKeys.PLACE_BUILDING_TARGET] 空）则跳过。
 */
object PlaceNewBuilding {

    /** 放置落点（屏幕基准 1280×720）；真机复标（T30）后按需调整。 */
    private val DROP_X = 640
    private val DROP_Y = 640

    suspend fun placeNewBuilding(account: Int): Boolean {
        val target = readMemory(StorageKeys.withAccountNumber(StorageKeys.PLACE_BUILDING_TARGET, account))
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.PLACE_BUILDING_ENABLED, account)) != "1" || target.isEmpty()) {
            return false
        }
        ShowMessage("账号$account，放置建筑：开始（目标=$target）")
        if (!openShop()) {
            ShowMessage("账号$account，放置建筑：未能打开商店")
            return false
        }
        val building = ChineseTextReader.locate(target)
        if (building == null) {
            ShowMessage("账号$account，放置建筑：未找到「$target」")
            return false
        }
        TouchActions.tap(building.centerX(), building.centerY())
        delay(1200)
        // 点到地图落点（选建筑后会进入放置态，点地图空位放置）
        TouchActions.tap(DROP_X, DROP_Y, delayTime = 800)
        delay(800)
        // 确认放置：点「确定」/「对」/「建造」
        val confirm = ChineseTextReader.locate("确定") ?: ChineseTextReader.locate("建造")
        ?: ChineseTextReader.locate("对")
        if (confirm != null) {
            TouchActions.tap(confirm.centerX(), confirm.centerY())
            delay(1500)
            ShowMessage("账号$account，放置建筑：已放置 $target")
            return true
        }
        return false
    }

    /** 打开商店：定位「商店」入口并点击。 */
    private suspend fun openShop(): Boolean {
        val shop = ChineseTextReader.locate("商店") ?: ChineseTextReader.locate("建造")
        if (shop != null) {
            TouchActions.tap(shop.centerX(), shop.centerY())
            delay(1500)
            return true
        }
        return false
    }
}
