package com.coc.zkqcode.jar.code.universal.deploy

import com.coc.zkqcode.core.util.basic.ShowMessage
import com.coc.zkqcode.jar.code.universal.smalltools.getStaticConfig

/**
 * Deploy debug logging gate.
 *
 * Controlled by the global setting `deploy_debug` ("下兵调试日志", HomeScreen switch). When off,
 * nothing is logged (keeping the normal run clean); when on, the deploy pipeline prints every
 * decision so a stuck / wrong placement can be located quickly.
 *
 * The switch is read live on every call, so toggling it takes effect without a rebuild.
 */
object DeployDebug {
    private const val KEY = "deploy_debug"

    /** True when the "下兵调试日志" global switch is on. */
    fun enabled(): Boolean = runCatching { getStaticConfig(KEY) == "1" }.getOrDefault(false)

    /** Logs [message] (prefixed) only when the debug switch is on. */
    fun log(message: String) {
        if (enabled()) ShowMessage("【下兵】$message")
    }
}
