package com.coc.zkqcode.core.util.basic

import android.content.Context
import android.util.Log
import com.coc.zkqcode.core.data.database.GlobalVars
import com.coc.zkqcode.core.ui.floatingwindows.MessageBoxHelper.showFloatingMessage
import timber.log.Timber
import java.lang.ref.WeakReference

object ShowMessage {
    private var contextRef: WeakReference<Context>? = null
    private var lastMessage: String = ""
    private var lastShowTime: Long = 0L

    // 运行日志级别：在正式版(RELEASE)也会写入文件；仅供 run()/log() 系列方法内部使用
    const val RUN = Log.INFO

    fun init(context: Context) {
        this.contextRef = WeakReference(context.applicationContext)
    }

    fun getContext(): Context? = contextRef?.get()

    /**
     * 旧入口，保持与预编译 jar/历史代码的二进制兼容。
     * 旧代码调用 ShowMessage(text) 会走这里，默认写 VERBOSE 日志（测试版才落文件）。
     */
    operator fun invoke(text: String, isChecking: Boolean = true) {
        emit(text, isChecking, Log.VERBOSE)
    }

    /** 运行日志：正式版(RELEASE)也会写入文件，用于记录关键流程节点 */
    fun run(text: String, isChecking: Boolean = true) {
        emit(text, isChecking, Log.INFO)
    }

    /** 警告日志 */
    fun warn(text: String, isChecking: Boolean = true) {
        emit(text, isChecking, Log.WARN)
    }

    /** 错误日志 */
    fun error(text: String, isChecking: Boolean = true) {
        emit(text, isChecking, Log.ERROR)
    }

    /** 通用分级入口；如需指定非 INFO/WARN/ERROR 级别，可调用此方法 */
    fun log(text: String, isChecking: Boolean = true, level: Int = Log.VERBOSE) {
        emit(text, isChecking, level)
    }

    private fun emit(text: String, isChecking: Boolean, level: Int) {
        if (isChecking && !GlobalVars.isPlaying.value && !GlobalVars.isSwitchingAccount) {
            return//the user paused the script, then we should also stop
        }
        val now = System.currentTimeMillis()
        val elapsed = now - lastShowTime
        val shouldShow = elapsed >= 100 && !(text == lastMessage && elapsed < 500)

        lastMessage = text
        lastShowTime = now
        contextRef?.get()?.let { context ->
            // 节流与去重只作用于浮窗显示；Timber 始终按级别记录
            if (shouldShow) {
                showFloatingMessage(context = context, text = text)
            }
            // 按级别写入 Timber：正式版(RELEASE)文件树只保留 >=INFO，测试版(DEBUG)记录全部(含 VERBOSE)
            when {
                level >= Log.ERROR -> Timber.tag("zkq_debug").e(text)
                level >= Log.WARN -> Timber.tag("zkq_debug").w(text)
                level >= Log.INFO -> Timber.tag("zkq_debug").i(text)
                else -> Timber.tag("zkq_debug").v("Verbose: $text")
            }
        } ?: Timber.tag("zkq_debug").e("ShowMessage: Context not initialized or released, skipping message display")
    }
}