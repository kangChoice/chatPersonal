package com.needai.chat.data.repository

import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.TokenTotals
import com.needai.chat.data.mapper.MessageMapper
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import com.needai.chat.data.local.db.dao.SkillUnreadCount
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val settingsDataStore: SettingsDataStore
) : ChatRepository {

    override fun getMessages(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessages(sessionId).map { entities ->
            entities.map { MessageMapper.toDomain(it) }
        }
    }

    override suspend fun insertMessage(message: Message): Long {
        return messageDao.insertMessage(MessageMapper.toEntity(message))
    }

    override suspend fun updateMessageContent(messageId: Long, content: String) {
        messageDao.updateContent(messageId, content)
    }

    override suspend fun updateMessageTokenUsage(messageId: Long, promptTokens: Int?, completionTokens: Int?, totalTokens: Int?) {
        messageDao.updateTokenUsage(messageId, promptTokens, completionTokens, totalTokens)
    }

    override suspend fun clearSession(sessionId: String) {
        messageDao.clearSession(sessionId)
    }

    override suspend fun getCurrentSessionId(): String {
        return settingsDataStore.currentSessionId.first()
    }

    override suspend fun createNewSession(): String {
        val newId = UUID.randomUUID().toString()
        settingsDataStore.setCurrentSessionId(newId)
        return newId
    }

    override suspend fun getTokenTotalsBySession(sessionId: String): TokenTotals {
        return TokenTotals(
            promptTokens = messageDao.getTotalPromptTokensByUser(sessionId),
            completionTokens = messageDao.getTotalCompletionTokensByAssistant(sessionId),
            totalTokens = messageDao.getTotalTokens(sessionId)
        )
    }

    override suspend fun getTokenTotalsByModelConfig(modelConfigId: String): TokenTotals {
        return messageDao.getTokenTotalsByModelConfig(modelConfigId)
            ?: TokenTotals()
    }

    override suspend fun getTokenTotalsByTimeRange(startTime: Long, endTime: Long): TokenTotals {
        return messageDao.getTokenTotalsByTimeRange(startTime, endTime)
            ?: TokenTotals()
    }

    override suspend fun markMessagesAsReadBySkill(skillId: String) {
        messageDao.markMessagesAsReadBySkill(skillId)
    }

    override suspend fun markMessagesAsReadBySession(sessionId: String) {
        messageDao.markMessagesAsReadBySession(sessionId)
    }

    override fun getUnreadCountsBySkill(): Flow<Map<String, Int>> {
        return messageDao.getUnreadCountsBySkill().map { list ->
            list.associate { it.skillId to it.count }
        }
    }
}
