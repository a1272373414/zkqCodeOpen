package com.coc.zkqcode.jar.code.clancapital.attack

import android.graphics.Point
import com.coc.zkqcode.BuildConfig
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.clancapital.attack.CapitalTarget
import com.coc.zkqcode.jar.code.clancapital.saveCapitalScreenshot
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsAll
import android.util.Log
import com.coc.zkqcode.core.system.screencapture.ScreenCaptureManager
import timber.log.Timber

/**
 * Clan-capital battle deploy, ported from the legacy script `函数31a`.
 *
 * Legacy flow per battle:
 *   1. Confirm a battle is running (the give-up button is on screen).
 *   2. Collect "deploy points" by matching the grass/terrain signatures (颜色js1a),
 *      scanning three regions with fallback (区域1 → 区域2 → 区域3)，源 函数31a.
 *   3. For every troop card in the deploy bar: select it, then keep tapping the deploy points
 *      until the card disappears (that troop is fully deployed).
 *   4. For every spell: select it, then tap the matching marker until the spell card disappears.
 *      - 骷髅法术 uses the low-HP enemy markers (残血)
 *      - 疗伤/弹跳/雷电 use the "sneak spell" markers (偷袭法术下兵点)
 *      - 冰冻 uses the "sneak freeze spell" markers (偷袭冰冻法术下兵点)，回退到偷袭法术下兵点
 *
 * Second round (源 主流程 函数39a): when the first scan yields < 2 points, swipe the view to
 * reveal more ground and re-scan, then also add the target building coordinate located by the
 * chosen sub-district (the "9-map" target-building locator) as a deploy point.
 *
 * NOTE: the deploy-bar cards are recognised by their battle-time colour features migrated into
 * [MyColors] (see colorpackage/clancapital/CapitalDeployColors.kt). Those features still need an
 * on-device pass in a real capital battle — no capital-battle screenshot exists offline.
 */

/** Troops to deploy, in the legacy script's order. */
private val DEPLOY_TROOPS: List<ColorSchema> = listOf(
    MyColors.CapitalDeploySuperMiner,
    MyColors.CapitalDeployBattleRam,
    MyColors.CapitalDeploySuperGiant,
    MyColors.CapitalDeploySneakyArcher,
    MyColors.CapitalDeploySuperBarbarian,
    MyColors.CapitalDeployHogRaider,
    MyColors.CapitalDeploySuperWizard,
)

/** 法术投放方案（源 函数31a）：主投放点 primary，缺失时回退 fallback，再缺失用固定兜底点。 */
private data class SpellPlan(
    val schema: ColorSchema,
    val rounds: Int,
    val primary: List<ColorSchema>,
    val fallback: List<ColorSchema>? = null
)

/**
 * 法术投放点（源 函数31a）：
 * - 骷髅法术 → 残血目标（CapitalLowHpTargets）
 * - 疗伤/弹跳/雷电 → 偷袭法术下兵点（CapitalSneakSpellPoints）
 * - 冰冻 → 偷袭法术下兵点（CapitalSneakSpellPoints，源 95233 行 `特征["偷袭法术下兵点"]`），
 *          回退到偷袭冰冻法术下兵点（CapitalSneakFreezePoints）；卡未投出时硬投战场中心 (640,360)
 */
private val DEPLOY_SPELLS: List<SpellPlan> = listOf(
    SpellPlan(MyColors.CapitalDeploySkeletonSpell, 25, MyColors.CapitalLowHpTargets),
    SpellPlan(MyColors.CapitalDeployHealingSpell, 40, MyColors.CapitalSneakSpellPoints),
    SpellPlan(MyColors.CapitalDeployLightningSpell, 25, MyColors.CapitalSneakSpellPoints),
    SpellPlan(MyColors.CapitalDeployJumpSpell, 25, MyColors.CapitalSneakSpellPoints),
    // 源 95233：冰冻用「偷袭法术下兵点」(CapitalSneakSpellPoints) 作主投放点，遍历所有匹配点逐个投（不 break）；
    // 回退到「偷袭冰冻法术下兵点」(CapitalSneakFreezePoints)；仍投不出时由 deploySpell 兜底战场中心 (640,360)
    SpellPlan(MyColors.CapitalDeployFreezeSpell, 25, MyColors.CapitalSneakSpellPoints, MyColors.CapitalSneakFreezePoints),
)

