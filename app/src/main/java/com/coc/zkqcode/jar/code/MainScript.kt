package com.coc.zkqcode.jar.code

import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.util.basic.RunShell
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.basic.waitForPlay
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.builderbase.playBuilderBase
import com.coc.zkqcode.jar.code.clancapital.playClanCapital
import com.coc.zkqcode.jar.code.clancapital.attack.playCapitalFriendlyChallenge
import com.coc.zkqcode.jar.code.clancapital.saveCapitalScreenshot
import com.coc.zkqcode.jar.code.clancapital.attack.enterClanCapital
import com.coc.zkqcode.jar.code.clancapital.attack.isInClanCapital
import com.coc.zkqcode.jar.code.clancapital.attack.openCapitalRaidMap
import com.coc.zkqcode.jar.code.clancapital.attack.pickCapitalTarget
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.colors.findMultiColorsUntil
import com.coc.zkqcode.jar.code.mainbase.playMainBase
import com.coc.zkqcode.jar.code.mainbase.clangames.playClanGames
import com.coc.zkqcode.jar.code.universal.CameraState
import com.coc.zkqcode.jar.code.universal.GameRunControl
import com.coc.zkqcode.jar.code.universal.GameVersion
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.SceneState
import com.coc.zkqcode.jar.code.universal.create.batchCreateAccounts
import com.coc.zkqcode.jar.code.universal.enterMainScreen
import com.coc.zkqcode.jar.code.universal.handleRunControl
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.getConfigOrStop
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import com.coc.zkqcode.jar.code.universal.smalltools.writeGameFiles
import com.coc.zkqcode.jar.code.universal.smalltools.writeMemory
import com.coc.zkqcode.jar.code.universal.smalltools.AgreementPopups
import com.coc.zkqcode.jar.code.universal.smalltools.ChangeWarBase
import com.coc.zkqcode.jar.code.universal.smalltools.Trader
import com.coc.zkqcode.jar.code.mainbase.clan.ClanWar
import com.coc.zkqcode.jar.code.mainbase.league.League
import com.coc.zkqcode.jar.code.mainbase.clan.donateToClan
import com.coc.zkqcode.core.util.fileactions.LogHelper.logAndRestart
import com.coc.zkqcode.jar.code.mainbase.herohall.upgradeGearsAndPets
import com.coc.zkqcode.jar.code.mainbase.others.mainBaseCheckTutorials
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors
import com.coc.zkqcode.jar.ui.schema.Schema
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


