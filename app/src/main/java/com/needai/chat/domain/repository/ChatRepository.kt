package com.needai.chat.domain.repository

import com.needai.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(sessionId: String): Flow<List<Message>>
    suspend fun insertMessage(message: Message): Long
    suspend fun updateMessageContent(messageId: Long, content: String)
    suspend fun clearSession(sessionId: String)
    suspend fun getCurrentSessionId(): String
    suspend fun createNewSession(): String
}
