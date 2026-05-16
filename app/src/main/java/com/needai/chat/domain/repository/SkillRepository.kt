package com.needai.chat.domain.repository

import com.needai.chat.domain.model.Skill
import kotlinx.coroutines.flow.Flow

interface SkillRepository {
    fun getAllSkills(): Flow<List<Skill>>
    fun selectedSkillIdFlow(): Flow<String>
    suspend fun getSkillById(id: String): Skill?
    suspend fun insertSkill(skill: Skill)
    suspend fun updateSkill(skill: Skill)
    suspend fun deleteSkill(id: String)
    suspend fun getSelectedSkillId(): String
    suspend fun setSelectedSkillId(id: String)
    suspend fun getSkillsByVoiceId(voiceId: String): List<Skill>
    suspend fun updateSkillsVoiceId(voiceId: String, selectedSkillIds: Set<String>)
    suspend fun clearVoiceIdForSkillIds(skillIds: Set<String>)
}