suspend fun runMainScript() {
    waitForPlay()
    ShowMessage("检测到设置已更新\n即将重新运行")
    delay(200)
    // Read hot update preference and emit signal if "OnStart" mode is selected
    val updateOption = getConfigOrStop(Schema.GLOBAL_SETTINGS.AUTO_UPDATE.key).toIntOrNull() ?: 0
    if (updateOption == 1 || updateOption == 2) {
        val deferred = CompletableDeferred<Unit>()
        GlobalVars.updateCheckSignal.tryEmit(deferred)
        deferred.await()
    }
    // 1. Initialize/update local memory state
    val isBatchCreate = getConfigOrStop(Schema.GLOBAL_SETTINGS.BATCH_CREATE_ACCOUNT.key) == "1"

    val accountTotal: Int = if (isBatchCreate) {
        getConfigOrStop(Schema.GLOBAL_SETTINGS.CREATE_END_ID.key).toIntOrNull() ?: logAndRestart("${Schema.GLOBAL_SETTINGS.CREATE_END_ID.displayName} 必须是数字，请检查配置")
    } else {
        getConfigOrStop(Schema.GLOBAL_SETTINGS.ACCOUNT_COUNT.key).toIntOrNull() ?: logAndRestart("${Schema.GLOBAL_SETTINGS.ACCOUNT_COUNT.displayName} 必须是数字，请检查配置")
    }
    // In batch-create mode, the lower bound comes from CREATE_START_ID
    val accountStart: Int = if (isBatchCreate) {
        getConfigOrStop(Schema.GLOBAL_SETTINGS.CREATE_START_ID.key).toIntOrNull() ?: logAndRestart("${Schema.GLOBAL_SETTINGS.CREATE_START_ID.displayName} 必须是数字，请检查配置")
    } else {
        1
    }

    val startAccount = readMemory(StorageKeys.ACCOUNT_NUMBER).toIntOrNull() ?: accountStart
    // Reset startAccount to accountStart if it is out of range (e.g. account count was reduced)
    val safeStartAccount = if (startAccount !in accountStart..accountTotal) accountStart else startAccount

    // 2. Find the first enabled account starting from the saved position, wrapping around
    val initialSearchOrder = (safeStartAccount..accountTotal) + (accountStart until safeStartAccount)
    val activeAccount = findAndActivateAccount(initialSearchOrder, isBatchCreate) ?: return
    // 3. Execute main logic loop
    InGamesVars.currentAccountNumber = activeAccount
    // In batch-create mode, force GLOBAL version for all accounts
    InGamesVars.currentGameVersion = if (isBatchCreate) {
        GameVersion.GLOBAL
    } else {
        GameVersion.fromId(
            getConfigOrStop("${Schema.ACCOUNT_SETTINGS.GAME_VERSION.key}${InGamesVars.currentAccountNumber}").toIntOrNull() ?: logAndRestart("${Schema.ACCOUNT_SETTINGS.GAME_VERSION.displayName} 必须是数字，请检查配置")
        )
    }
    batchCreateAccounts()//Create all needed accounts first.
    // Track elapsed time for periodic hot update checks (updateOption 2)
    var lastUpdateCheckTime = System.currentTimeMillis()
    var nextUpdateInterval = (2 * 3600_000L) + (Math.random() * 3600_000L).toLong()
    while (currentCoroutineContext().isActive) {
        // 临时调试入口（2026-09-27 都城突袭真打调试）：只跑都城流程，跳过夜世界/主世界。
        // 调试完成后务必还原为注释，恢复正式主循环！
        runTestCode()

        // Phase 4 (reused from legacy 游戏运行控制): act on the current run-control state and
        // bail out if the script was stopped (repeated unknown / fatal state).
        if (!handleRunControl()) return

        RunShell.runNoOutput("am kill-all")//clean up memory
        // Use labeled block to skip remaining steps on failure
        run stepBlock@{
            if (!writeGameFiles()) {
                delayWithMultiplier(2000)
                return@stepBlock
            }

            // 协议弹窗处理（T05）：首启/切号重进可能卡在「同意/全部接受」，先清掉再进游戏
            AgreementPopups.handleAgreementPopups()

            if (!enterMainScreen(true)) {
                ShowMessage("进入游戏失败")
                return@stepBlock
            }

            // 战争基地阵型切换（T04）：CHANGE_BASE 开启且本账号配置了目标阵型时执行（0/未配置则不换）
            ChangeWarBase.changeWarBaseLayout(InGamesVars.currentAccountNumber)

            // 每周精选 / 商人购买（T08）：TRADER_ENABLED 开启且本账号配置了商品开关时执行
            Trader.purchaseWeeklySelection(InGamesVars.currentAccountNumber)

            // 部落战进攻（T09）：CLAN_WAR_ENABLED 开启且有可用进攻机会时执行（下兵复用主世界）
            ClanWar.clanWarAttack(InGamesVars.currentAccountNumber)

            // 联赛(CWL)进攻（T10）：LEAGUE_WAR_ENABLED 开启且有可用进攻机会时执行（下兵复用主世界）
            League.leagueAttack(InGamesVars.currentAccountNumber)

            if (!playBuilderBase()) {
                ShowMessage("夜世界对战完成，准备进入主世界")
                return@stepBlock
            }
            if (!playMainBase()) {
                ShowMessage("主世界对战完成，准备切换账号")
                return@stepBlock
            }
            // 竞赛阶段（T07）：接夜世界竞赛任务 + 领竞赛奖励，由 do_clan_games / claim_clan_game_rewards 控制。
            // 竞赛是附加功能：内部任何一步拿不准都自行跳过，不参与 stepBlock 的中断判定。
            playClanGames()
            // 都城阶段（M4-③）：领都城币 / 捐都城币 / 发起突袭 / 打突袭，由各自的配置开关控制。
            // 约定与 playBuilderBase 一致：返回 false = 本阶段结束（无配置或无目标时也会立刻返回）。
            if (!playClanCapital()) {
                ShowMessage("都城流程完成，准备切换账号")
                return@stepBlock
            }
        }
        // Circularly search for the next enabled account, wrapping back to currentAccountNumber (inclusive)
        // Emit hot update signal at safe point, throttled to once every 2-3 hours randomly
        if (updateOption == 2 && System.currentTimeMillis() - lastUpdateCheckTime >= nextUpdateInterval) {
            val deferred = CompletableDeferred<Unit>()
            GlobalVars.updateCheckSignal.tryEmit(deferred)
            deferred.await()
            lastUpdateCheckTime = System.currentTimeMillis()
            nextUpdateInterval = (2 * 3600_000L) + (Math.random() * 3600_000L).toLong()
        }
        val searchOrder = ((InGamesVars.currentAccountNumber + 1)..accountTotal) + (accountStart..InGamesVars.currentAccountNumber)
        val nextAccount = findAndActivateAccount(searchOrder, isBatchCreate) ?: return

        InGamesVars.currentAccountNumber = nextAccount
        // 切换账号后镜头状态未知（换号=重进游戏），必须整体复位，避免沿用上一个账号的
        // "已缩小"标记而漏掉第一次缩小。
        CameraState.reset()
        writeMemory(StorageKeys.ACCOUNT_NUMBER, nextAccount.toString())
        // In batch-create mode, force GLOBAL version for all accounts
        InGamesVars.currentGameVersion = if (isBatchCreate) {
            GameVersion.GLOBAL
        } else {
            GameVersion.fromId(
                getConfigOrStop("${Schema.ACCOUNT_SETTINGS.GAME_VERSION.key}${InGamesVars.currentAccountNumber}").toIntOrNull()
                    ?: logAndRestart("${Schema.ACCOUNT_SETTINGS.GAME_VERSION.displayName} 必须是数字，请检查配置")
            )
        }
    }
}

