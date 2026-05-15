package com.needai.chat.data.repository

import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType
import com.needai.chat.domain.repository.ModelConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

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

    private val _configs = MutableStateFlow(listOf(
        ModelConfig(
            id = "test-1",
            name = "测试配置",
            modelType = ModelType.REMOTE,
            remoteBaseUrl = "https://api.deepseek.com",
            remoteApiKey = "test-key",
            remoteModelName = "deepseek-v4-flash",
            temperature = 0.7,
            maxTokens = 4096,
            topP = 1.0
        )
    ))

    private val _selectedConfigId = MutableStateFlow("test-1")

    override fun getModelConfig(): Flow<ModelConfig> = _config

    override fun getAllConfigs(): Flow<List<ModelConfig>> = _configs

    override suspend fun getConfigById(id: String): ModelConfig? {
        return _configs.value.find { it.id == id }
    }

    override suspend fun saveModelConfig(config: ModelConfig) {
        _config.value = config
        val existing = _configs.value.indexOfFirst { it.id == config.id }
        if (existing >= 0) {
            _configs.value = _configs.value.toMutableList().also { it[existing] = config }
        } else {
            _configs.value = _configs.value + config
        }
    }

    override suspend fun deleteConfig(id: String) {
        _configs.value = _configs.value.filter { it.id != id }
    }

    override suspend fun getSelectedConfigId(): String = _selectedConfigId.value

    override suspend fun setSelectedConfigId(id: String) {
        _selectedConfigId.value = id
    }
}
