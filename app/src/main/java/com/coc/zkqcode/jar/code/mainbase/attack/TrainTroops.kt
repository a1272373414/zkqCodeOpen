package com.coc.zkqcode.jar.code.mainbase.attack

import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.ColorSchema
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.checkMemoryFile
import com.coc.zkqcode.jar.code.universal.smalltools.getBooleanConfigRuntime
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory
import com.coc.zkqcode.jar.code.mainbase.MainBaseArmyRecognizer
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.GameScene
import com.coc.zkqcode.jar.code.universal.detectCurrentScene
import com.coc.zkqcode.jar.code.universal.waitForScene
import com.coc.zkqcode.jar.ui.schema.Schema

/**
 * Training-page category tabs, calibrated on-device (emulator-5556, 1280x720) on
 * 2026-09-20. Each coordinate taps the corresponding queue row / tab slot to open its
 * selection panel; the switch is verified by the category's signature troop schema
 * (see [switchTrainingTab]). Reused from the legacy freescript idea of a `兵种显示`
 * state machine: we track which tab we are on and verify the switch by checking the
 * category's signature troop schema.
 *
 * Confirmed working: troops(891,234) / spells(797,420) / siege(1126,423) open their
 * panels; (219,139) closes the panel. The trash buttons (DeleteAll1/2/3) only render
 * when that queue is non-empty, so a NOT-FOUND there simply means the queue is empty.
 */
private val TAB_TROOPS = 891 to 234
private val TAB_SPELLS = 797 to 420
private val TAB_SIEGE = 1126 to 423

/**
 * Taps a training-page category tab and verifies the switch succeeded by looking for
 * the category's signature schema. On failure it logs a warning (observability) instead
 * of silently mis-clicking; the caller decides whether to continue.
 *
 * @return true if the expected category became visible.
 */
private suspend fun switchTrainingTab(
    x: Int, y: Int, verifySchema: ColorSchema?, tabName: String
): Boolean {
    TouchActions.tap(x, y, delayTime = 800)
    if (verifySchema == null) return true
    val ok = findMultiColorsUntil(schemas = listOf(verifySchema), duration = 800) != null
    if (!ok) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，切换分页【$tabName】可能失败（未找到特征）")
    }
    return ok
}

/**
 * 兵种名 -> 训练卡片特征。
 *
 * 迁移自原脚本 `函数275a` 的「造XX」特征（见 [MainBaseTrainCardColors]，77 个），
 * 原脚本的定位方式是「当前页用该兵种自己的特征找卡片，找不到就翻页（左/中/右）重试」，
 * 因此**不依赖兵种顺序与位置**：最左侧的活动兵（数量/名称都不固定）不会影响其它兵种的定位。
 */
