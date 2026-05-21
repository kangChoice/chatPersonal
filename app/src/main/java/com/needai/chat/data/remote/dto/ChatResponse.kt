package com.needai.chat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatStreamChunk(
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

data class Choice(
    val delta: Delta,
    val index: Int = 0,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class Delta(
    val role: String? = null,
    val content: String? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerializedName("completion_tokens")
    val completionTokens: Int? = null,
    @SerializedName("total_tokens")
    val totalTokens: Int? = null
)

data class ErrorResponse(
    val error: ErrorDetail? = null
)

data class ErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

// 非流式响应
data class ChatNonStreamResponse(
    val choices: List<NonStreamChoice>? = null,
    val usage: Usage? = null
)

data class NonStreamChoice(
    val message: NonStreamMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class NonStreamMessage(
    val role: String? = null,
    val content: String? = null
)
