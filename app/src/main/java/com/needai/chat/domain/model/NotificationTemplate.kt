package com.needai.chat.domain.model

data class NotificationTemplate(
    val id: String = "",
    val label: String,
    val prompt: String,
    val isBuiltin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
