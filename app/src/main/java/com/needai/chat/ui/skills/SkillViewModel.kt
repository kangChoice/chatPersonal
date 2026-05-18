package com.needai.chat.ui.skills

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.repository.VoiceRepository
import com.needai.chat.util.AvatarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val voiceRepository: VoiceRepository,
    @ApplicationContext private val appContext: Context? = null
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

    fun createSkill(name: String, description: String, systemPrompt: String, avatar: String, greeting: String, temperature: Double, voiceId: String = "", avatarPath: String = "", enableMemory: Boolean = false, onResult: ((Boolean, String) -> Unit)? = null) {
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
            voiceId = voiceId,
            avatarPath = avatarPath,
            enableMemory = enableMemory
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
            // 获取删除前的 skill 信息，以便清理头像文件
            val skill = skillRepository.getSkillById(id)
            if (skill != null) {
                appContext?.let { AvatarUtils.deleteAvatar(it, skill.id, skill.isBuiltin) }
            }
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
