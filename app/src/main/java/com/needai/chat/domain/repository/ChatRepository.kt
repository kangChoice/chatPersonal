package com.needai.chat.domain.repository

import com.needai.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

import com.needai.chat.data.local.db.dao.TokenTotals

interface ChatRepository {
    fun getMessages(sessionId: String): Flow<List<Message>>
    suspend fun insertMessage(message: Message): Long
    suspend fun updateMessageContent(messageId: Long, content: String)
    suspend fun updateMessageTokenUsage(messageId: Long, promptTokens: Int?, completionTokens: Int?, totalTokens: Int?)
    suspend fun clearSession(sessionId: String)
    suspend fun getCurrentSessionId(): String
    suspend fun createNewSession(): String
    suspend fun getTokenTotalsBySession(sessionId: String): TokenTotals
    suspend fun getTokenTotalsByModelConfig(modelConfigId: String): TokenTotals
    suspend fun getTokenTotalsByTimeRange(startTime: Long, endTime: Long): TokenTotals
    suspend fun markMessagesAsReadBySkill(skillId: String)
    suspend fun markMessagesAsReadBySession(sessionId: String)
    fun getUnreadCountsBySkill(): Flow<Map<String, Int>>
}
