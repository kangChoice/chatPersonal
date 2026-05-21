package com.needai.chat.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.needai.chat.data.local.datastore.AiNotificationManager
import com.needai.chat.util.FileLogger
import java.time.LocalDate
import java.time.LocalTime

/**
 * AlarmManager 30 秒闹钟接收器。
 *
 * 职责：读取已持久化的定时任务配置，找出 5 分钟窗口内到期且未被触发过的任务，
 * 交给 ScheduleNotificationService 处理。
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "闹钟触发")
        val pendingResult = goAsync()

        Thread {
            try {
                // 立即预订下一次闹钟
                AiNotificationScheduler.reschedule(context)

                val manager = AiNotificationManager(context, Gson())
                val allConfigs = kotlinx.coroutines.runBlocking { manager.getAll() }
                val configs = allConfigs.filter { it.enabled }

                if (configs.isEmpty()) {
                    Log.d(TAG, "无启用的定时任务，跳过")
                    return@Thread
                }

                val now = LocalTime.now()
                val today = LocalDate.now()
                val currentMinutes = now.hour * 60 + now.minute

                val dueConfigs = configs.filter { config ->
                    val scheduledMinutes = config.hour * 60 + config.minute
                    val diff = currentMinutes - scheduledMinutes
                    val due = diff in 0..5 && !triggeredToday(config, today)
                    if (due) {
                        Log.d(TAG, "到期任务: ${config.skillName} ${String.format("%02d:%02d", config.hour, config.minute)} (diff=${diff}min)")
                    }
                    due
                }

                if (dueConfigs.isNotEmpty()) {
                    Log.d(TAG, "共 ${dueConfigs.size} 个到期任务，启动 Service")
                    FileLogger.i(TAG, "共 ${dueConfigs.size} 个到期任务，启动 Service")
                    val ids = dueConfigs.map { it.id }.toTypedArray()
                    val serviceIntent = Intent(context, ScheduleNotificationService::class.java).apply {
                        putExtra(ScheduleNotificationService.EXTRA_CONFIG_IDS, ids)
                    }
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.d(TAG, "当前窗口无到期任务 (now=$now, enabled=${configs.size})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "检查异常: ${e.localizedMessage}", e)
                FileLogger.w(TAG, "检查异常: ${e.localizedMessage}")
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun triggeredToday(config: com.needai.chat.domain.model.AiNotificationConfig, today: LocalDate): Boolean {
        val last = config.lastTriggeredAt ?: return false
        val lastDate = java.time.Instant.ofEpochMilli(last)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        return lastDate == today
    }
}
