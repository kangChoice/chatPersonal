package com.needai.chat.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.needai.chat.data.local.db.entity.SessionEntity
import com.needai.chat.domain.model.ChatSession

object SessionMapper {
    private val gson = Gson()

    fun toDomain(
        entity: SessionEntity,
        skillName: String,
        skillAvatar: String,
        messageCount: Int
    ): ChatSession {
        val skillIds: List<String> = if (!entity.skillIds.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(entity.skillIds, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        return ChatSession(
            id = entity.id,
            skillId = entity.skillId,
            type = entity.type,
            skillIds = skillIds,
            skillName = skillName,
            skillAvatar = skillAvatar,
            title = entity.title,
            messageCount = messageCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(session: ChatSession): SessionEntity {
        return SessionEntity(
            id = session.id,
            skillId = session.skillId,
            type = session.type,
            skillIds = if (session.skillIds.isNotEmpty()) gson.toJson(session.skillIds) else null,
            title = session.title,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt
        )
    }
}
