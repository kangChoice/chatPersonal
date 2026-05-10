package com.needai.chat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null
)

data class AnthropicMessage(
    val role: String,
    val content: String
)