/**
 * 法术专属释放点（独立于下兵点）：覆盖横屏战场中部有效区域，避开顶部资源/横幅（y<130）
 * 和底部部署栏（y>600）。源 函数31a 的法术投在战场目标点 intX,intY，本项目尚未标定"残血"特征，
 * 故用这套点位让法术铺在战场中前部、分散且不被下兵点限制。
 */
private fun generateSpellPoints(): List<Point> {
    return listOf(
        Point(400, 250), Point(640, 250), Point(880, 250),
        Point(300, 400), Point(640, 400), Point(980, 400),
        Point(400, 520), Point(880, 520),
    )
}

/** Legacy troop deploy loop is bounded to 20 rounds. */
private const val TROOP_ROUNDS = 20
private const val MAX_DEPLOY_POINTS = 10

/**
 * 进战斗后缩小地图，让地皮特征能在更大视野内命中更多下兵点。
 *
 * 2026-09-27 三轮都城村庄内实测标定（不消耗突袭次数）：
 *  - ✅ 有效写法：pinchIn(141,423,1052,352→638,365)（项目已验证 API，两指多帧插值收拢到一点），
 *    连续执行两次可把镜头从近景缩小到远景（主世界实测）；
 *  - ❌ 照抄源 L94829~94847 的单步收拢到 20px：被游戏完全忽略；
 *  - ❌ 分步成对张开（起点相距 160px）：起点压建筑被判单击，直接跳转界面；
 *  - ❌ pinchIn 起点压屏幕边缘 UI（如 y=80 顶部资源条）：整只手被 UI 拦截，无效。
 * 因此本函数只做"中部起点 pinchIn ×2"，绝不额外平移/拖拽。
 */
private suspend fun zoomOutCapitalBattlefield() {
    TouchActions.pinchIn(141, 423, 1052, 352, 638, 365, duration = 350L, isJitter = false)
    delayWithMultiplier(500)
    TouchActions.pinchIn(141, 423, 1052, 352, 638, 365, duration = 350L, isJitter = false)
    delayWithMultiplier(300)
}

/**
 * 生成战场均匀网格下兵点，作为地皮特征不足时的兜底。
 * 覆盖横屏战场中部有效区域，避开顶部资源/横幅（y<120）和底部部署栏（y>620）。
 */
private fun generateDeployGrid(): List<Point> {
    val xs = listOf(180, 460, 740, 1020)
    val ys = listOf(160, 280, 400, 520)
    val list = ArrayList<Point>(xs.size * ys.size)
    for (y in ys) {
        for (x in xs) {
            list.add(Point(x, y))
        }
    }
    return list
}

/**
 * 收集下兵点：源 函数31a 三区域逐级回退扫描（区域1 → 区域2 → 区域3）。
 * 源语义：累计满 10 个点才停止（`if 10 <= #特征统计数量 then break`），不足则扫下一区域累加；
 * 同一种地皮绿在战场多处命中，源靠 `findMultiColor` 在 3 区域累积满 10 个点、把兵铺满战场。
 * 本项目 2026-09-27 修正：此前用 findMultiColors（每个地形色只取第一个命中点），整场最多 4 个点、
 * 实际只命中 1 个，导致兵/法术全下在进场视野的一个点。改为 findMultiColorsAll 取每个地形色的
 * 全部命中点。另外，当扫描结果极少时再用战场网格兜底，确保友谊战（不缩放镜头）也能铺开兵/法术。
 */
private suspend fun collectDeployPoints(): List<Point> {
    val points = ArrayList<Point>()
    for (zone in listOf(
        MyColors.CapitalDeployTerrainZone1,
        MyColors.CapitalDeployTerrainZone2,
        MyColors.CapitalDeployTerrain
    )) {
        for (schema in zone) {
            // 取该地形色在本区域的所有命中点（而非第一个），逐点去重累加到多点集合
            val all = findMultiColorsAll(schema, maxMatches = 8)
            for (p in all) {
                if (points.none { it.x == p.x && it.y == p.y }) points.add(p)
                if (points.size >= MAX_DEPLOY_POINTS) return points
            }
        }
    }
    // 地皮特征极少时（常见友谊战未缩放的进场视野），用网格兜底铺满战场，避免全堆一点
    if (points.size < 2) {
        val grid = generateDeployGrid()
        for (p in grid) {
            if (points.none { it.x == p.x && it.y == p.y }) points.add(p)
            if (points.size >= MAX_DEPLOY_POINTS) break
        }
    }
    return points
}

