package com.needai.chat.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.needai.chat.data.local.datastore.AiNotificationManager
import com.needai.chat.data.remote.client.RemoteModelClient
import com.needai.chat.domain.model.AiNotificationConfig
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.usecase.ChatMessage
import com.needai.chat.util.FileLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * AlarmManager 30 秒闹钟接收器。
 *
 * 内联处理到期任务（AI 调用 + 发送通知），不依赖前台 Service，
 * 避免与 IlinkBridgeService 共享 dataSync FGS 时间预算导致的崩溃。
 */
class AlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlarmReceiverEntryPoint {
        val skillRepository: SkillRepository
        val modelConfigRepository: ModelConfigRepository
        val notificationHelper: AppNotificationHelper
        val aiNotificationManager: AiNotificationManager
    }

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "闹钟触发")
        val pendingResult = goAsync()

        Thread {
            try {
                AiNotificationScheduler.reschedule(context)

                val entryPoint = EntryPointAccessors.fromApplication(
                    context, AlarmReceiverEntryPoint::class.java
                )
                val manager = entryPoint.aiNotificationManager
                val skillRepo = entryPoint.skillRepository
                val modelConfigRepo = entryPoint.modelConfigRepository
                val notifHelper = entryPoint.notificationHelper

                val allConfigs = runBlocking { manager.getAll() }
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

                if (dueConfigs.isEmpty()) {
                    Log.d(TAG, "当前窗口无到期任务 (now=$now, enabled=${configs.size})")
                    return@Thread
                }

                Log.d(TAG, "共 ${dueConfigs.size} 个到期任务，开始内联处理")
                FileLogger.i(TAG, "共 ${dueConfigs.size} 个到期任务，开始内联处理")

                for (config in dueConfigs) {
                    try {
                        processConfig(config, skillRepo, modelConfigRepo, notifHelper, manager)
                    } catch (e: Exception) {
                        Log.w(TAG, "处理任务失败: ${config.skillName}, ${e.localizedMessage}", e)
                        FileLogger.w(TAG, "处理任务失败: ${config.skillName}, ${e.localizedMessage}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "检查异常: ${e.localizedMessage}", e)
                FileLogger.w(TAG, "检查异常: ${e.localizedMessage}")
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun processConfig(
        config: AiNotificationConfig,
        skillRepo: SkillRepository,
        modelConfigRepo: ModelConfigRepository,
        notifHelper: AppNotificationHelper,
        manager: AiNotificationManager
    ) {
        runBlocking {
            Log.d(TAG, "处理任务: skill=${config.skillName}, prompt=${config.prompt}")
            FileLogger.i(TAG, "处理任务: skill=${config.skillName}, time=${String.format("%02d:%02d", config.hour, config.minute)}")

            val skill = skillRepo.getSkillById(config.skillId)
            if (skill == null) {
                Log.w(TAG, "角色 ${config.skillId} 不存在")
                notifHelper.showAiNotification(config.skillName, "角色已不存在，请更新定时任务配置", config.id.hashCode())
                return@runBlocking
            }

            val modelConfig = modelConfigRepo.getModelConfig().first()
            if (modelConfig.remoteApiKey.isBlank()) {
                Log.w(TAG, "API Key 为空")
                notifHelper.showAiNotification(config.skillName, "定时消息生成失败：请先配置 API Key", config.id.hashCode())
                return@runBlocking
            }

            val modelClient = RemoteModelClient(com.google.gson.Gson())
            val messages = listOf(
                ChatMessage(role = "system", content = skill.systemPrompt),
                ChatMessage(role = "user", content = config.prompt)
            )

            val result = modelClient.chatNonStreaming(messages, modelConfig, skill)

            if (result.isFailure) {
                Log.w(TAG, "AI 调用失败: ${result.exceptionOrNull()?.localizedMessage}")
                notifHelper.showAiNotification(config.skillName, "消息生成失败，请检查网络和 API Key 配置", config.id.hashCode())
                return@runBlocking
            }

            val text = result.getOrThrow().trim()
            Log.d(TAG, "发送通知: skill=${config.skillName}, len=${text.length}")
            notifHelper.showAiNotification(config.skillName, text, config.id.hashCode())
            FileLogger.i(TAG, "通知已发送: skill=${config.skillName}, len=${text.length}")

            manager.update(config.copy(lastTriggeredAt = System.currentTimeMillis()))
            delay(1000)
        }
    }

    private fun triggeredToday(config: AiNotificationConfig, today: LocalDate): Boolean {
        val last = config.lastTriggeredAt ?: return false
        val lastDate = Instant.ofEpochMilli(last)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return lastDate == today
    }
}
