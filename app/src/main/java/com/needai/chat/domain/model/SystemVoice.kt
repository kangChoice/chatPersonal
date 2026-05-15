package com.needai.chat.domain.model

data class SystemVoice(
    val voiceId: String,
    val displayName: String,
    val description: String,
    val supportedModels: List<String> = emptyList()
)
