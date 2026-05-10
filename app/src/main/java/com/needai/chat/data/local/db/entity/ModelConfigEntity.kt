package com.needai.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_configs")
data class ModelConfigEntity(
    @PrimaryKey val id: String,
    val name: String,
    val protocol: String,
    val remoteBaseUrl: String,
    val remoteApiKey: String,
    val remoteModelName: String,
    val temperature: Double,
    val maxTokens: Int,
    val topP: Double,
    val isBuiltin: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
