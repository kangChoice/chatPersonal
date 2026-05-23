package com.needai.chat.data.repository

import com.needai.chat.data.local.db.dao.NotificationTemplateDao
import com.needai.chat.data.mapper.NotificationTemplateMapper
import com.needai.chat.domain.model.NotificationTemplate
import com.needai.chat.domain.repository.NotificationTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTemplateRepositoryImpl @Inject constructor(
    private val dao: NotificationTemplateDao
) : NotificationTemplateRepository {

    override fun getAllTemplates(): Flow<List<NotificationTemplate>> {
        return dao.getAllTemplates().map { entities ->
            entities.map { NotificationTemplateMapper.toDomain(it) }
        }
    }

    override suspend fun getTemplateById(id: String): NotificationTemplate? {
        return dao.getTemplateById(id)?.let { NotificationTemplateMapper.toDomain(it) }
    }

    override suspend fun insertTemplate(template: NotificationTemplate) {
        dao.upsertTemplate(NotificationTemplateMapper.toEntity(template))
    }

    override suspend fun updateTemplate(template: NotificationTemplate) {
        dao.upsertTemplate(NotificationTemplateMapper.toEntity(template))
    }

    override suspend fun deleteTemplate(id: String) {
        dao.deleteCustomTemplate(id)
    }
}
