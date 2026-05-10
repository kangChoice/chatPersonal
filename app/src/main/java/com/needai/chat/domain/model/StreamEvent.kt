package com.needai.chat.domain.model

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class Done(
        val promptTokens: Int? = null,
        val completionTokens: Int? = null,
        val totalTokens: Int? = null
    ) : StreamEvent()
}
