package com.needai.chat.data.local.db.dao

data class TokenTotals(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)
