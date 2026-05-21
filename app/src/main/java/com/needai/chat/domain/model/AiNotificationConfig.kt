package com.needai.chat.domain.model

data class AiNotificationConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val skillId: String,
    val skillName: String,
    val skillAvatar: String,
    val prompt: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
)
