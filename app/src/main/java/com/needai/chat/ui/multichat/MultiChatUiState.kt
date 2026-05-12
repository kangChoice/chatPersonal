package com.needai.chat.ui.multichat

import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.Skill

data class MultiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String = "",
    val skillId: String? = null,
    val skillName: String? = null,
    val skillAvatar: String? = null,
    val isStreaming: Boolean = false,
    val dbId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MultiChatUiState(
    val messages: List<MultiChatMessage> = emptyList(),
    val selectedSkills: List<Skill> = emptyList(),
    val availableSkills: List<Skill> = emptyList(),
    val isGenerating: Boolean = false,
    val currentRespondingSkill: Skill? = null,
    val currentStreamingContent: String = "",
    val inputText: String = "",
    val multiPrompt: String = "你们都喜欢我，互相反驳对方的话，会争风吃醋",
    val showSkillSelector: Boolean = false,
    val showPromptEditor: Boolean = false,
    val error: String? = null,
    val sessionId: String = "",
    val historySessions: List<ChatSession> = emptyList()
)