/**
 * 临时标定（2026-09-27 删兵重造功能）：走到进攻确认层 → 点"编辑都城军队"打开面板 → 截图存档后停止。
 * 不点进攻，不消耗突袭次数。用于标定面板上的"删除(垃圾桶)/保存"按钮特征。
 */
/**
 * 临时调试（2026-09-27）：都城友谊战下兵测试。
 * 不消耗突袭次数：用户在部落聊天手动发起都城友谊战（红框卡片），脚本点击红「进攻」进入战场后部署军队，
 * 用于标定/验证都城部署颜色（CapitalDeployColors）与下兵逻辑。
 * 若用户已手动点过进攻、当前就在战场，则跳过点击直接部署。
 * 调试完成后还原为原测试代码（upgradeGearsAndPets 循环）。
 */
private suspend fun runTestCode() {
    playCapitalFriendlyChallenge()
    ShowMessage("都城友谊战下兵测试结束，停止脚本")
    SceneState.setRunControl(GameRunControl.STOPPED)
}

/**
 * Searches [searchOrder] for the first enabled account.
 * When [skipIsOpenCheck] is true, all accounts are treated as active (used for batch-create mode).
 * If none is found, loops showing a message until the coroutine is canceled,
 * then returns null — the caller should return immediately on null.
 */
private suspend fun findAndActivateAccount(
    searchOrder: Iterable<Int>, skipIsOpenCheck: Boolean = false
): Int? {
    // In batch-create mode, treat all accounts as active
    val found = if (skipIsOpenCheck) {
        searchOrder.firstOrNull()
    } else {
        searchOrder.firstOrNull { id ->
            getConfigOrStop("${Schema.ACCOUNT_SETTINGS.ISOPEN.key}$id") == "1"
        }
    }
    if (found == null) {
        while (currentCoroutineContext().isActive) {
            ShowMessage("当前未开启任何账号\n请勾选要开启的账号。")
            delay(2500)
        }
    }
    return found
}