/** Select the troop card, then tap the deploy points until the card disappears. */
private suspend fun deployTroop(points: List<Point>, schema: ColorSchema): Boolean {
    val card = findMultiColors(schema) ?: return false
    TouchActions.tap(card.x, card.y, delayTime = 150)
    repeat(TROOP_ROUNDS) {
        for (p in points) {
            TouchActions.tap(p.x, p.y, delayTime = 50)
            if (findMultiColors(schema) == null) return true
        }
    }
    return findMultiColors(schema) == null
}

/**
 * 选择法术卡，按源 函数31a 的投放逻辑把法术洒出去，直到卡消失。
 * 释放点独立于下兵点：源脚本投在战场目标点 intX,intY，本项目未标定"残血"特征，
 * 故无专属目标时投到 [generateSpellPoints] 的战场中点集（每轮换点分散），不依赖下兵点。
 */
private suspend fun deploySpell(plan: SpellPlan): Boolean {
    val t0 = System.currentTimeMillis()
    val card = findMultiColors(plan.schema) ?: run {
        Timber.tag("zkq_debug").v("ZKQDBG:法术[${plan.schema.name}] 未找到卡, 直接跳过 (${System.currentTimeMillis() - t0}ms)")
        return false
    }
    Timber.tag("zkq_debug").v("ZKQDBG:法术[${plan.schema.name}] 命中卡@(${card.x},${card.y}) 开始投放")
    TouchActions.tap(card.x, card.y, delayTime = 150)
    val spellPts = generateSpellPoints()
    repeat(plan.rounds) { round ->
        var tapped = false
        var nTargets = 0
        val tRound0 = System.currentTimeMillis()
        // 本轮只截一次图，所有投放点特征复用同一张截图扫描（原来每个特征各截一次，开销很大）
        val cap = ScreenCaptureManager.capture(asBitmap = false) as? ScreenCaptureManager.CaptureResult
        // 主投放点：源 骷髅→残血，其余→偷袭法术下兵点；取全部匹配点逐个喷洒（对齐源脚本多目标逻辑）
        for (hp in plan.primary) {
            for (target in findMultiColorsAll(hp, maxMatches = 8, byteBuffer = cap)) {
                TouchActions.tap(target.x, target.y, delayTime = 300)
                tapped = true
                nTargets++
            }
        }
        // 主投放点缺失时回退（源：冰冻→偷袭冰冻法术下兵点）
        if (!tapped) {
            plan.fallback?.let { fb ->
                for (hp in fb) {
                    for (target in findMultiColorsAll(hp, maxMatches = 8, byteBuffer = cap)) {
                        TouchActions.tap(target.x, target.y, delayTime = 300)
                        tapped = true
                        nTargets++
                    }
                }
            }
        }
        // 无专属目标特征时投到法术专属点（独立于下兵点），按轮次换点避免所有法术堆在一处
        if (!tapped) {
            val p = spellPts[round % spellPts.size]
            TouchActions.tap(p.x, p.y, delayTime = 300)
            tapped = true
            nTargets++
        }
        // 源 函数31a 冰冻段最终兜底：卡仍未投出时硬投战场中心（竖屏 360,640 → 横屏 640,360）
        if (!tapped) {
            TouchActions.tap(640, 360, delayTime = 300)
            tapped = true
            nTargets++
        }
        val tAfterDeploy = System.currentTimeMillis()
        delayWithMultiplier(50)
        val stillThere = findMultiColors(plan.schema) != null
        val tAfterStill = System.currentTimeMillis()
        Timber.tag("zkq_debug").v("ZKQDBG:法术[${plan.schema.name}] round=$round tapped=$tapped nTargets=$nTargets deployMs=${tAfterDeploy - tRound0} stillMs=${tAfterStill - tAfterDeploy} stillThere=$stillThere elapsed=${System.currentTimeMillis() - t0}ms")
        if (!stillThere) {
            Timber.tag("zkq_debug").v("ZKQDBG:法术[${plan.schema.name}] 卡已消失, 结束 (${System.currentTimeMillis() - t0}ms)")
            return true
        }
    }
    Timber.tag("zkq_debug").v("ZKQDBG:法术[${plan.schema.name}] 跑满 rounds=${plan.rounds} 仍未消失, 结束 (${System.currentTimeMillis() - t0}ms)")
    return findMultiColors(plan.schema) == null
}

