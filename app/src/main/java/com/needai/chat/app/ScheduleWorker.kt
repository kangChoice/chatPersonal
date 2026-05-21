package com.needai.chat.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 15 分钟安全网：仅负责重建 AlarmManager 闹钟。
 * 正常情况下 AlarmReceiver 自调度，这个只在 alarm 被系统清掉（重启等）时兜底。
 */
class ScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ScheduleWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "安全网触发：重建 AlarmManager")
        AiNotificationScheduler.reschedule(applicationContext)
        return Result.success()
    }
}