private val TRAIN_CARD: Map<String, ColorSchema> = mapOf(
    // 圣水兵
    "野蛮人" to MyColors.TrainCard野蛮, "弓箭手" to MyColors.TrainCard弓箭,
    "巨人" to MyColors.TrainCard巨人, "哥布林" to MyColors.TrainCard小偷,
    "炸弹人" to MyColors.TrainCard炸弹, "气球兵" to MyColors.TrainCard气球,
    "法师" to MyColors.TrainCard法师, "天使" to MyColors.TrainCard天使,
    "飞龙" to MyColors.TrainCard飞龙, "飞龙宝宝" to MyColors.TrainCard龙宝,
    "皮卡超人" to MyColors.TrainCard皮卡, "大雪怪" to MyColors.TrainCard雪怪,
    "掘地矿工" to MyColors.TrainCard矿工, "雷电飞龙" to MyColors.TrainCard雷龙,
    "巨矛投手" to MyColors.TrainCard巨矛, "雷霆泰坦" to MyColors.TrainCard泰坦,
    "根蔓骑士" to MyColors.TrainCard根骑, "龙骑士" to MyColors.TrainCard龙骑,
    "陨石巨人" to MyColors.TrainCard陨石戈仑,
    // 黑油兵
    "亡灵" to MyColors.TrainCard亡灵, "女巫" to MyColors.TrainCard女巫,
    "守护者学徒" to MyColors.TrainCard学徒, "巨石投手" to MyColors.TrainCard投手,
    "废墟女巫" to MyColors.TrainCard废墟女巫, "德鲁伊" to MyColors.TrainCard鲁伊,
    "戈仑冰人" to MyColors.TrainCard冰人, "戈仑石人" to MyColors.TrainCard石头,
    "烈焰熔炉" to MyColors.TrainCard熔炉, "熔岩猎犬" to MyColors.TrainCard猎犬,
    "瓦基丽武神" to MyColors.TrainCard武神, "英雄猎手" to MyColors.TrainCard猎手,
    "野猪骑士" to MyColors.TrainCard野猪,
    // 超级兵
    "超级野蛮人" to MyColors.TrainCard超蛮, "超级弓箭手" to MyColors.TrainCard超弓,
    "超级巨人" to MyColors.TrainCard超巨, "隐秘哥布林" to MyColors.TrainCard超偷,
    "超级炸弹人" to MyColors.TrainCard超炸, "超级法师" to MyColors.TrainCard超法,
    "超级亡灵" to MyColors.TrainCard超亡, "超级女巫" to MyColors.TrainCard超巫,
    "超级瓦基丽武神" to MyColors.TrainCard超武, "超级矿工" to MyColors.TrainCard超矿,
    "超级野猪骑士" to MyColors.TrainCard超猪, "火箭气球兵" to MyColors.TrainCard超球,
    "超级大雪怪" to MyColors.TrainCard超怪, "超级飞龙" to MyColors.TrainCard超龙,
    "超级巨石投手" to MyColors.TrainCard超投, "寒冰猎犬" to MyColors.TrainCard超犬,
)

/** 每种兵的默认造兵次数；队列满后多余点击会被游戏忽略，不会出错。 */
private val DEFAULT_COUNT = mapOf(
    "飞龙" to 25, "巨人" to 5, "弓箭手" to 40, "野蛮人" to 40,
    "哥布林" to 30, "法师" to 10, "掘地矿工" to 10, "飞龙宝宝" to 10,
    "雷电飞龙" to 10, "天使" to 5, "炸弹人" to 10, "大雪怪" to 10, "皮卡超人" to 5,
)

/**
 * 选兵面板内的翻页手势：面板为一屏一页的横向列表（活动兵在最左，圣水兵/黑油兵/超级兵依次向右），
 * 右滑回最左、左滑进一页。坐标在 emulator-5556 / 1280x720 上标定（2026-09-20）。
 */
private const val PAGE_LEFT_X = 120
private const val PAGE_RIGHT_X = 1180
private const val PAGE_Y = 560
private const val PAGE_SWIPE_MS = 400

/**
 * 选兵面板页数上限。
 *
 * 兵种页**不是固定的 3 页**：活动兵是动态的（数量/名称都不固定，且固定占用最左侧卡片），
 * 会把后续兵种整体右移、必要时多出一页；赛季/活动结束后又会变回。因此这里取一个足够大的上限并
 * 配合「全部找到就早退 + 连续空页提前结束」，既不会漏掉被挤到后面的兵种，也不会无意义地一直滑。
 */
private const val MAX_PICKER_PAGES = 6

/** 连续多少页没找到任何目标兵种就提前结束翻页（避免面板到头后空滑）。 */
private const val EMPTY_PAGE_LIMIT = 2

/**
 * 活动兵（可选配置）。活动兵是动态的：数量、名称都随赛季/活动变化，且固定排在最左侧。
 *
 * **解耦原则**：即使这里完全没有配置（表为空，或将来接入的配置读取失败/格式变化），
 * 也只会"不造活动兵"，**不会影响圣水兵/黑油兵/超级兵等原有兵种的定位与造兵** ——
 * 因为每个原有兵种都是靠自己的训练卡片特征、跨页搜索定位的，不依赖任何绝对位置或兵种顺序。
 *
 * 后续接入配置时，只需往这两张表里填「显示名 -> 训练卡片特征／造兵次数」即可，
 * 无需改动 `mainBaseTrainTroops()` 的主流程。
 */
