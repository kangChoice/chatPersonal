package com.needai.chat.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()

    private val _customVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val customVoices: StateFlow<List<VoiceInfo>> = _customVoices.asStateFlow()

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
        viewModelScope.launch {
            voiceRepository.getVoices().let { voices ->
                _customVoices.value = voices
            }
        }
    }

    fun createSkill(name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double, voiceId: String = "", onResult: ((Boolean, String) -> Unit)? = null) {
        val skill = Skill(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            description = description,
            avatar = avatar,
            systemPrompt = systemPrompt,
            greeting = greeting,
            temperature = temperature,
            tags = listOf("custom"),
            isBuiltin = false,
            voiceId = voiceId
        )
        viewModelScope.launch {
            skillRepository.insertSkill(skill)
            onResult?.invoke(true, "角色「${skill.name}」已创建")
        }
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

    fun getSkillsByVoiceId(voiceId: String, callback: (List<Skill>) -> Unit) {
        viewModelScope.launch {
            val result = skillRepository.getSkillsByVoiceId(voiceId)
            callback(result)
        }
    }

    fun updateSkillsVoiceId(voiceId: String, selectedSkillIds: Set<String>) {
        viewModelScope.launch {
            skillRepository.updateSkillsVoiceId(voiceId, selectedSkillIds)
        }
    }

    fun clearVoiceIdForSkillIds(skillIds: Set<String>) {
        viewModelScope.launch {
            skillRepository.clearVoiceIdForSkillIds(skillIds)
        }
    }

    fun importSkill(skill: Skill, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            skillRepository.insertSkill(skill)
            onResult(true, "角色已导入")
        }
    }

    fun importSkills(skills: List<Skill>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            var successCount = 0
            for (skill in skills) {
                skillRepository.insertSkill(skill)
                successCount++
            }
            onResult(true, "成功导入 $successCount 个角色")
        }
    }
}