/** 滑动视野（源 主流程第二轮 swipes(1,160,976,710,158,300) 的横屏映射），露出更多可下兵区域。 */
private suspend fun swipeCapitalView() {
    // 竖屏(160,976)→(710,158) 旋转 90° → 横屏(976,559)→(158,9)；实际为避免触及顶部边缘(y=9)误触通知栏，
    // 终点取 (300,250) 的安全位置，滑动幅度已足以改变视野、露出更多可下兵区域
    TouchActions.swipe(976, 559, 300, 250, delayTime = 400)
    delayWithMultiplier(400)
}

/**
 * Deploy the whole capital army in the current battle (troops first, then spells).
 *
 * @param target 本次选择的子城目标（含横屏坐标 x,y），用于第二轮「9 地图定位目标建筑」补点。
 * @return true when at least one troop/spell was deployed.
 */
suspend fun capitalDeployArmy(
    target: CapitalTarget? = null,
    zoomOut: Boolean = true,
    requireGiveUp: Boolean = true
): Boolean {
    Timber.tag("zkq_debug").v("ZKQDBG:>>>>> ENTER capitalDeployArmy 版本=${BuildConfig.BUILD_ID}")
    if (requireGiveUp && findMultiColors(MyColors.GiveUpButton) == null) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：未检测到放弃按钮，可能不在战斗中")
        return false
    }
    // 关键节点截图存档（测试期宝贵，每号每周仅 5 次机会，务必留存）
    saveCapitalScreenshot("capital_battle_start")

    // 源主流程：下兵前先缩小镜头，否则兵/法术全下在进场视野的一个点。
    // 2026-09-27 已在都城村庄完成三轮手势标定（见 zoomOutCapitalBattlefield 注释），启用之。
    // zoomOut=false 用于友谊战测试首跑：战斗内缩放手势尚未专项标定，先跳过避免误触跳转界面。
    if (zoomOut) zoomOutCapitalBattlefield()

    // 第一轮：三区域逐级回退扫描下兵点
    var points = collectDeployPoints()
    ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第一轮找到下兵点 ${points.size} 个")

    // 第二轮（源 函数31a/主流程）：第一轮点不足时滑动视野重扫，并用 9 地图定位目标建筑坐标
    if (points.size < 2) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第一轮下兵点不足，进入第二轮（滑动视野重扫 + 9地图目标建筑）")
        swipeCapitalView()
        // 第二轮扫描结果为可变列表，便于按 9 地图定位追加目标建筑放兵点
        val second = collectDeployPoints().toMutableList()
        // 9 地图定位：把本次选择的子城目标建筑坐标（横屏 x,y）加入放兵点
        target?.let { t ->
            if (second.none { it.x == t.x && it.y == t.y }) second.add(Point(t.x, t.y))
        }
        if (second.isNotEmpty()) {
            points = second
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第二轮找到下兵点 ${points.size} 个")
        } else {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：第二轮仍无下兵点")
        }
    }

    if (points.isEmpty()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：始终未找到下兵点，结束本场进攻")
        return false
    }

    var acted = false
    // 先放法术：法术先铺场（中心区域），兵再跟进到各下兵点，避免兵先占满后法术落点被挤到边角
    for (plan in DEPLOY_SPELLS) {
        val pre = findMultiColors(plan.schema)
        Timber.tag("zkq_debug").v("ZKQDBG:法术预判[${plan.schema.name}] findMultiColors=${pre?.let { "(${it.x},${it.y})" } ?: "null"}")
        if (deploySpell(plan)) {
            acted = true
            ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：法术已投放 ${plan.schema.name}")
        }
    }
    for (schema in DEPLOY_TROOPS) if (deployTroop(points, schema)) {
        acted = true
        ShowMessage("账号${InGamesVars.currentAccountNumber}，都城：兵种已下场 ${schema.name}")
    }
    saveCapitalScreenshot("capital_battle_deployed")
    return acted
}
