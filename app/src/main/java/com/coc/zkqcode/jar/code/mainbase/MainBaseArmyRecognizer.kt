package com.coc.zkqcode.jar.code.mainbase

import android.graphics.Point
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

/**
 * Recognises main-base troops in two screen contexts:
 *  - the army-composition 7x2 slot grid ("部队配置" / current-army preview), and
 *  - the black-elixir training-panel selection grid.
 *
 * The per-troop features live in [MyColors]:
 *  - 圣水兵 via [com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase.IMainBaseTroopColors]
 *    (tools/gen_mainbase_troop_colors.py),
 *  - 黑油兵 via [com.coc.zkqcode.jar.code.colorschema.colorpackage.mainbase.IMainBaseBlackElixirTroopColors]
 *    (tools/gen_mainbase_black_elixir.py).
 * All features were auto-named from the trophy icons and derived from real 1280x720
 * screenshots, so we simply try each one and collect the hits.
 */
object MainBaseArmyRecognizer {

    /** All main-base troop schemas, in display order. */
    private val TROOP_SCHEMAS: List<Pair<String, ColorSchema>> = listOf(
        // 圣水兵 (army-composition 7x2 grid)
        "哥布林" to MyColors.MainBaseGoblin,
        "大雪怪" to MyColors.MainBaseYeti,
        "天使" to MyColors.MainBaseHealer,
        "巨人" to MyColors.MainBaseGiant,
        "弓箭手" to MyColors.MainBaseArcher,
        "掘地矿工" to MyColors.MainBaseMiner,
        "法师" to MyColors.MainBaseWizard,
        "炸弹人" to MyColors.MainBaseWallBreaker,
        "皮卡超人" to MyColors.MainBasePekka,
        "野蛮人" to MyColors.MainBaseBarbarian,
        "雷电飞龙" to MyColors.MainBaseElectroDragon,
        "飞龙" to MyColors.MainBaseDragon,
        "飞龙宝宝" to MyColors.MainBaseBabyDragon,
        // 黑油兵 (training-panel selection grid)
        "亡灵" to MyColors.MainBaseMinion,
        "德鲁伊" to MyColors.MainBaseDruid,
        "戈仑冰人" to MyColors.MainBaseIceGolem,
        "巨石投手" to MyColors.MainBaseBowler,
        "废墟女巫" to MyColors.MainBaseRuinWitch,
        "女巫" to MyColors.MainBaseWitch,
        "守护者学徒" to MyColors.MainBaseApprenticeWarden,
        "熔岩猎犬" to MyColors.MainBaseLavaHound,
        "英雄猎手" to MyColors.MainBaseHunter,
        "瓦基丽武神" to MyColors.MainBaseValkyrie,
        "野猪骑士" to MyColors.MainBaseHogRider,
        // 黑油兵 (training-panel selection grid) — 补齐此前缺失的戈仑石人/烈焰熔炉
        "戈仑石人" to MyColors.MainBaseGolem,
        "烈焰熔炉" to MyColors.MainBaseInfernoDragon,
        // 超级兵 (配兵界面最右列两槽)
        "寒冰猎犬" to MyColors.MainBaseSuperFrostHound,
        "超级巨石投手" to MyColors.MainBaseSuperBowler,
        "超级弓箭手" to MyColors.MainBaseSuperArcher,
        "超级巨人" to MyColors.MainBaseSuperGiant,
        "超级法师" to MyColors.MainBaseSuperWizard,
        "超级亡灵" to MyColors.MainBaseSuperMinion,
        "超级炸弹人" to MyColors.MainBaseSuperWallBreaker,
        "火箭气球兵" to MyColors.MainBaseRocketBalloon,
        "超级瓦基丽武神" to MyColors.MainBaseSuperValkyrie,
        "超级女巫" to MyColors.MainBaseSuperWitch,
        "超级矿工" to MyColors.MainBaseSuperMiner,
        "超级野猪骑士" to MyColors.MainBaseSuperHogRider,
        "隐秘哥布林" to MyColors.MainBaseSneakyGoblin,
        "超级大雪怪" to MyColors.MainBaseSuperYeti,
    )

    /**
     * @return every troop detected in the grid, as (troopName, slotCenter) pairs.
     *         Empty list when on a screen without the army grid.
     */
    suspend fun recognizeArmy(): List<Pair<String, Point>> {
        val found = mutableListOf<Pair<String, Point>>()
        for ((name, schema) in TROOP_SCHEMAS) {
            val hit = findMultiColors(schema = schema) ?: continue
            found.add(name to Point(hit.x, hit.y))
        }
        return found
    }

    /** Convenience: just the troop names currently present. */
    suspend fun recognizeTroopNames(): List<String> =
        recognizeArmy().map { it.first }
}
