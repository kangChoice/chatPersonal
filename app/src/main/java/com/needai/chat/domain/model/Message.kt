package com.needai.chat.domain.model

data class Message(
    val id: Long = 0,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val skillId: String? = null,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
