package com.needai.chat.domain.model

data class ModelConfig(
    val id: String = "",
    val name: String = "",
    val modelType: ModelType = ModelType.REMOTE,
    val protocol: ApiProtocol = ApiProtocol.OPENAI,
    val remoteBaseUrl: String = "",
    val remoteApiKey: String = "",
    val remoteModelName: String = "",
    val localBaseUrl: String = "",
    val localModelName: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val topP: Double = 1.0
)
