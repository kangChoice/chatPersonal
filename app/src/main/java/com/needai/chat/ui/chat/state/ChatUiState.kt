package com.needai.chat.ui.chat.state

import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.model.Skill

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentStreamingMessage: String = "",
    val isStreaming: Boolean = false,
    val currentSkill: Skill = Skill(
        id = "default",
        name = "默认助手",
        description = "通用AI助手",
        avatar = "🤖",
        systemPrompt = "你是一个友好的AI助手，请用中文回答用户的问题。",
        greeting = "你好！我是你的AI助手，有什么可以帮你的？",
        isBuiltin = true
    ),
    val availableSkills: List<Skill> = emptyList(),
    val historySessions: List<ChatSession> = emptyList(),
    val currentModel: ModelType = ModelType.REMOTE,
    val isModelConfigured: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val inputText: String = "",
    val sessionId: String = ""
)
