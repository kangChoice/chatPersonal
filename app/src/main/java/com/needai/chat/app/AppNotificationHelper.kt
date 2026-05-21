package com.needai.chat.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.needai.chat.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val AI_NOTIFICATION_CHANNEL = "ai_notification"
        private const val AI_PROGRESS_CHANNEL = "ai_notification_progress"
        const val MESSAGE_NOTIFICATION_ID = 2001
    }

    init {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messageChannel = NotificationChannel(
            AI_NOTIFICATION_CHANNEL,
            "AI 定时通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "AI 角色定时生成的消息通知"
            setShowBadge(true)
        }
        manager.createNotificationChannel(messageChannel)

        val progressChannel = NotificationChannel(
            AI_PROGRESS_CHANNEL,
            "AI 通知运行状态",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "AI 定时通知处理中的进度提示"
            setShowBadge(false)
        }
        manager.createNotificationChannel(progressChannel)
    }

    fun showAiNotification(title: String, content: String) {
        showAiNotification(title, content, MESSAGE_NOTIFICATION_ID)
    }

    fun showAiNotification(title: String, content: String, notificationId: Int) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AI_NOTIFICATION_CHANNEL)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun buildProgressNotification(title: String, text: String = "正在生成消息..."): android.app.Notification {
        return NotificationCompat.Builder(context, AI_PROGRESS_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    fun updateProgressNotification(text: String) {
        val notification = buildProgressNotification("AI 定时通知", text)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2002, notification)
    }
}
