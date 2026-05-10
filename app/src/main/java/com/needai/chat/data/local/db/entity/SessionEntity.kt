package com.needai.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val skillId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
