package com.needai.chat.ui.schedule

import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.needai.chat.ui.theme.BrandBlue
import com.needai.chat.ui.theme.StatusRed
import com.needai.chat.ui.theme.TextPrimary
import com.needai.chat.ui.theme.TextSecondary
import com.needai.chat.util.AutoStartHelper

@Composable
fun SchedulePermissionCards() {
    val context = LocalContext.current
    val canScheduleExactAlarms = remember {
        val am = context.getSystemService(AlarmManager::class.java)
        am.canScheduleExactAlarms()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 精确闹钟权限
        PermissionCard(
            title = "请检查精确闹钟权限是否开启",
            subtitle = if (canScheduleExactAlarms) "当前已开启：定时任务将准时触发" else "当前未开启，定时任务可能延迟",
            buttonText = "去设置",
            onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )

        // 省电白名单
        PermissionCard(
            title = "省电白名单建议",
            subtitle = "将本应用加入省电白名单，避免后台被系统限制导致定时任务失效",
            buttonText = "去设置",
            onClick = {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        )

        // 自启动设置（仅国产 ROM 显示）
        if (AutoStartHelper.needsAutoStartGuide()) {
            PermissionCard(
                title = "请检查是否开启自启动",
                subtitle = "国产 ROM 在应用被划掉后会取消闹钟，必须开启自启动才能保证定时任务正常运行",
                buttonText = "去设置",
                accentColor = StatusRed,
                onClick = { AutoStartHelper.openAutoStartSettings(context) }
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    buttonText: String,
    accentColor: androidx.compose.ui.graphics.Color = BrandBlue,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            TextButton(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}
