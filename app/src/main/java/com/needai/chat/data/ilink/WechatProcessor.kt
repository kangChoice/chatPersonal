package com.needai.chat.data.ilink

import com.needai.chat.data.remote.client.ModelClient
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.usecase.ChatMessage
import com.needai.chat.util.FileLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 微信 ClawBot 消息处理接口。
 *
 * 与 ChatViewModel 完全解耦：
 * - 不走 TTS 音色合成
 * - 不写 Room 消息表
 * - 不更新 UI 状态
 * - 不触发 Compose 重组
 *
 * 纯文本入 → 纯文本出。
 */
@Singleton
class WechatProcessor @Inject constructor(
    private val modelClient: ModelClient,
    private val skillRepository: SkillRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val authManager: IlinkAuthManager
) {
    private val TAG = "WechatProcessor"

    data class ProcessResult(
        val text: String,
        val error: String? = null
    )

    /**
     * 处理用户发来的微信消息。
     *
     * @param text 用户消息文本
     * @return 回复文本，失败时返回错误描述
     */
    suspend fun process(text: String): ProcessResult {
        FileLogger.i(TAG, "process: text=${text.take(100)}")

        // 1. 获取 ClawBot 独立的角色（未设置则回退聊天页选中角色）
        val ilinkId = authManager.getIlinkSkillId()
        val skillId = if (!ilinkId.isNullOrBlank()) ilinkId else skillRepository.getSelectedSkillId()
        val skill = skillRepository.getSkillById(skillId)
        if (skill == null) {
            FileLogger.w(TAG, "未选中角色, skillId=$skillId")
            return ProcessResult(text = "", error = "未选中角色，请在 App 中选择一个角色")
        }
        FileLogger.i(TAG, "使用角色: ${skill.name}")

        // 2. 获取当前模型配置
        val configId = modelConfigRepository.getSelectedConfigId()
        val config = modelConfigRepository.getConfigById(configId)
        if (config == null) {
            FileLogger.w(TAG, "未配置模型, configId=$configId")
            return ProcessResult(text = "", error = "未配置 AI 模型，请在设置中完成配置")
        }

        // 3. 构建消息列表
        val messages = buildList {
            if (skill.systemPrompt.isNotBlank()) {
                add(ChatMessage(role = "system", content = skill.systemPrompt))
            }
            add(ChatMessage(role = "user", content = text))
        }

        // 4. 调用 AI（聚合流式响应）
        return try {
            FileLogger.i(TAG, "开始调用 AI, model=${config.remoteModelName}")
            val fullContent = StringBuilder()
            val events = modelClient.streamChat(
                messages = messages,
                config = config,
                skill = skill
            ).toList()

            for (event in events) {
                when (event) {
                    is StreamEvent.Token -> fullContent.append(event.text)
                    is StreamEvent.Done -> { /* 完成 */ }
                }
            }

            val reply = fullContent.toString()
            FileLogger.i(TAG, "AI 回复完成, 长度=${reply.length}: ${reply.take(20)}")
            if (reply.isBlank()) {
                ProcessResult(text = "", error = "AI 返回了空回复")
            } else {
                ProcessResult(text = reply)
            }
        } catch (e: Exception) {
            FileLogger.e(TAG, "AI 调用异常", e)
            ProcessResult(text = "", error = "AI 调用失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }
}
