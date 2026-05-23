package com.needai.chat.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.needai.chat.data.local.datastore.AiNotificationManager
import com.needai.chat.util.FileLogger
import com.needai.chat.domain.model.AiNotificationConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 定时通知调度器：按需精准调度 + WorkManager 15 分钟安全网。
 *
 * - 每次处理完通知后扫描所有启用配置，找出下一个最近的时间点
 * - setAlarmClock（目标时间 + 1 分钟），Android 最高优先级闹钟
 * - reschedule 时回看 5 分钟窗口，遗漏任务立即触发
 * - 无前台 Service、不占用 dataSync 预算
 */
@Singleton
class AiNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RescheduleEntryPoint {
        val aiNotificationManager: AiNotificationManager
    }

    companion object {
        private const val TAG = "AISchedule:Sched"
        private const val PERIODIC_WORK_NAME = "ai_notification_fallback"
        private const val CHECK_INTERVAL_MINUTES = 15L
        private const val ALARM_REQUEST_CODE = 9001

        /** 计算下一个通知时间并注册 setAlarmClock */
        fun reschedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context, RescheduleEntryPoint::class.java
                )
                val manager = entryPoint.aiNotificationManager

                if (!runBlocking { manager.isGlobalEnabled() }) {
                    Log.d(TAG, "AI 通知全局关闭，取消闹钟")
                    alarmManager.cancel(pendingIntent)
                    return
                }

                val configs = runBlocking { manager.getAll().filter { it.enabled } }

                if (configs.isEmpty()) {
                    Log.d(TAG, "无启用的定时任务，取消闹钟")
                    alarmManager.cancel(pendingIntent)
                    return
                }

                val now = LocalTime.now()
                val today = LocalDate.now()
                val currentMinutes = now.hour * 60 + now.minute

                // 先检查过去5分钟内是否有遗漏任务
                val hasMissed = configs.any { config ->
                    val scheduledMinutes = config.hour * 60 + config.minute
                    val diff = currentMinutes - scheduledMinutes
                    diff in 0..5 && !triggeredToday(config, today)
                }

                val triggerTimeMs: Long
                if (hasMissed) {
                    triggerTimeMs = System.currentTimeMillis() + 1000
                    Log.d(TAG, "发现遗漏任务（5分钟窗口内），立即触发")
                    FileLogger.i(TAG, "reschedule: immediate trigger, hasMissed=true")
                } else {
                    // 找到下一个未来时间点
                    var nextMinutes: Int? = null
                    for (config in configs) {
                        val scheduledMinutes = config.hour * 60 + config.minute
                        if (scheduledMinutes > currentMinutes) {
                            if (nextMinutes == null || scheduledMinutes < nextMinutes) {
                                nextMinutes = scheduledMinutes
                            }
                        }
                    }
                    if (nextMinutes == null) {
                        nextMinutes = configs.minOf { it.hour * 60 + it.minute } + 24 * 60
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
            } catch (e: Exception) {
                Log.w(TAG, "reschedule 异常: ${e.localizedMessage}")
            }
        }

        private fun triggeredToday(config: AiNotificationConfig, today: LocalDate): Boolean {
            val last = config.lastTriggeredAt ?: return false
            val lastDate = Instant.ofEpochMilli(last)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            return lastDate == today
        }

        private fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private val workManager = WorkManager.getInstance(context)

    fun start() {
        workManager.cancelUniqueWork("ai_notification_check")
        workManager.cancelUniqueWork("ai_notification_exact")

        reschedule(context)
        schedulePeriodicFallback()
    }

    fun stop() {
        cancelAlarm(context)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    private fun schedulePeriodicFallback() {
        val workRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(
            CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
