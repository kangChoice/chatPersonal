package com.needai.chat.domain.model

data class Message(
    val id: Long = 0,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val skillId: String? = null,
    val timestamp: Long,
    val isStreaming: Boolean = false,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val modelConfigId: String? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
