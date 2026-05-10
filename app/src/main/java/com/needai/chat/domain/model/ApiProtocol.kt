package com.needai.chat.domain.model

enum class ApiProtocol(val value: String) {
    OPENAI("openai"),
    ANTHROPIC("anthropic");

    companion object {
        fun fromValue(value: String): ApiProtocol {
            return entries.find { it.value == value } ?: OPENAI
        }
    }
}
