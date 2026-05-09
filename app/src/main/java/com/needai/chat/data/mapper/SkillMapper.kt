package com.needai.chat.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.needai.chat.data.local.db.entity.SkillEntity
import com.needai.chat.domain.model.Skill

object SkillMapper {

    private val gson = Gson()

    fun toDomain(entity: SkillEntity): Skill {
        val tags: List<String> = try {
            gson.fromJson(entity.tags, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
        return Skill(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            avatar = entity.avatar,
            systemPrompt = entity.systemPrompt,
            greeting = entity.greeting,
            temperature = entity.temperature,
            tags = tags,
            isBuiltin = entity.isBuiltin
        )
    }

    fun toEntity(skill: Skill): SkillEntity {
        return SkillEntity(
            id = skill.id,
            name = skill.name,
            description = skill.description,
            avatar = skill.avatar,
            systemPrompt = skill.systemPrompt,
            greeting = skill.greeting,
            temperature = skill.temperature,
            tags = gson.toJson(skill.tags),
            isBuiltin = skill.isBuiltin,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}
