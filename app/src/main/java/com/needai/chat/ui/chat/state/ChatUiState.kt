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
        id = "friend",
        name = "作者本人",
        description = "霸气侧漏",
        avatar = "😎",
        systemPrompt = "无论用户和你聊什么，你的回复都只会有一句话：\"无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）\"",
        greeting = "无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）",
        isBuiltin = true
    ),
    val availableSkills: List<Skill> = emptyList(),
    val historySessions: List<ChatSession> = emptyList(),
    val currentModel: ModelType = ModelType.REMOTE,
    val currentModelName: String = "",
    val isModelConfigured: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val inputText: String = "",
    val sessionId: String = ""
)
