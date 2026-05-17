package com.needai.chat.data.repository

import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.SessionDao
import com.needai.chat.data.local.db.dao.SkillDao
import com.needai.chat.data.local.db.entity.SessionEntity
import com.needai.chat.data.mapper.SessionMapper
import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val skillDao: SkillDao,
    private val messageDao: MessageDao
) : SessionRepository {

    private suspend fun toDomainSession(entity: SessionEntity): ChatSession {
        val count = messageDao.getMessageCount(entity.id)
        if (entity.type == "multi") {
            return SessionMapper.toDomain(
                entity = entity,
                skillName = "群聊",
                skillAvatar = "💬",
                messageCount = count
            )
        }
        val skill = skillDao.getSkillById(entity.skillId)
        return SessionMapper.toDomain(
            entity = entity,
            skillName = skill?.name ?: "已删除的技能",
            skillAvatar = skill?.avatar ?: "❓",
            messageCount = count
        )
    }

    override fun getAllSessions(): Flow<List<ChatSession>> {
        return sessionDao.getAllSessions().map { entities ->
            val result = mutableListOf<ChatSession>()
            for (entity in entities) {
                result.add(toDomainSession(entity))
            }
            result
        }
    }

    override fun getSessionsByType(type: String): Flow<List<ChatSession>> {
        return sessionDao.getSessionsByType(type).map { entities ->
            val result = mutableListOf<ChatSession>()
            for (entity in entities) {
                result.add(toDomainSession(entity))
            }
            result
        }
    }

    override suspend fun getSessionById(id: String): ChatSession? {
        val entity = sessionDao.getSessionById(id) ?: return null
        return toDomainSession(entity)
    }

    override suspend fun getSessionsBySkillId(skillId: String): List<ChatSession> {
        return sessionDao.getSessionsBySkillId(skillId).map { toDomainSession(it) }
    }

    override suspend fun saveSession(session: ChatSession) {
        sessionDao.upsertSession(SessionMapper.toEntity(session))
    }

    override suspend fun deleteSession(id: String) {
        sessionDao.deleteSession(id)
    }

    override suspend fun deleteSessionsBySkillId(skillId: String) {
        val sessions = sessionDao.getSessionsBySkillId(skillId)
        for (s in sessions) {
            messageDao.clearSession(s.id)
        }
        sessionDao.deleteSessionsBySkillId(skillId)
    }
}
