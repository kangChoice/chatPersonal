package com.needai.chat.data.mapper

import com.needai.chat.data.local.db.entity.NotificationTemplateEntity
import com.needai.chat.domain.model.NotificationTemplate

object NotificationTemplateMapper {
    fun toDomain(entity: NotificationTemplateEntity): NotificationTemplate {
        return NotificationTemplate(
            id = entity.id,
            label = entity.label,
            prompt = entity.prompt,
            isBuiltin = entity.isBuiltin,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(domain: NotificationTemplate): NotificationTemplateEntity {
        return NotificationTemplateEntity(
            id = domain.id,
            label = domain.label,
            prompt = domain.prompt,
            isBuiltin = domain.isBuiltin,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
