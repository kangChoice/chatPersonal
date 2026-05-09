package com.needai.chat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,
    @SerializedName("top_p")
    val topP: Double = 1.0
)

data class ChatMessageDto(
    val role: String,
    val content: String
)
