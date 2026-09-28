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

    /**
     * 日志按**小时**切分：`info_yyyyMMdd_HH.log` / `error_yyyyMMdd_HH.log`。
     *
     * 为什么是小时：复查问题时通常是"昨天下午 3 点左右出错"，按小时定位最直观；
     * 保留两天也只有 48 个文件。10 分钟切分会有 288 个、5 分钟 576 个，
     * 文件过多且文件名不直观，排查时反而是负担。
     */
    private const val LOG_RETENTION_HOURS = 48L

    /** 单个日志文件超过该大小就分片（_part2/_part3），防止 VERBOSE 全量时单小时日志过大。 */
    private const val MAX_LOG_FILE_BYTES = 2 * 1024 * 1024L

    /** 兜底清理检查间隔：日志稀疏（长时间没跨小时）时也要能清掉过期文件。 */
    private const val CLEANUP_INTERVAL_MS = 30 * 60 * 1000L

    /** 清理过期日志时的锁，避免与写入并发产生竞争。 */
    private val cleanupLock = Any()

    /** 保证 Timber 只种一次：App 进程里 MainActivity 与 DaemonService 都会调 [initTimber]，
     *  重复 plant FileLoggingTree 会导致**每条日志被写两遍**（实测日志成对重复）。 */
    private val timberPlanted = AtomicBoolean(false)

    fun initTimber(context: Context) {
        if (!timberPlanted.compareAndSet(false, true)) return
        // 启动时先清一次过期日志，避免历史文件堆积
        cleanupExpiredLogs(context)
        if (BuildConfig.DEBUG) {
            // 测试版：logcat + 文件全量(含详细 VERBOSE)，便于排查细节
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(context, Log.VERBOSE))
        } else {
            // 正式版：仅记录运行日志(>=INFO)与异常(WARN/ERROR)，不记录详细排错日志，避免文件过大
            Timber.plant(FileLoggingTree(context, Log.INFO))
        }
    }

    /**
     * 删除超过 [LOG_RETENTION_HOURS] 小时的日志文件（按**文件名里的时间戳**判断，
     * 不依赖文件修改时间，避免拷贝/回写导致误判）。
     *
     * 调用时机：App 启动时（[initTimber]）+ 跨小时切文件时 + 每 [CLEANUP_INTERVAL_MS] 兜底检查。
     */
    fun cleanupExpiredLogs(context: Context) {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) return
        val cutoff = System.currentTimeMillis() - LOG_RETENTION_HOURS * 3600_000L
        synchronized(cleanupLock) {
            lastCleanupAt = System.currentTimeMillis()
            val files = dir.listFiles() ?: return
            for (file in files) {
                // 优先按文件名里的时间戳判断；旧版 `info.log` 之类解析不出时间的，
                // 退化为按最后修改时间判断，避免历史文件永久残留
                val fileTime = parseFileHourMillis(file.name) ?: file.lastModified().takeIf { it > 0 }
                if (fileTime == null) continue
                if (fileTime < cutoff) {
                    runCatching { file.delete() }
                }
            }
        }
    }

    /** 最近一次执行清理的时间戳。 */
    @Volatile
    private var lastCleanupAt = 0L

    /**
     * 从日志文件名解析出该文件的小时起始时间（毫秒）。
     * 支持 `info_20260929_13.log` / `info_20260929_13_part2.log`；旧版 `info.log` 返回 null（不处理）。
     */
    private fun parseFileHourMillis(fileName: String): Long? {
        if (!fileName.endsWith(".log")) return null
        val parts = fileName.removeSuffix(".log").split("_")
        if (parts.size < 3) return null
        return try {
            SimpleDateFormat("yyyyMMdd_HH", Locale.US).parse(parts[1] + "_" + parts[2])?.time
        } catch (e: Exception) {
            null
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

    /**
     * 文件日志：按**小时**切分文件，单文件超限分片，只保留最近 [LOG_RETENTION_HOURS] 小时。
     *
     * 相比旧实现（单文件 + 每条日志都全文件 readLines/writeText 做行数裁剪），
     * 这里改为**保持打开的流顺序追加**，不再每条都重写整个文件，写入开销大幅下降。
     */
    class FileLoggingTree(private val context: Context, private val minPriority: Int = Log.VERBOSE) : Timber.Tree() {

        private val lock = Any()
        private var currentHour = ""
        private var currentBase = ""
        private var partIndex = 0
        private var currentFile: File? = null
        private var currentStream: FileOutputStream? = null
        private var currentBytes = 0L

        private val hourFormat = SimpleDateFormat("yyyyMMdd_HH", Locale.US)
        private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        @SuppressLint("LogNotTimber")
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // 低于最低记录级别的日志直接丢弃（正式版借此过滤详细排错日志）
            if (priority < minPriority) return

            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) logDir.mkdirs()

            val base = if (priority >= Log.ERROR) "error" else "info"
            val hour = hourFormat.format(Date())
            val timestamp = timeFormat.format(Date())
            val entry = buildString {
                append(timestamp).append(" [").append(tag).append("] ").append(message).append('\n')
                if (t != null) append(Log.getStackTraceString(t)).append('\n')
            }
            val bytes = entry.toByteArray()

            synchronized(lock) {
                // 跨小时 / 切换 info↔error → 切到新文件（分片序号归零）
                if (hour != currentHour || base != currentBase || currentStream == null) {
                    partIndex = 0
                    rotate(logDir, base, hour)
                } else if (currentBytes >= MAX_LOG_FILE_BYTES) {
                    // 单文件超限 → 分片，避免单个文件过大不好打开
                    partIndex++
                    rotate(logDir, base, hour)
                }

                // 定期清理（跨小时时必清一次，日志稀疏时靠间隔兜底）。
                // FileLoggingTree 是嵌套类，访问外层对象成员需加 LogHelper. 限定符。
                if (System.currentTimeMillis() - LogHelper.lastCleanupAt > LogHelper.CLEANUP_INTERVAL_MS) {
                    LogHelper.cleanupExpiredLogs(context)
                }

                try {
                    currentStream?.write(bytes)
                    currentBytes += bytes.size
                } catch (e: Exception) {
                    // Use standard Log to avoid infinite recursion if Timber fails
                    Log.e("FileLoggingTree", "Error writing to ${currentFile?.name}", e)
                }
            }
        }

        /** 关闭当前流并按 (base, hour, 分片序号) 打开新文件。 */
        private fun rotate(dir: File, base: String, hour: String) {
            try {
                currentStream?.close()
            } catch (e: Exception) {
                Log.e("FileLoggingTree", "Error closing ${currentFile?.name}", e)
            }
            val name = if (partIndex == 0) "${base}_${hour}.log" else "${base}_${hour}_part${partIndex}.log"
            val file = File(dir, name)
            currentFile = file
            currentBytes = if (file.exists()) file.length() else 0L
            currentStream = try {
                FileOutputStream(file, true)
            } catch (e: Exception) {
                Log.e("FileLoggingTree", "Error opening $name", e)
                null
            }
            currentHour = hour
            currentBase = base
        }
    }
}
