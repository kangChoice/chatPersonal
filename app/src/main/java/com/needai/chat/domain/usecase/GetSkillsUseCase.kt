package com.needai.chat.domain.usecase

import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSkillsUseCase @Inject constructor(
    private val repository: SkillRepository
) {
    operator fun invoke(): Flow<List<Skill>> = repository.getAllSkills()
}
