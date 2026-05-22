package com.needai.chat.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 15 分钟安全网：仅负责重建 ClawBot AlarmManager 闹钟。
 * 正常情况下 ClawBotScheduleReceiver 自调度，这个只在 alarm 被系统清掉（重启等）时兜底。
 */
class ClawBotScheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WxSchedule:Fallback"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "安全网触发：重建 ClawBot AlarmManager")
        ClawBotScheduleScheduler.reschedule(applicationContext)
        return Result.success()
    }
}
