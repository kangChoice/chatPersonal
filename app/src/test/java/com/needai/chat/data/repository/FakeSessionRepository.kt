package com.needai.chat.data.repository

import com.needai.chat.domain.model.ChatSession
import com.needai.chat.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionRepository : SessionRepository {

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())

    fun setSessions(sessions: List<ChatSession>) {
        _sessions.value = sessions
    }

    override fun getAllSessions(): Flow<List<ChatSession>> = _sessions

    override fun getSessionsByType(type: String): Flow<List<ChatSession>> {
        return MutableStateFlow(_sessions.value.filter { it.type == type })
    }

    override suspend fun getSessionById(id: String): ChatSession? {
        return _sessions.value.find { it.id == id }
    }

    override suspend fun getSessionsBySkillId(skillId: String, type: String): List<ChatSession> {
        return _sessions.value.filter { it.skillId == skillId && it.type == type }
    }

    override suspend fun saveSession(session: ChatSession) {
        val existing = _sessions.value.indexOfFirst { it.id == session.id }
        if (existing >= 0) {
            _sessions.value = _sessions.value.toMutableList().also { it[existing] = session }
        } else {
            _sessions.value = _sessions.value + session
        }
    }

    override suspend fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filter { it.id != id }
    }

    override suspend fun deleteSessionsBySkillId(skillId: String) {
        _sessions.value = _sessions.value.filter { it.skillId != skillId }
    }

    override suspend fun updateSummary(sessionId: String, summaryText: String?, summaryEndMessageId: Long?) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) session.copy(
                summaryText = summaryText,
                summaryEndMessageId = summaryEndMessageId
            ) else session
        }
    }
}
