package com.needai.chat.domain.repository

import com.needai.chat.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    fun getSessionsByType(type: String): Flow<List<ChatSession>>
    suspend fun getSessionById(id: String): ChatSession?
    suspend fun saveSession(session: ChatSession)
    suspend fun deleteSession(id: String)
    suspend fun deleteSessionsBySkillId(skillId: String)
}
