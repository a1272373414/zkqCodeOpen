package com.coc.zkqcode.jar.code.clancapital.attack

import android.graphics.Point
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.basic.delayWithMultiplier
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.colorschema.MyColors
import com.coc.zkqcode.jar.code.universal.GameVersion
import com.coc.zkqcode.jar.code.universal.InGamesVars
import com.coc.zkqcode.jar.code.universal.colors.findMultiColors

/**
 * 聊天界面「都城友谊战」识别与进入。
 *
 * 用途：用户手动在部落聊天里发起一场都城友谊战（不消耗突袭次数），本模块识别该卡片并点击
 * 红「进攻」按钮进入，从而用真实都城战场调试都城下兵 / 法术部署，无需消耗每周 5 次突袭机会。
 *
 * 识别依据（详见 [com.coc.zkqcode.jar.code.colorschema.colorpackage.clancapital.CapitalFriendlyChatColors]）：
 * 卡片底部「绿『侦察』 + 红『进攻』 + 蓝『详细信息』」三按钮组合。已攻破的友谊战（如法师山谷 100%）
 * 只有绿『回放』+ 蓝『详细信息』、没有红『进攻』，故本特征只命中【尚未进攻过】的都城友谊战卡片。
 */
object CapitalFriendlyChat {

    /**
     * 在聊天界面中查找「未进攻过的都城友谊战」卡片的红「进攻」按钮。
     * @return 命中时返回红按钮上的一点（落在按钮矩形内，可直接点击）；未命中返回 null。
     */
    suspend fun findCapitalFriendlyAttackButton(): Point? {
        return findMultiColors(MyColors.CapitalFriendlyAttackButton)
    }

    /**
     * 查找并点击都城友谊战的红「进攻」按钮，进入友谊战战场。
     * @return true=找到并点击了红「进攻」；false=未找到都城友谊战卡片（可能聊天未打开、或卡片已攻破）。
     */
    suspend fun enterCapitalFriendlyFromChat(): Boolean {
        val p = findCapitalFriendlyAttackButton() ?: run {
            ShowMessage("未找到都城友谊战卡片（红「进攻」按钮）")
            return false
        }
        // 命中点落在红按钮顶部（按钮内有白字，多色匹配只在文字外的填充区触发，返回点偏顶部）。
        // 下移 23px 落到红按钮竖直中心，点击即触发「进攻」。
        val tapX = p.x
        val tapY = p.y + 23
        ShowMessage("找到都城友谊战，点击进攻 @($tapX,$tapY)")
        TouchActions.tap(tapX, tapY)
        return true
    }

