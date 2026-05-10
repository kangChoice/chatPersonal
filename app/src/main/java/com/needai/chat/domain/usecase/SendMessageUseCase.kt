package com.needai.chat.domain.usecase

import com.needai.chat.data.remote.client.ModelClient
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val skillRepository: SkillRepository,
    private val modelClient: ModelClient
) {
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String
    ): Flow<StreamEvent> {
        val config = modelConfigRepository.getModelConfig().let {
            var last: ModelConfig? = null
            it.collect { last = it }
            last!!
        }
        val skillId = skillRepository.getSelectedSkillId()
        val skill = skillRepository.getSkillById(skillId) ?: Skill(
            id = "default",
            name = "默认助手",
            description = "通用助手风格",
            avatar = "🤖",
            systemPrompt = "你是一个友好的AI助手，请用中文回答用户的问题。",
            greeting = "你好！我是你的AI助手，有什么可以帮你的？",
            isBuiltin = true
        )

        val messages = mutableListOf<ChatMessage>()

        // Add system prompt
        messages.add(ChatMessage(role = "system", content = skill.systemPrompt))

        // Add existing conversation
        chatRepository.getMessages(sessionId).let { flow ->
            var msgList: List<Message>? = null
            flow.collect { msgList = it }
            msgList?.forEach { msg ->
                val role = when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.SYSTEM -> "system"
                }
                messages.add(ChatMessage(role = role, content = msg.content))
            }
        }

        // Add current user message
        messages.add(ChatMessage(role = "user", content = userMessage))

        return modelClient.streamChat(
            messages = messages,
            config = config,
            skill = skill
        )
    }
}

data class ChatMessage(
    val role: String,
    val content: String
)
