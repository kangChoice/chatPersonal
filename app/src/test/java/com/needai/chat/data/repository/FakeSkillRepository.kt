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
}
