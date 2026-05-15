package com.needai.chat.domain.model

data class VoiceInfo(
    val voiceId: String,
    val displayName: String = "",
    val voicePrompt: String = "",
    val targetModel: String = "",
    val status: String = "",
    val previewText: String = "",
    val gmtCreate: String = "",
    val gmtModified: String = ""
)
