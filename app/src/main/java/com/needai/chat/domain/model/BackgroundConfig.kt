package com.needai.chat.domain.model

data class BackgroundConfig(
    val id: String,
    val name: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
