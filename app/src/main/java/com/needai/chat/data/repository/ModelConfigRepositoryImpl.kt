package com.needai.chat.data.repository

import com.needai.chat.data.local.db.dao.ModelConfigDao
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.mapper.ModelConfigMapper
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.repository.ModelConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelConfigRepositoryImpl @Inject constructor(
    private val modelConfigDao: ModelConfigDao,
    private val settingsDataStore: SettingsDataStore
) : ModelConfigRepository {

    companion object {
        private const val DEFAULT_CONFIG_ID = "default_config"
    }

    override fun getAllConfigs(): Flow<List<ModelConfig>> {
        return modelConfigDao.getAllConfigs().map { entities ->
            entities.map { ModelConfigMapper.toDomain(it) }
        }
    }

    override fun getModelConfig(): Flow<ModelConfig> {
        return settingsDataStore.selectedModelConfigId.flatMapLatest { selectedId ->
            val targetId = selectedId.ifEmpty { DEFAULT_CONFIG_ID }
            flow { emit(modelConfigDao.getConfigById(targetId)) }
        }.map { entity ->
            if (entity != null) ModelConfigMapper.toDomain(entity) else ModelConfig()
        }
    }

    override suspend fun getConfigById(id: String): ModelConfig? {
        return modelConfigDao.getConfigById(id)?.let { ModelConfigMapper.toDomain(it) }
    }

    override suspend fun saveModelConfig(config: ModelConfig) {
        val id = config.id.ifEmpty { UUID.randomUUID().toString() }
        val existing = modelConfigDao.getConfigById(id)
        val entity = if (existing != null) {
            ModelConfigMapper.toEntity(config.copy(id = id), existing)
        } else {
            ModelConfigMapper.toEntity(config.copy(id = id))
        }
        modelConfigDao.upsertConfig(entity)

        if (settingsDataStore.selectedModelConfigId.first().isEmpty()) {
            settingsDataStore.setSelectedModelConfigId(id)
        }
    }

    override suspend fun deleteConfig(id: String) {
        if (id == DEFAULT_CONFIG_ID) return
        modelConfigDao.deleteConfig(id)
        if (settingsDataStore.selectedModelConfigId.first() == id) {
            settingsDataStore.setSelectedModelConfigId("")
        }
    }

    override suspend fun getSelectedConfigId(): String {
        val id = settingsDataStore.selectedModelConfigId.first()
        if (id.isNotEmpty()) return id
        val configs = modelConfigDao.getAllConfigs().first()
        if (configs.isNotEmpty()) {
            settingsDataStore.setSelectedModelConfigId(configs.first().id)
            return configs.first().id
        }
        return ""
    }

    override suspend fun setSelectedConfigId(id: String) {
        settingsDataStore.setSelectedModelConfigId(id)
    }
}
