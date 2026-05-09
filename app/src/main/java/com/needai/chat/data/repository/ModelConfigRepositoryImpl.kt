package com.needai.chat.data.repository

import com.needai.chat.data.local.datastore.ModelConfigDataStore
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.repository.ModelConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelConfigRepositoryImpl @Inject constructor(
    private val modelConfigDataStore: ModelConfigDataStore
) : ModelConfigRepository {

    override fun getModelConfig(): Flow<ModelConfig> {
        return modelConfigDataStore.modelConfig
    }

    override suspend fun saveModelConfig(config: ModelConfig) {
        modelConfigDataStore.saveModelConfig(config)
    }
}
