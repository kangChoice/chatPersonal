package com.needai.chat.domain.repository

import com.needai.chat.domain.model.ModelConfig
import kotlinx.coroutines.flow.Flow

interface ModelConfigRepository {
    fun getModelConfig(): Flow<ModelConfig>
    suspend fun saveModelConfig(config: ModelConfig)
}
