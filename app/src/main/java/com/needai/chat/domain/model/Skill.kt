package com.needai.chat.domain.model

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val systemPrompt: String,
    val greeting: String,
    val temperature: Double = 0.7,
    val tags: List<String> = emptyList(),
    val isBuiltin: Boolean = false,
    val voiceId: String = "",
    val avatarPath: String = ""
)
