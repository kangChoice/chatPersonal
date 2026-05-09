package com.needai.chat.domain.usecase

import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import javax.inject.Inject

class SwitchSkillUseCase @Inject constructor(
    private val repository: SkillRepository
) {
    suspend operator fun invoke(skill: Skill) {
        repository.setSelectedSkillId(skill.id)
    }
}
