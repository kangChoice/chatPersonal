package com.needai.chat.data.repository

import com.needai.chat.data.local.db.dao.TokenTotals
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FakeChatRepository : ChatRepository {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    private var _currentSessionId = UUID.randomUUID().toString()
    private var nextId = 1L

    override fun getMessages(sessionId: String): Flow<List<Message>> {
        return _messages.map { list -> list.filter { it.sessionId == sessionId } }
    }

    override suspend fun insertMessage(message: Message): Long {
        val id = nextId++
        val msg = message.copy(id = id)
        _messages.value = _messages.value + msg
        if (!message.isRead) refreshUnreadCounts()
        return id
    }

    override suspend fun updateMessageContent(messageId: Long, content: String) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(content = content) else it
        }
    }

    override suspend fun updateMessageTokenUsage(messageId: Long, promptTokens: Int?, completionTokens: Int?, totalTokens: Int?) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(promptTokens = promptTokens, completionTokens = completionTokens, totalTokens = totalTokens) else it
        }
    }

    override suspend fun clearSession(sessionId: String) {
        _messages.value = _messages.value.filter { it.sessionId != sessionId }
    }

    override suspend fun getCurrentSessionId(): String = _currentSessionId

    override suspend fun createNewSession(): String {
        _currentSessionId = UUID.randomUUID().toString()
        return _currentSessionId
    }

    override suspend fun getTokenTotalsBySession(sessionId: String): TokenTotals {
        return TokenTotals(0, 0, 0)
    }

    override suspend fun getTokenTotalsByModelConfig(modelConfigId: String): TokenTotals {
        return TokenTotals(0, 0, 0)
    }

    override suspend fun getTokenTotalsByTimeRange(startTime: Long, endTime: Long): TokenTotals {
        return TokenTotals(0, 0, 0)
    }

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    override suspend fun markMessagesAsReadBySkill(skillId: String) {
        _messages.value = _messages.value.map {
            if (it.skillId == skillId && !it.isRead) it.copy(isRead = true) else it
        }
        refreshUnreadCounts()
    }

    override suspend fun markMessagesAsReadBySession(sessionId: String) {
        _messages.value = _messages.value.map {
            if (it.sessionId == sessionId && !it.isRead) it.copy(isRead = true) else it
        }
        refreshUnreadCounts()
    }

    override fun getUnreadCountsBySkill(): Flow<Map<String, Int>> = _unreadCounts

    private fun refreshUnreadCounts() {
        _unreadCounts.value = _messages.value
            .filter { !it.isRead && it.skillId != null }
            .groupBy { it.skillId!! }
            .mapValues { it.value.size }
    }
}
