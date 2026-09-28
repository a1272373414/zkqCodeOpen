package com.coc.zkqcode.jar.code.mainbase.clan

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.core.util.touchactions.TouchActions
import com.coc.zkqcode.jar.code.universal.recognizer.ChineseTextReader
import com.coc.zkqcode.jar.code.universal.smalltools.StorageKeys
import com.coc.zkqcode.jar.code.universal.smalltools.readMemory
import kotlinx.coroutines.delay

/**
 * 审批入群申请（T40，源 函数287a L38656）。
 *
 * 源流程：进部落成员/申请页 → findMultiColorAll 找绿色「同意」按钮（32AD64）→ 逐个点同意，
 * 可选地按「审批暗号」校验昵称再同意。本项目简化：定位「审批/成员/申请」入口后，
 * 反复 [ChineseTextReader.locate]「同意」并点击（最多 [MAX_APPROVE] 次），不校验暗号。
 * 源「同意」绿色配色（32AD64）作注释兜底，待 T30 真机复标。
 *
 * 主开关 [StorageKeys.CLAN_APPROVE_ENABLED] 未开则跳过。
 */
object ClanApprove {

    private const val MAX_APPROVE = 20

    suspend fun approveClanRequests(account: Int): Boolean {
        if (readMemory(StorageKeys.withAccountNumber(StorageKeys.CLAN_APPROVE_ENABLED, account)) != "1") {
            return false
        }
        ShowMessage("账号$account，审批：开始")
        if (!enterClan()) {
            ShowMessage("账号$account，审批：未能进入部落")
            return false
        }
        // 进审批/成员/申请页：优先「审批」，其次「申请」，再次「成员」
        val entry = ChineseTextReader.locate("审批") ?: ChineseTextReader.locate("申请")
        ?: ChineseTextReader.locate("成员")
        if (entry != null) {
            TouchActions.tap(entry.centerX(), entry.centerY())
            delay(1500)
        }
        var approved = 0
        repeat(MAX_APPROVE) {
            val agree = ChineseTextReader.locate("同意") ?: return@repeat
            TouchActions.tap(agree.centerX(), agree.centerY())
            delay(700)
            approved++
        }
        ShowMessage("账号$account，审批：已同意 $approved 个申请")
        return approved > 0
    }

    private suspend fun enterClan(): Boolean {
        ChineseTextReader.locate("部落")?.let {
            TouchActions.tap(it.centerX(), it.centerY())
            delay(1500)
            return true
        }
        return false
    }
}
