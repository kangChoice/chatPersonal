package com.needai.chat.domain.model

data class ChatSession(
    val id: String,
    val skillId: String,
    val skillName: String,
    val skillAvatar: String,
    val title: String,
    val messageCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
