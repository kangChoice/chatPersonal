package com.needai.chat.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val systemPrompt: String,
    val greeting: String,
    val temperature: Double,
    val tags: String,
    val isBuiltin: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
