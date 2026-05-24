package com.needai.chat.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d(TAG, "设备启动完成，重建闹钟")
        try {
            AiNotificationScheduler.reschedule(context)
        } catch (e: Exception) {
            Log.w(TAG, "AI通知闹钟重建失败: ${e.localizedMessage}")
        }
        try {
            ClawBotScheduleScheduler.reschedule(context)
        } catch (e: Exception) {
            Log.w(TAG, "ClawBot闹钟重建失败: ${e.localizedMessage}")
        }
    }
}
