package com.needai.chat.data.mapper

import com.needai.chat.data.local.db.entity.MessageEntity
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole

object MessageMapper {

    fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            sessionId = entity.sessionId,
            role = try {
                MessageRole.valueOf(entity.role)
            } catch (e: IllegalArgumentException) {
                MessageRole.USER
            },
            content = entity.content,
            skillId = entity.skillId,
            timestamp = entity.timestamp,
            isStreaming = entity.isStreaming
        )
    }

    fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            role = message.role.name,
            content = message.content,
            skillId = message.skillId,
            timestamp = message.timestamp,
            isStreaming = message.isStreaming
        )
    }
}
