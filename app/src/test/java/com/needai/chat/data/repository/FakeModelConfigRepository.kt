package com.needai.chat.data.repository

import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.repository.ModelConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeModelConfigRepository : ModelConfigRepository {

    private val _config = MutableStateFlow(
        ModelConfig(
            modelType = ModelType.REMOTE,
            remoteBaseUrl = "https://api.deepseek.com",
            remoteApiKey = "test-key",
            remoteModelName = "deepseek-v4-flash",
            temperature = 0.7,
            maxTokens = 4096,
            topP = 1.0
        )
    )

    override fun getModelConfig(): Flow<ModelConfig> = _config

    override suspend fun saveModelConfig(config: ModelConfig) {
        _config.value = config
    }
}
