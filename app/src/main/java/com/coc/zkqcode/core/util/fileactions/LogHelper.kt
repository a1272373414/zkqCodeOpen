package com.coc.zkqcode.core.util.fileactions

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.coc.zkqcode.BuildConfig
import com.coc.zkqcode.core.util.basic.ShowMessage
import com.topjohnwu.superuser.Shell
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object LogHelper {
    // Re-entry guard to prevent recursive calls (e.g. ShowMessage -> logAndRestart -> ShowMessage)
    private val isRestarting = AtomicBoolean(false)

    // 每个日志文件保留的最大行数（滚动覆盖，仅保留最近 N 行）
    // 临时调高到 2000：夜世界下兵诊断现在每轮打 3 行（卡扫描/选中卡/落点），
    // 一场约 200 行，500 行会把前一场的关键部分滚掉，不利于排查。
    private const val MAX_LOG_LINES = 2000

    fun initTimber(context: Context) {
        if (BuildConfig.DEBUG) {
            // 测试版：logcat + 文件全量(含详细 VERBOSE)，便于排查细节
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(context, Log.VERBOSE))
        } else {
            // 正式版：仅记录运行日志(>=INFO)与异常(WARN/ERROR)，不记录详细排错日志，避免文件过大
            Timber.plant(FileLoggingTree(context, Log.INFO))
        }
    }

    fun logAndRestart(message: String): Nothing {
        // Prevent re-entrant calls; only the first caller proceeds with the restart sequence
        if (!isRestarting.compareAndSet(false, true)) {
            error("logAndRestart re-entered: $message")
        }
        Timber.tag("zkq_debug").e("CRITICAL_ERROR: $message")
        repeat(3) {
            try {
                ShowMessage("出现未知错误，即将尝试重启。\n注意：请检查辅助配置，确保除了部落标签和暗号以外，其他所有的输入框都不能为空。\n并且该填数字的地方就要填数字，该填文字的地方填文字，不能乱填。\n若辅助配置没问题，则请截图该错误信息向作者反馈。\n\n错误信息：\n$message")
            } catch (e: Exception) {
                Timber.tag("zkq_debug").e("ShowMessage failed during logAndRestart: ${e.message}")
            }
            Thread.sleep(1000)
        }
        // Spawn a detached process via setsid to restart the app after killing it.
        // The new session ensures this child survives the parent process termination.
        Shell.cmd(
            "setsid sh -c 'sleep 2; am force-stop com.coc.zkqcode; sleep 1; am start -n com.coc.zkqcode/.MainActivity' > /dev/null 2>&1 &"
        ).exec()
        error(message)
    }

    fun showDebugInfo(message: String) {
        Timber.tag("zkq_debug").d("Debug info: $message")
    }

    class FileLoggingTree(private val context: Context, private val minPriority: Int = Log.VERBOSE) : Timber.Tree() {
        @SuppressLint("LogNotTimber")
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // 低于最低记录级别的日志直接丢弃（正式版借此过滤详细排错日志）
            if (priority < minPriority) return

            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) logDir.mkdirs()

            val fileName = if (priority >= Log.ERROR) "error.log" else "info.log"
            val logFile = File(logDir, fileName)

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logEntry = "$timestamp [$tag] $message\n"

            try {
                // Append log entry
                FileOutputStream(logFile, true).use { fos ->
                    fos.write(logEntry.toByteArray())
                }

                // Maintain MAX_LOG_LINES limit for each file (only keep the most recent N lines)
                synchronized(this) {
                    val lines = logFile.readLines()
                    if (lines.size > MAX_LOG_LINES) {
                        val trimmedLines = lines.takeLast(MAX_LOG_LINES)
                        logFile.writeText(trimmedLines.joinToString("\n") + "\n")
                    }
                }
            } catch (e: Exception) {
                // Use standard Log to avoid infinite recursion if Timber fails
                Log.e("FileLoggingTree", "Error writing to $fileName", e)
            }
        }
    }
}
