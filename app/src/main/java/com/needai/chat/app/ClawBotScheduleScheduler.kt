package com.needai.chat.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkScheduleManager
import com.needai.chat.util.FileLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ClawBot 定时发送闹钟调度器：按需精准调度 + WorkManager 15 分钟安全网。
 *
 * - 从 IlinkScheduleManager 读取定时配置，计算下一个发送时间
 * - setAlarmClock（目标时间 + 10 秒），Android 最高优先级闹钟
 * - start() 时 PendingIntent 触发一次立即检查，让 checkAndSend 自身去重
 * - 与 IlinkBridgeService 的长轮询完全解耦，App 被杀也不影响发送
 */
@Singleton
class ClawBotScheduleScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RescheduleEntryPoint {
        val authManager: IlinkAuthManager
        val scheduleManager: IlinkScheduleManager
    }

    companion object {
        private const val TAG = "WxSchedule:Sched"
        private const val PERIODIC_WORK_NAME = "clawbot_schedule_fallback"
        private const val CHECK_INTERVAL_MINUTES = 15L
        private const val ALARM_REQUEST_CODE = 9002

        /** 计算下一个发送时间并注册 setAlarmClock */
        fun reschedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClawBotScheduleReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context, RescheduleEntryPoint::class.java
                )
                val authManager = entryPoint.authManager
                val scheduleManager = entryPoint.scheduleManager

                runBlocking {
                    val token = authManager.getToken()
                    if (token == null) {
                        Log.d(TAG, "Token 为空，取消闹钟")
                        alarmManager.cancel(pendingIntent)
                        return@runBlocking
                    }

                    scheduleManager.initialize()
                    val config = scheduleManager.getConfig()
                    val fixedMessages = config.fixedMessages.filter {
                        it.time.isNotBlank() && it.message.isNotBlank()
                    }

                    val fixedEnabled = config.fixedEnabled && fixedMessages.isNotEmpty()
                    val hasAnySchedule = fixedEnabled || config.randomEnabled

                    if (!hasAnySchedule) {
                        Log.d(TAG, "无启用的定时配置，取消闹钟")
                        alarmManager.cancel(pendingIntent)
                        return@runBlocking
                    }

                    val now = LocalTime.now()
                    val currentMinutes = now.hour * 60 + now.minute

                    // 收集所有定时时间点
                    val allScheduledMinutes = mutableListOf<Int>()
                    if (fixedEnabled) {
                        for (item in fixedMessages) {
                            val parts = item.time.split(":")
                            val minutes = parts[0].toIntOrNull()?.let { h ->
                                parts.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m }
                            } ?: continue
                            allScheduledMinutes.add(minutes)
                        }
                    }
                    if (config.randomEnabled) {
                        for (time in scheduleManager.getTodayRandomTimes()) {
                            val parts = time.split(":")
                            val minutes = parts[0].toIntOrNull()?.let { h ->
                                parts.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m }
                            } ?: continue
                            allScheduledMinutes.add(minutes)
                        }
                    }

                    // 先检查过去5分钟内是否有遗漏任务（已发送的不会重复触发）
                    val hasMissed = (fixedEnabled && fixedMessages.any { item ->
                        val parts = item.time.split(":")
                        val minutes = parts[0].toIntOrNull()?.let { h ->
                            parts.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m }
                        } ?: 0
                        val diff = currentMinutes - minutes
                        diff in 0..5 && !scheduleManager.isFixedSent(item.time)
                    }) || (config.randomEnabled && scheduleManager.getTodayRandomTimes().any { time ->
                        val parts = time.split(":")
                        val minutes = parts[0].toIntOrNull()?.let { h ->
                            parts.getOrNull(1)?.toIntOrNull()?.let { m -> h * 60 + m }
                        } ?: 0
                        val diff = currentMinutes - minutes
                        diff in 0..5 && !scheduleManager.isRandomSent(time)
                    })

                    val triggerTimeMs: Long
                    if (hasMissed) {
                        triggerTimeMs = System.currentTimeMillis() + 1000
                        Log.d(TAG, "发现遗漏任务（5分钟窗口内），立即触发")
                        FileLogger.i(TAG, "reschedule: immediate trigger, hasMissed=true")
                    } else {
                        // 找到下一个未来时间点
                        var nextMinutes: Int? = null
                        for (minutes in allScheduledMinutes) {
                            if (minutes > currentMinutes) {
                                if (nextMinutes == null || minutes < nextMinutes) {
                                    nextMinutes = minutes
                                }
                            }
                        }
                        if (nextMinutes == null) {
                            nextMinutes = (allScheduledMinutes.minOrNull() ?: 0) + 24 * 60
                        }

                        val nowEpochMs = System.currentTimeMillis()
                        val todayStartMs = nowEpochMs - (currentMinutes * 60_000L)
                        triggerTimeMs = todayStartMs + (nextMinutes * 60_000L) + 10_000L
                        val displayMinutes = nextMinutes % (24 * 60)
                        Log.d(TAG, "下一次闹钟: ${displayMinutes / 60}:${String.format("%02d", displayMinutes % 60)}")
                        FileLogger.i(TAG, "reschedule: 下一次闹钟=${displayMinutes / 60}:${String.format("%02d", displayMinutes % 60)}, triggerTimeMs=$triggerTimeMs, hasMissed=false")
                    }

                    val showIntent = PendingIntent.getActivity(
                        context, ALARM_REQUEST_CODE + 1000,
                        Intent(context, com.needai.chat.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerTimeMs, showIntent),
                            pendingIntent
                        )
                        FileLogger.d(TAG, "reschedule: 使用精确闹钟")
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                        Log.d(TAG, "无精确闹钟权限，使用普通闹钟替代")
                        FileLogger.w(TAG, "reschedule: 无精确闹钟权限，降级为普通闹钟")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "reschedule 异常: ${e.localizedMessage}")
            }
        }

        private fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClawBotScheduleReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private val workManager = WorkManager.getInstance(context)

    fun start() {
        reschedule(context)
        schedulePeriodicFallback()
        // 启动时触发一次立即检查，让 checkAndSend 自身去重（不依赖 reschedule 的 hasMissed）
        triggerImmediateCheck()
    }

    fun stop() {
        cancelAlarm(context)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** PendingIntent 触发一次 Receiver，让 checkAndSend 用 sentFixed/sentRandom 自身判断 */
    private fun triggerImmediateCheck() {
        try {
            val intent = Intent(context, ClawBotScheduleReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE + 5000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            pi.send()
            Log.d(TAG, "启动时立即检查已触发")
        } catch (e: Exception) {
            Log.w(TAG, "立即检查触发失败: ${e.localizedMessage}")
        }
    }

    private fun schedulePeriodicFallback() {
        val workRequest = PeriodicWorkRequestBuilder<ClawBotScheduleWorker>(
            CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
