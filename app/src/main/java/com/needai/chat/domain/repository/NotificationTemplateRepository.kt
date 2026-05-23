package com.needai.chat.domain.repository

import com.needai.chat.domain.model.NotificationTemplate
import kotlinx.coroutines.flow.Flow

interface NotificationTemplateRepository {
    fun getAllTemplates(): Flow<List<NotificationTemplate>>
    suspend fun getTemplateById(id: String): NotificationTemplate?
    suspend fun insertTemplate(template: NotificationTemplate)
    suspend fun updateTemplate(template: NotificationTemplate)
    suspend fun deleteTemplate(id: String)
}