private val EVENT_TROOP_CARDS: Map<String, ColorSchema> = emptyMap()
private val EVENT_TROOP_COUNTS: Map<String, Int> = emptyMap()

/** 识别为空时的核心造兵计划（避免静默不造兵）。 */
private val CORE_FALLBACK = listOf("飞龙", "巨人", "弓箭手", "野蛮人")

/** 查兵种对应的训练卡片特征：活动兵配置优先，其次为迁移来的固定兵种表。 */
private fun trainCardOf(name: String): ColorSchema? = EVENT_TROOP_CARDS[name] ?: TRAIN_CARD[name]

/** 查兵种默认造兵次数。 */
private fun trainCountOf(name: String): Int =
    DEFAULT_COUNT[name] ?: EVENT_TROOP_COUNTS[name] ?: 10

/**
 * 把选兵列表钉回最左页。幂等：已在最左时多余滑动不会越界；次数取 [MAX_PICKER_PAGES]，
 * 保证活动兵把列表拉长、页数变多时也能回到真正的第一页。
 */
private suspend fun pinPickerToFirstPage() {
    repeat(MAX_PICKER_PAGES) {
        TouchActions.swipe(PAGE_LEFT_X, PAGE_Y, PAGE_RIGHT_X, PAGE_Y, delayTime = PAGE_SWIPE_MS)
        delayWithMultiplier(250)
    }
}

/** 左滑一页（进入下一页兵种）。 */
private suspend fun nextPickerPage() {
    TouchActions.swipe(PAGE_RIGHT_X, PAGE_Y, PAGE_LEFT_X, PAGE_Y, delayTime = PAGE_SWIPE_MS)
    delayWithMultiplier(350)
}

/**
 * 在当前页用 [feature] 定位 [name] 的训练卡片并连点 [times] 次造兵。
 * @return true 表示当前页找到了该兵种卡片（已点击）。
 */
private suspend fun trainTroopOnPage(name: String, feature: ColorSchema, times: Int): Boolean {
    val p = findMultiColors(feature) ?: return false
    repeat(times) { TouchActions.tap(p.x, p.y, delayTime = 40) }
    return true
}

/** 一个造兵目标：显示名 + 训练卡片特征 + 点击次数。 */
private data class TrainTarget(val name: String, val feature: ColorSchema, val times: Int)

/**
 * 选兵面板通用造兵流程（兵种 / 法术 / 攻城器共用）：
 * 钉回最左页 → 逐页「当前页找到就连点造兵，找不到翻页重试」。
 * 页数自适应：见 [MAX_PICKER_PAGES] / [EMPTY_PAGE_LIMIT]（活动兵会改变页数）。
 */
private suspend fun trainByFeatures(label: String, plan: List<TrainTarget>) {
    val pending = plan.toMutableList()
    if (pending.isEmpty()) return
    pinPickerToFirstPage()
    var page = 1
    var emptyPages = 0
    while (pending.isNotEmpty() && page <= MAX_PICKER_PAGES && emptyPages < EMPTY_PAGE_LIMIT) {
        SceneState.setFlowNode("练兵-$label-第${page}页")
        var hitsOnPage = 0
        val pendingIt = pending.iterator()
        while (pendingIt.hasNext()) {
            val target = pendingIt.next()
            if (trainTroopOnPage(target.name, target.feature, target.times)) {
                hitsOnPage++
                pendingIt.remove()
            }
        }
        if (pending.isEmpty()) break
        emptyPages = if (hitsOnPage == 0) emptyPages + 1 else 0
        page++
        nextPickerPage()
    }
    if (pending.isNotEmpty()) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，$label 未找到训练卡片：${pending.joinToString("、") { it.name }}")
    }
}

/**
 * 法术显示名 -> 训练卡片特征（迁移自原脚本「造XX」，2026-09-21 在真机全量校验 18/18 命中）。
 */
