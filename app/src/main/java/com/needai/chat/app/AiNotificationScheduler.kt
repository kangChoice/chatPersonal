package com.needai.chat.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 定时通知调度器：AlarmManager 30 秒精确闹钟 + WorkManager 15 分钟安全网。
 *
 * - AlarmManager 每 30 秒唤醒 AlarmReceiver 检查一次
 * - alarm 被系统清掉（如重启）后，WorkManager 周期性兜底重建 alarm
 * - 无前台 Service、不占用 dataSync 预算
 */
@Singleton
class AiNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PERIODIC_WORK_NAME = "ai_notification_fallback"
        private const val CHECK_INTERVAL_MINUTES = 15L
        private const val ALARM_INTERVAL_MS = 30_000L
        private const val ALARM_REQUEST_CODE = 9001

        /** 预订下一次闹钟（静态方法，供 AlarmReceiver 自调度） */
        fun reschedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_INTERVAL_MS,
                pendingIntent
            )
        }
    }

    private val workManager = WorkManager.getInstance(context)

    fun start() {
        // 清理旧版残留
        workManager.cancelUniqueWork("ai_notification_check")
        workManager.cancelUniqueWork("ai_notification_exact")

        reschedule(context)
        schedulePeriodicFallback()
    }

    fun stop() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
