package com.needai.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_templates")
data class NotificationTemplateEntity(
    @PrimaryKey val id: String,
    val label: String,
    val prompt: String,
    val isBuiltin: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
