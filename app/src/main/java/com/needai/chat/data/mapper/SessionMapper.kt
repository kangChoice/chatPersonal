package com.needai.chat.data.mapper

import com.needai.chat.data.local.db.entity.SessionEntity
import com.needai.chat.domain.model.ChatSession

object SessionMapper {
    fun toDomain(
        entity: SessionEntity,
        skillName: String,
        skillAvatar: String,
        messageCount: Int
    ): ChatSession {
        return ChatSession(
            id = entity.id,
            skillId = entity.skillId,
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
            title = session.title,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt
        )
    }
}
