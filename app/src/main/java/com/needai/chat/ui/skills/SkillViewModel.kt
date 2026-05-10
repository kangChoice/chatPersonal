package com.needai.chat.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val _selectedSkillId = MutableStateFlow("default")
    val selectedSkillId: StateFlow<String> = _selectedSkillId.asStateFlow()

    init {
        viewModelScope.launch {
            skillRepository.getAllSkills().collect { skillList ->
                _skills.value = skillList
            }
        }
        viewModelScope.launch {
            _selectedSkillId.value = skillRepository.getSelectedSkillId()
        }
    }

    fun createSkill(name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double): Skill {
        val skill = Skill(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            avatar = avatar,
            systemPrompt = systemPrompt,
            greeting = greeting,
            temperature = temperature,
            tags = listOf("custom"),
            isBuiltin = false
        )
        viewModelScope.launch {
            skillRepository.insertSkill(skill)
        }
        return skill
    }

    fun updateSkill(skill: Skill) {
        viewModelScope.launch {
            skillRepository.updateSkill(skill)
        }
    }

    fun deleteSkill(id: String) {
        viewModelScope.launch {
            skillRepository.deleteSkill(id)
        }
    }

    fun selectSkill(skill: Skill) {
        viewModelScope.launch {
            skillRepository.setSelectedSkillId(skill.id)
            _selectedSkillId.value = skill.id
        }
    }

    suspend fun getSkillById(id: String): Skill? {
        return skillRepository.getSkillById(id)
    }

    fun importSkill(skill: Skill, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            skillRepository.insertSkill(skill)
            onResult(true, "技能已导入")
        }
    }
}
