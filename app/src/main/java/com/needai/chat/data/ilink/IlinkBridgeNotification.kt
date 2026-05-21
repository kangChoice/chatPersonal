package com.needai.chat.data.ilink

import android.app.Notification
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
class IlinkBridgeNotification @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "ilink_bridge"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ClawBot 桥接",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "微信 ClawBot 连接状态通知"
            setShowBadge(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun buildConnected(skillName: String): Notification {
        val openIntent = Intent(context, com.needai.chat.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClawBot 已连接")
            .setContentText("角色：$skillName")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .build()
    }

    fun buildConnecting(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClawBot 连接中")
            .setContentText("正在等待微信消息...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    fun buildAuthPolling(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("等待微信授权")
            .setContentText("请在微信中确认授权...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    fun buildError(error: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ClawBot 连接断开")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(false)
            .build()
    }
}
