package com.needai.chat.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.needai.chat.data.local.db.entity.NotificationTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationTemplateDao {
    @Query("SELECT * FROM notification_templates ORDER BY isBuiltin DESC, createdAt ASC")
    fun getAllTemplates(): Flow<List<NotificationTemplateEntity>>

    @Query("SELECT * FROM notification_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): NotificationTemplateEntity?

    @Upsert
    suspend fun upsertTemplate(template: NotificationTemplateEntity)

    @Query("DELETE FROM notification_templates WHERE id = :id AND isBuiltin = 0")
    suspend fun deleteCustomTemplate(id: String)

    @Query("SELECT COUNT(*) FROM notification_templates")
    suspend fun getCount(): Int
}