    /**
     * 都城友谊战完整流程：聊天卡片进入 → 确认弹窗点绿「进攻」→ 等战斗开始 → 部署军队。
     *
     * 用途：在【不消耗突袭次数】的前提下调试都城下兵 / 法术部署（[capitalDeployArmy]）。
     * 两种调用场景都兼容：
     *  - 聊天里友谊战卡片仍在：点击红「进攻」后，再点确认弹窗里的绿「进攻」真正进场。
     *  - 用户已手动点过进攻、当前就在战场：跳过点击，直接部署。
     *
     * 安全兜底：若既看不到聊天卡片、也看不到战场放弃按钮，说明仍在部落/主世界等无关界面，
     * 必须停止，避免在主世界上执行都城下兵逻辑（会报"第二轮仍无下兵点"等错误日志）。
     *
     * @param zoomOut 是否在下兵前缩小镜头（默认 false：友谊战首跑暂跳过战斗内缩放，待专项标定）。
     * @return true 表示至少成功部署了一个兵/法术或已进入战斗；false 表示未进入战斗，已安全停止。
     */
    suspend fun playCapitalFriendlyChallenge(zoomOut: Boolean = false): Boolean {
        // 自动点开聊天框：既不在战场、又没看到友谊战卡片时，点击部落聊天气泡打开聊天面板，
        // 让友谊战卡片进入可见区（对齐捐兵功能 DonateToClan 的打开聊天逻辑）。
        if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) == null
            && findCapitalFriendlyAttackButton() == null
        ) {
            val chatIcon = findMultiColors(MyColors.ClanChatIcon, increment = 1)
            if (chatIcon != null) {
                ShowMessage("都城友谊战：点击部落聊天气泡，自动打开聊天框")
                TouchActions.tap(chatIcon.x, chatIcon.y, delayTime = 500)
                // 聚焦聊天区并滚到底部，确保友谊战卡片进入可见区（坐标对齐捐兵逻辑）
                if (InGamesVars.currentGameVersion == GameVersion.CN) {
                    TouchActions.tap(523, 239, delayTime = 200)
                    TouchActions.tap(152, 88, delayTime = 200)
                } else {
                    TouchActions.tap(523, 98, delayTime = 300)
                }
                TouchActions.tap(40, 608, delayTime = 500)
                // 等聊天展开、友谊战卡片出现（最多 8 秒）
                val chatDeadline = System.currentTimeMillis() + 8_000L
                while (System.currentTimeMillis() < chatDeadline) {
                    if (findCapitalFriendlyAttackButton() != null) break
                    delayWithMultiplier(300)
                }
            } else {
                ShowMessage("都城友谊战：未找到部落聊天气泡，无法自动打开聊天框（请手动打开部落聊天）")
            }
        }
        if (findCapitalFriendlyAttackButton() != null) {
            ShowMessage("都城友谊战：点击红「进攻」打开确认弹窗")
            enterCapitalFriendlyFromChat()

            // 点击聊天红按钮后，游戏会弹出"要立即进攻吗？"确认窗，必须再点一次绿"进攻"才进战场。
            // 等待确认窗或战场放弃按钮出现（最多 15 秒）。
            val confirmDeadline = System.currentTimeMillis() + 15_000L
            var confirmReady: Point? = null
            while (System.currentTimeMillis() < confirmDeadline) {
                if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) != null) {
                    ShowMessage("都城友谊战：已进入战斗（放弃按钮出现）")
                    return capitalDeployArmy(null, zoomOut = zoomOut, requireGiveUp = false)
                }
                if (findMultiColors(schema = MyColors.CapitalAttackConfirm, increment = 1) != null) {
                    confirmReady = findMultiColors(schema = MyColors.CapitalAttackConfirmReady, increment = 1)
                    val confirmGray = findMultiColors(schema = MyColors.CapitalAttackConfirmGray, increment = 1) != null
                    val state = when {
                        confirmReady != null -> "进攻按钮可用（部队已满）"
                        confirmGray -> "进攻按钮灰色（部队未满，不可进攻）"
                        else -> "进攻按钮状态未识别"
                    }
                    if (confirmReady == null) {
                        ShowMessage("都城友谊战：确认弹窗状态异常（$state），停止测试")
                        return false
                    }
                    ShowMessage("都城友谊战：确认弹窗出现，点绿「进攻」真正进场（$state）")
                    TouchActions.tap(confirmReady.x, confirmReady.y, delayTime = 1500)
                    break
                }
                delayWithMultiplier(400)
            }
            if (confirmReady == null) {
                ShowMessage("都城友谊战：点击红「进攻」后未出现确认弹窗，停止测试")
                return false
            }

            // 点绿「进攻」后等待真正进入战场（放弃按钮出现）。
            val battleDeadline = System.currentTimeMillis() + 15_000L
            while (System.currentTimeMillis() < battleDeadline) {
                if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) != null) {
                    ShowMessage("都城友谊战：已进入战斗（放弃按钮出现）")
                    return capitalDeployArmy(null, zoomOut = zoomOut, requireGiveUp = false)
                }
                delayWithMultiplier(400)
            }
            ShowMessage("都城友谊战：点击绿「进攻」后未进入战场，停止测试")
            return false
        }

        // 卡片未找到时不再直接假设已进场，必须以战场 UI 放弃按钮佐证。
        // 若放弃按钮不可见，则当前很可能仍在主世界/部落聊天，继续下兵会误触无关界面。
        if (findMultiColors(schema = MyColors.GiveUpButton, increment = 1) == null) {
            ShowMessage("都城友谊战：未找到聊天卡片且不在战场，停止测试")
            return false
        }
        ShowMessage("都城友谊战：不在聊天界面但检测到战场，直接部署")
        return capitalDeployArmy(null, zoomOut = zoomOut, requireGiveUp = false)
    }
}

/**
 * 顶层入口（供 [com.coc.zkqcode.jar.code.MainScript] 直接 import 调用）：
 * 委托给 [CapitalFriendlyChat.playCapitalFriendlyChallenge]。
 */
suspend fun playCapitalFriendlyChallenge(zoomOut: Boolean = false): Boolean =
    CapitalFriendlyChat.playCapitalFriendlyChallenge(zoomOut)