private val SPELL_CARD: Map<String, ColorSchema> = mapOf(
    "闪电法术" to MyColors.TrainCard雷电, "冰冻法术" to MyColors.TrainCard冰冻,
    "冰障法术" to MyColors.TrainCard冰障, "地震法术" to MyColors.TrainCard地震,
    "毒药法术" to MyColors.TrainCard毒药, "疗伤法术" to MyColors.TrainCard疗伤,
    "弹跳法术" to MyColors.TrainCard弹跳, "急速法术" to MyColors.TrainCard急速,
    "狂暴法术" to MyColors.TrainCard狂暴, "镜像法术" to MyColors.TrainCard镜像,
    "隐形法术" to MyColors.TrainCard隐形, "回溯法术" to MyColors.TrainCard回溯,
    "图腾法术" to MyColors.TrainCard图腾, "愤怒法术" to MyColors.TrainCard愤怒法术,
    "骷髅法术" to MyColors.TrainCard骷髅, "蝙蝠法术" to MyColors.TrainCard蝙蝠,
    "复苏法术" to MyColors.TrainCard复苏, "蔓生法术" to MyColors.TrainCard蔓生,
)

/**
 * 攻城器显示名 -> 训练卡片特征（2026-09-21 在真机全量校验 9/9 命中；列表向右还有内容，需翻页）。
 */
private val SIEGE_CARD: Map<String, ColorSchema> = mapOf(
    "攻城战车" to MyColors.TrainCard战车, "战斗飞艇" to MyColors.TrainCard飞艇,
    "攻城气球" to MyColors.TrainCard战球, "攻城兵营" to MyColors.TrainCard战营,
    "滚木发射器" to MyColors.TrainCard滚木, "烈焰喷射器" to MyColors.TrainCard烈焰,
    "战斗钻机" to MyColors.TrainCard钻机, "空中战车" to MyColors.TrainCard空中战车,
    "部队发射器" to MyColors.TrainCard部队发射器,
)

/**
 * 法术默认造兵计划。原先是对 3 个写死坐标点共 8 次（换设备/列表变化即失效），
 * 现改为按「闪电法术」的训练卡片特征定位后连点 8 次；要调整造什么法术只改这张表
 * （可选法术见 [SPELL_CARD]，已全量校验）。
 */
/** 每种法术的默认造兵次数；不在表里的法术默认不造（随时可在此开启）。 */
private val SPELL_COUNTS = mapOf("闪电法术" to 8)

private val SPELL_PLAN = SPELL_CARD
    .map { (name, feature) -> TrainTarget(name, feature, SPELL_COUNTS[name] ?: 0) }
    .filter { it.times > 0 }

/**
 * 攻城器默认造兵计划：原先是对 4 个写死坐标各点 1 次（即每种造 1 个），
 * 现改为按特征定位——每种造 1 个，找不到的会被记录（不静默失败）。
 * 9 种已全部校验可定位（含右滑后的第 2 页）。
 */
private val SIEGE_PLAN = SIEGE_CARD.map { (name, feature) -> TrainTarget(name, feature, 1) }

