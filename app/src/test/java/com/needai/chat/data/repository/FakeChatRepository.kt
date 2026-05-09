package com.needai.chat.data.repository

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
        return id
    }

    override suspend fun updateMessageContent(messageId: Long, content: String) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(content = content) else it
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
}
