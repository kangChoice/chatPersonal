package com.needai.chat.domain.model

data class ModelConfig(
    val modelType: ModelType = ModelType.REMOTE,
    val remoteBaseUrl: String = "https://api.deepseek.com",
    val remoteApiKey: String = "sk-043367a2c3e64df9977d0d3062b08887",
    val remoteModelName: String = "deepseek-v4-flash",
    val localBaseUrl: String = "http://localhost:11434",
    val localModelName: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val topP: Double = 1.0
)
