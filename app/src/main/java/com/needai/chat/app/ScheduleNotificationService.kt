package com.needai.chat.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.needai.chat.data.local.datastore.AiNotificationManager
import com.needai.chat.data.remote.client.RemoteModelClient
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SessionRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.usecase.ChatMessage
import com.needai.chat.util.FileLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * AI 定时通知手动测试 Service。
 *
 * 仅由用户点击"测试发送"按钮触发（前台环境，无需 FGS）。
 * 正常定时触发由 AlarmReceiver 内联处理，不经过此 Service。
 */
@AndroidEntryPoint
class ScheduleNotificationService : Service() {

    @Inject lateinit var skillRepository: SkillRepository
    @Inject lateinit var modelConfigRepository: ModelConfigRepository
    @Inject lateinit var notificationHelper: AppNotificationHelper
    @Inject lateinit var aiNotificationManager: AiNotificationManager
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var sessionRepository: SessionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null

    companion object {
        private const val TAG = "ScheduleNotifSvc"
        const val EXTRA_FORCE_ALL = "force_all"
        const val EXTRA_CONFIG_IDS = "config_ids"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val forceAll = intent?.getBooleanExtra(EXTRA_FORCE_ALL, false) ?: false
        val configIds = intent?.getStringArrayExtra(EXTRA_CONFIG_IDS)?.toSet()

        if (!forceAll && (configIds == null || configIds.isEmpty())) {
            stopSelfSafe()
            return START_NOT_STICKY
        }

        processingJob = scope.launch {
            try {
                val configs = aiNotificationManager.getAll().filter { it.enabled }
                val targets = when {
                    forceAll -> configs
                    else -> configs.filter { c -> configIds?.contains(c.id) == true }
                }

                for (config in targets) {
                    processConfig(config)
                }
            } catch (e: Exception) {
                Log.w(TAG, "处理异常: ${e.localizedMessage}", e)
                FileLogger.w(TAG, "处理异常: ${e.localizedMessage}")
            } finally {
                stopSelfSafe()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processConfig(config: com.needai.chat.domain.model.AiNotificationConfig) {
        Log.d(TAG, "处理任务: skill=${config.skillName}, prompt=${config.prompt}")
        FileLogger.i(TAG, "处理任务: skill=${config.skillName}, time=${String.format("%02d:%02d", config.hour, config.minute)}")

        val skill = skillRepository.getSkillById(config.skillId)
        if (skill == null) {
            Log.w(TAG, "角色 ${config.skillId} 不存在")
            notificationHelper.showAiNotification(config.skillName, "角色已不存在，请更新定时任务配置", config.id.hashCode(), config.skillId)
            return
        }

        val modelConfig = modelConfigRepository.getModelConfig().first()
        if (modelConfig.remoteApiKey.isBlank()) {
            Log.w(TAG, "API Key 为空")
            notificationHelper.showAiNotification(config.skillName, "定时消息生成失败：请先配置 API Key", config.id.hashCode(), config.skillId)
            return
        }

        val modelClient = RemoteModelClient(com.google.gson.Gson())
        val messages = listOf(
            ChatMessage(role = "system", content = skill.systemPrompt),
            ChatMessage(role = "user", content = config.prompt)
        )

        val result = modelClient.chatNonStreaming(messages, modelConfig, skill)

        if (result.isFailure) {
            Log.w(TAG, "AI 调用失败: ${result.exceptionOrNull()?.localizedMessage}")
            notificationHelper.showAiNotification(config.skillName, "消息生成失败，请检查网络和 API Key 配置", config.id.hashCode(), config.skillId)
            return
        }

        val text = result.getOrThrow().trim()
        Log.d(TAG, "发送通知: skill=${config.skillName}, len=${text.length}")

        // 找到或创建该技能的会话，插入 ASSISTANT 消息
        val sessions = sessionRepository.getSessionsBySkillId(config.skillId, "single")
        val latestSession = sessions.maxByOrNull { it.updatedAt }
        val sessionId = latestSession?.id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        chatRepository.insertMessage(
            Message(
                sessionId = sessionId,
                role = MessageRole.ASSISTANT,
                content = text,
                skillId = config.skillId,
                timestamp = now,
                isRead = false
            )
        )
        sessionRepository.saveSession(
            ChatSession(
                id = sessionId,
                skillId = config.skillId,
                skillName = config.skillName,
                skillAvatar = config.skillAvatar,
                title = config.prompt.take(50).replace("\n", " "),
                messageCount = (latestSession?.messageCount ?: 0) + 1,
                createdAt = latestSession?.createdAt ?: now,
                updatedAt = now
            )
        )
        FileLogger.i(TAG, "聊天记录已插入: sessionId=${sessionId.take(8)}, skill=${config.skillName}")

        notificationHelper.showAiNotification(config.skillName, text, config.id.hashCode(), config.skillId)
        FileLogger.i(TAG, "通知已发送: skill=${config.skillName}, len=${text.length}")

        aiNotificationManager.update(config.copy(lastTriggeredAt = System.currentTimeMillis()))
        delay(2000)
    }

    private fun stopSelfSafe() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        processingJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
