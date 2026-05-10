package com.needai.chat.data.mapper

import com.needai.chat.data.local.db.entity.ModelConfigEntity
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.ModelType

object ModelConfigMapper {

    fun toDomain(entity: ModelConfigEntity): ModelConfig {
        return ModelConfig(
            id = entity.id,
            name = entity.name,
            protocol = ApiProtocol.fromValue(entity.protocol),
            remoteBaseUrl = entity.remoteBaseUrl,
            remoteApiKey = entity.remoteApiKey,
            remoteModelName = entity.remoteModelName,
            temperature = entity.temperature,
            maxTokens = entity.maxTokens,
            topP = entity.topP,
            isBuiltin = entity.isBuiltin
        )
    }

    fun toEntity(config: ModelConfig): ModelConfigEntity {
        return ModelConfigEntity(
            id = config.id,
            name = config.name,
            protocol = config.protocol.value,
            remoteBaseUrl = config.remoteBaseUrl,
            remoteApiKey = config.remoteApiKey,
            remoteModelName = config.remoteModelName,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP,
            isBuiltin = config.isBuiltin,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun toEntity(config: ModelConfig, existing: ModelConfigEntity): ModelConfigEntity {
        return existing.copy(
            name = config.name,
            protocol = config.protocol.value,
            remoteBaseUrl = config.remoteBaseUrl,
            remoteApiKey = config.remoteApiKey,
            remoteModelName = config.remoteModelName,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP,
            updatedAt = System.currentTimeMillis()
        )
    }
}
