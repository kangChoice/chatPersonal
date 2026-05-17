package com.needai.chat.data.repository

import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSkillRepository : SkillRepository {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    private val _selectedSkillId = MutableStateFlow("default")

    fun setSkills(skills: List<Skill>) {
        _skills.value = skills
    }

    override fun getAllSkills(): Flow<List<Skill>> = _skills

    override fun selectedSkillIdFlow(): Flow<String> = _selectedSkillId

    override suspend fun getSkillById(id: String): Skill? {
        return _skills.value.find { it.id == id }
    }

    override suspend fun insertSkill(skill: Skill) {
        _skills.value = _skills.value + skill
    }

    override suspend fun updateSkill(skill: Skill) {
        _skills.value = _skills.value.map { if (it.id == skill.id) skill else it }
    }

    override suspend fun deleteSkill(id: String) {
        _skills.value = _skills.value.filter { it.id != id }
    }

    override suspend fun getSelectedSkillId(): String = _selectedSkillId.value

    override suspend fun setSelectedSkillId(id: String) {
        _selectedSkillId.value = id
    }

    override suspend fun getSkillsByVoiceId(voiceId: String): List<Skill> {
        return _skills.value.filter { it.voiceId == voiceId }
    }

    override suspend fun updateSkillsVoiceId(voiceId: String, selectedSkillIds: Set<String>) {
        _skills.value = _skills.value.map { skill ->
            if (skill.id in selectedSkillIds) skill.copy(voiceId = voiceId)
            else if (skill.voiceId == voiceId && skill.id !in selectedSkillIds) skill.copy(voiceId = "")
            else skill
        }
    }

    override suspend fun clearVoiceIdForSkillIds(skillIds: Set<String>) {
        _skills.value = _skills.value.map { skill ->
            if (skill.id in skillIds) skill.copy(voiceId = "") else skill
        }
    }
}