suspend fun mainBaseTrainTroops(): Boolean {
    val isAttackEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.AUTO_ATTACK.key)
    val isManualTrainEnabled = getBooleanConfigRuntime(Schema.MAIN_BASE_SETTINGS.MANUAL_TRAINING.key)
    if (!isAttackEnabled || isManualTrainEnabled) return true
    val storageKey = StorageKeys.withAccountNumber(StorageKeys.MAIN_BASE_TRAIN_TROOPS, InGamesVars.currentAccountNumber)

    if (checkMemoryFile(storageKey, 1440)) {
    ShowMessage("账号${InGamesVars.currentAccountNumber}，准备训练部队")
    GlobalVars.absorbEdge = 1

    // Phase 1 (reused from legacy freescript): log where we are before acting, so the
    // user always knows the current page / flow node instead of guessing.
    SceneState.setScene(detectCurrentScene())
    SceneState.setFlowNode("练兵-开始")

    // Open training menu
    var point = findMultiColorsUntil(schemas = listOf(MyColors.TrainTroops), duration = 1500)
    if (point != null) {
        TouchActions.tap(point.x, point.y, delayTime = 500)
    } else {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，训练部队失败")
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    }

    // Verify training page (unified wait loop: sweeps popups + logs the scene each tick).
    SceneState.setFlowNode("练兵-打开训练页")
    if (!waitForScene(GameScene.TRAINING_PAGE, 5)) {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，未找到训练标志")
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    }


        // Clean Queue 1
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll1), duration = 1000)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Train Troops Tab (圣水兵) — verify the colored barbarian card appears.
        SceneState.setFlowNode("练兵-圣水兵")
        switchTrainingTab(TAB_TROOPS.first, TAB_TROOPS.second, MyColors.TrainBarbarian, "圣水兵")

        // 兵种识别接入：识别当前部队配置（14 槽位）中的兵种，作为造兵依据（"按识别结果造兵"）
        val recognized = runCatching { MainBaseArmyRecognizer.recognizeTroopNames() }.getOrElse { emptyList() }
        if (recognized.isNotEmpty()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，当前部队：${recognized.joinToString("、")}")
        } else {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，兵种识别为空，回退核心造兵计划")
        }

        // 按识别出的兵种，去训练列表逐页定位并造兵。
        // 定位方式沿用原脚本：每个兵种有自己的训练卡片特征（MainBaseTrainCardColors，迁移自「造XX」），
        // 在当前页找不到就翻页重试 —— 不依赖兵种顺序/位置，最左侧的活动兵（数量/名称不固定）不影响定位。
        val pending = LinkedHashSet(recognized).mapNotNull { name ->
            trainCardOf(name)?.let { name to it }
        }.toMutableList()
        // 识别为空、或识别出的兵种都没有对应训练卡片（例如当前部队全是不认识的活动兵）时，
        // 回退核心兵种计划，避免静默不造兵。
        if (pending.isEmpty()) {
            ShowMessage("账号${InGamesVars.currentAccountNumber}，识别结果无可用训练卡片，回退核心造兵计划")
            CORE_FALLBACK.forEach { name -> trainCardOf(name)?.let { pending += name to it } }
        }

        trainByFeatures("兵种", pending.map { (name, feature) -> TrainTarget(name, feature, trainCountOf(name)) })

        // Close tab and Clean Queue 2
        TouchActions.tap(219, 139, delayTime = 1000)
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll2), duration = 500)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Spell Tab (法术) — verify the lightning spell card appears.
        SceneState.setFlowNode("练兵-法术")
        switchTrainingTab(TAB_SPELLS.first, TAB_SPELLS.second, MyColors.TrainLighteningSpell, "法术")
        if (findMultiColorsUntil(schemas = listOf(MyColors.TrainLighteningSpell), duration = 500) != null) {
            // 原为 3 个写死坐标共 8 次点击，现改为按特征定位（见 SPELL_PLAN）
            trainByFeatures("法术", SPELL_PLAN)
        }

        // Close tab and Clean Queue 3
        TouchActions.tap(219, 139, delayTime = 1000)
        point = findMultiColorsUntil(schemas = listOf(MyColors.DeleteAll3), duration = 500)
        if (point != null) {
            TouchActions.tap(point.x, point.y)
            delayWithMultiplier(500)
            findMultiColorsUntil(schemas = listOf(MyColors.MiddleGreenYes), duration = 1500)?.let {
                TouchActions.tap(it.x, it.y, delayTime = 500)
            }
        }

        // Siege Machines Tab (攻城机器) — verify the siege machine card appears.
        SceneState.setFlowNode("练兵-攻城机器")
        switchTrainingTab(TAB_SIEGE.first, TAB_SIEGE.second, MyColors.TrainSiegeMachine, "攻城机器")
        if (findMultiColorsUntil(schemas = listOf(MyColors.TrainSiegeMachine), duration = 500) != null) {
            // 原为 4 个写死坐标各点 1 次，现改为按特征定位（见 SIEGE_PLAN）
            trainByFeatures("攻城机器", SIEGE_PLAN)
        }

        // Final Close
        TouchActions.tap(219, 139, delayTime = 1000)
        TouchActions.tap(1232, 65, delayTime = 300)

        writeMemory(storageKey, (System.currentTimeMillis() / 60_000).toString())
        GlobalVars.absorbEdge = 0
        return enterMainScreen()
    } else {
        ShowMessage("账号${InGamesVars.currentAccountNumber}，该账号今日已练兵")
        return true
    }
}