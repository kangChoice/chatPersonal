package com.needai.chat.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AnthropicStreamEvent(
    val type: String? = null,
    val delta: AnthropicDelta? = null,
    @SerializedName("content_block")
    val contentBlock: AnthropicContentBlock? = null
)

data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null
)

data class AnthropicContentBlock(
    val type: String? = null,
    val text: String? = null
)

// 非流式响应
data class AnthropicNonStreamResponse(
    val content: List<AnthropicContentBlock>? = null,
    val usage: AnthropicUsage? = null
)

data class AnthropicUsage(
    @SerializedName("input_tokens") val inputTokens: Int? = null,
    @SerializedName("output_tokens") val outputTokens: Int? = null
)
