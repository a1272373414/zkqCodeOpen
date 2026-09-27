package com.coc.zkqcode.jar.ui.schema

import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.data.websocket.ServerActions
import com.coc.zkqcode.statehelper.AppMode
import com.coc.zkqcode.statehelper.AppStateManager

object ConfigManager {

    /**
     * Initializes all config states from the provided ServerActions.
     */
    fun initializeAllConfigs(actions: ServerActions) {
        val accountCount = actions.getValue("account_count")?.toIntOrNull() ?: 3
        val configCount = actions.getValue("config_count")?.toIntOrNull() ?: 3

        SchemaRegistry.ALL_MODULES.forEach { module ->
            when (module.scope) {
                Scope.GLOBAL -> {
                    module.settings.forEach { def ->
                        val savedValue = actions.getValue(def.key)
                        GlobalVars.configStates.getOrPut(def.key) {
                            mutableStateOf(savedValue ?: def.defaultValue.toString())
                        }.value = savedValue ?: def.defaultValue.toString()
                    }
                }

                Scope.ACCOUNT -> {
                    for (i in 1..accountCount) {
                        module.settings.forEach { def ->
                            val key = "${def.key}${i}"
                            val savedValue = actions.getValue(key)
                            val defaultValue =
                                if (def.key == "global_path" || def.key == "cn_path" || def.key == "data_content") {
                                    i.toString()
                                } else {
                                    def.defaultValue.toString()
                                }
                            GlobalVars.configStates.getOrPut(key) {
                                mutableStateOf(savedValue ?: defaultValue)
                            }.value = savedValue ?: defaultValue
                        }
                    }
                }

                Scope.PROFILE -> {
                    for (i in 1..configCount) {
                        module.settings.forEach { def ->
                            val key = "${def.key}_c$i"
                            val savedValue = actions.getValue(key)
                            GlobalVars.configStates.getOrPut(key) {
                                mutableStateOf(savedValue ?: def.defaultValue.toString())
                            }.value = savedValue ?: def.defaultValue.toString()
                        }
                    }
                }
            }
        }
    }

    fun expandAccountConfigs(newCount: Int) {
        SchemaRegistry.ALL_MODULES.filter { it.scope == Scope.ACCOUNT }.forEach { module ->
            for (i in 1..newCount) {
                module.settings.forEach { def ->
                    val key = "${def.key}${i}"
                    if (!GlobalVars.configStates.containsKey(key)) {
                        val defaultValue =
                            if (def.key == "global_path" || def.key == "cn_path" || def.key == "data_content") {
                                i.toString()
                            } else {
                                def.defaultValue.toString()
                            }
                        GlobalVars.configStates[key] = mutableStateOf(defaultValue)
                    }
                }
            }
        }
    }

    fun expandProfileConfigs(newCount: Int) {
        SchemaRegistry.ALL_MODULES.filter { it.scope == Scope.PROFILE }.forEach { module ->
            for (i in 1..newCount) {
                module.settings.forEach { def ->
                    val key = "${def.key}_c$i"
                    if (!GlobalVars.configStates.containsKey(key)) {
                        GlobalVars.configStates[key] = mutableStateOf(def.defaultValue.toString())
                    }
                }
            }
        }
    }

    /**
     * 保存所有配置到 JSON 文件并通知服务器更新。
     *
     * 保存失败不应阻塞辅助运行：UI 当前内存中的 [GlobalVars.configStates] 已经包含最新值，
     * runBot 读取的也是内存状态。因此即使落盘/通知服务器异常，仍要强制切到 Run 并关闭配置窗口，
     * 避免用户点了「保存并运行」后悬浮窗卡死在前台。
     */
    suspend fun saveConfigs(onSaveSuccess: () -> Unit = {}) {
        try {
            val baseDir = "${Environment.getExternalStorageDirectory().path}/zkqFiles/"
            val accountCountStr = GlobalVars.configStates["account_count"]?.value ?: "3"
            val configCountStr = GlobalVars.configStates["config_count"]?.value ?: "3"

            val accountCount = accountCountStr.toIntOrNull() ?: 3
            val configCount = configCountStr.toIntOrNull() ?: 3

            SchemaExporter.saveSchemaViaServer(
                baseDir,
                "zkq_config.json",
                accountCount = accountCount,
                configCount = configCount
            )
        } catch (e: Exception) {
            // 仅打印异常，不阻塞后续切 Run / 关闭窗口
            e.printStackTrace()
        }

        GlobalVars.updateWindowPosition = true
        AppStateManager.setMode(AppMode.Run)
        // 关闭配置悬浮窗，露出游戏界面；控制悬浮窗随后会由 handleUIIClose 拉起。
        onSaveSuccess()
    }

    /**
     * Save configs and run the bot.
     */
    suspend fun saveAndRun(onSaveSuccess: () -> Unit = {}) {
        saveConfigs(onSaveSuccess)
    }
}