package com.needai.chat.domain.repository

import com.needai.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow

interface ModelConfigRepository {
    fun getAllConfigs(): Flow<List<ModelConfig>>
    fun getModelConfig(): Flow<ModelConfig>
    suspend fun getConfigById(id: String): ModelConfig?
    suspend fun saveModelConfig(config: ModelConfig)
    suspend fun deleteConfig(id: String)
    suspend fun getSelectedConfigId(): String
    suspend fun setSelectedConfigId(id: String)
}
