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
            val sessions = sessionDao.getSessionsBySkillId(id)
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
}
