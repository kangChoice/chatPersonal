package com.needai.chat.data.repository

import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.SessionDao
import com.needai.chat.data.local.db.dao.SkillDao
import com.needai.chat.data.mapper.SkillMapper
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.repository.SkillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepositoryImpl @Inject constructor(
    private val skillDao: SkillDao,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val settingsDataStore: SettingsDataStore
) : SkillRepository {

    override fun getAllSkills(): Flow<List<Skill>> {
        return skillDao.getAllSkills().map { entities ->
            entities.map { SkillMapper.toDomain(it) }
        }
    }

    override fun selectedSkillIdFlow(): Flow<String> {
        return settingsDataStore.selectedSkillId
    }

    override suspend fun getSkillById(id: String): Skill? {
        return skillDao.getSkillById(id)?.let { SkillMapper.toDomain(it) }
    }

    override suspend fun insertSkill(skill: Skill) {
        skillDao.upsertSkill(SkillMapper.toEntity(skill))
    }

    override suspend fun updateSkill(skill: Skill) {
        skillDao.upsertSkill(SkillMapper.toEntity(skill))
    }

    override suspend fun deleteSkill(id: String) {
        skillDao.getSkillById(id)?.let {
            // Cascade delete: remove all sessions and their messages for this skill
            val sessions = sessionDao.getSessionsBySkillId(id, "single")
            for (s in sessions) {
                messageDao.clearSession(s.id)
            }
            sessionDao.deleteSessionsBySkillId(id)
            skillDao.deleteSkill(it)
        }
    }

    override suspend fun getSelectedSkillId(): String {
        return settingsDataStore.selectedSkillId.first()
    }

    override suspend fun setSelectedSkillId(id: String) {
        settingsDataStore.setSelectedSkillId(id)
    }

    override suspend fun getSkillsByVoiceId(voiceId: String): List<Skill> {
        return skillDao.getSkillsByVoiceId(voiceId).map { SkillMapper.toDomain(it) }
    }

    override suspend fun updateSkillsVoiceId(voiceId: String, selectedSkillIds: Set<String>) {
        // Unbind skills that were previously bound but not selected
        val currentBindings = skillDao.getSkillsByVoiceId(voiceId)
        for (entity in currentBindings) {
            if (entity.id !in selectedSkillIds) {
                skillDao.upsertSkill(entity.copy(voiceId = ""))
            }
        }
        // Bind newly selected skills
        for (skillId in selectedSkillIds) {
            skillDao.getSkillById(skillId)?.let { entity ->
                if (entity.voiceId != voiceId) {
                    skillDao.upsertSkill(entity.copy(voiceId = voiceId))
                }
            }
        }
    }

    override suspend fun clearVoiceIdForSkillIds(skillIds: Set<String>) {
        for (skillId in skillIds) {
            skillDao.getSkillById(skillId)?.let { entity ->
                if (entity.voiceId.isNotEmpty()) {
                    skillDao.upsertSkill(entity.copy(voiceId = ""))
                }
            }
        }
    }
}
