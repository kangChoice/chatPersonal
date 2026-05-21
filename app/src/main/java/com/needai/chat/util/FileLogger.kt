package com.needai.chat.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileLogger {

    private const val MAX_LOG_AGE_DAYS = 3
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024L

    enum class Level { DEBUG, INFO, WARN, ERROR }

    private data class LogEntry(
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
        val timestamp: Long
    )

    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private var logDir: File? = null
    private var scope: CoroutineScope? = null
    private var isInitialized = false
    private var currentLogFile: File? = null
    private var currentLogDate: String? = null

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        logDir = File(context.getExternalFilesDir(null), "logs")
        logDir?.mkdirs()

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope?.launch {
            for (entry in logChannel) {
                writeToFile(entry)
            }
        }
        scope?.launch { cleanupOldLogs() }

        Log.i("FileLogger", "日志目录: ${logDir?.absolutePath}")
    }

    fun d(tag: String, msg: String) { enqueue(Level.DEBUG, tag, msg, null) }
    fun i(tag: String, msg: String) { enqueue(Level.INFO, tag, msg, null) }
    fun w(tag: String, msg: String) { enqueue(Level.WARN, tag, msg, null) }
    fun e(tag: String, msg: String, throwable: Throwable? = null) { enqueue(Level.ERROR, tag, msg, throwable) }

    private fun enqueue(level: Level, tag: String, message: String, throwable: Throwable?) {
        logChannel.trySend(LogEntry(level, tag, message, throwable, System.currentTimeMillis()))
        // mirror to logcat
        when (level) {
            Level.DEBUG -> Log.d(tag, message)
            Level.INFO -> Log.i(tag, message)
            Level.WARN -> Log.w(tag, message)
            Level.ERROR -> Log.e(tag, message, throwable)
        }
    }

    private fun writeToFile(entry: LogEntry) {
        val dir = logDir ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        val dateStr = dateFormat.format(Date(entry.timestamp))

        // 跨天或首次写入 → 确定当天的基��文件
        if (dateStr != currentLogDate) {
            currentLogDate = dateStr
            currentLogFile = File(dir, "$dateStr.log")
        }

        // 当前文件超限 → 轮转到下一个序列号
        if (currentLogFile!!.exists() && currentLogFile!!.length() > MAX_LOG_FILE_SIZE) {
            var seq = 1
            var next: File
            do {
                next = File(dir, "$dateStr.$seq.log")
                seq++
            } while (next.exists())
            currentLogFile = next
        }

        val targetFile = currentLogFile!!

        val timestamp = timeFormat.format(Date(entry.timestamp))
        val levelChar = entry.level.name.first()
        val logLine = buildString {
            append("[$timestamp] [$levelChar/${entry.level.name}] [${entry.tag}] ${entry.message}")
            entry.throwable?.let {
                append(System.lineSeparator())
                append(Log.getStackTraceString(it))
            }
            append(System.lineSeparator())
        }

        try {
            targetFile.appendText(logLine)
        } catch (e: Exception) {
            Log.e("FileLogger", "写入日志文件失败", e)
        }
    }

    private suspend fun cleanupOldLogs() {
        val dir = logDir ?: return
        try {
            val cutoff = System.currentTimeMillis() - MAX_LOG_AGE_DAYS * 24 * 60 * 60 * 1000L
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("FileLogger", "清理旧日志失败", e)
        }
    }

    fun getLogFiles(): List<File> =
        logDir?.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun getLogDirectory(): File? = logDir

    fun shutdown() {
        scope?.cancel()
        isInitialized = false
    }
}
