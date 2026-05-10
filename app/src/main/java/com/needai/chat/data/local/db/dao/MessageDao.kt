package com.needai.chat.data.local.db.dao

import androidx.room.*
import com.needai.chat.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET content = :content WHERE id = :messageId")
    suspend fun updateContent(messageId: Long, content: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    @Query("UPDATE messages SET promptTokens = :prompt, completionTokens = :completion, totalTokens = :total WHERE id = :messageId")
    suspend fun updateTokenUsage(messageId: Long, prompt: Int?, completion: Int?, total: Int?)

    @Query("SELECT SUM(promptTokens) FROM messages WHERE sessionId = :sessionId")
    suspend fun getTotalPromptTokens(sessionId: String): Int?

    @Query("SELECT SUM(completionTokens) FROM messages WHERE sessionId = :sessionId")
    suspend fun getTotalCompletionTokens(sessionId: String): Int?

    @Query("SELECT SUM(totalTokens) FROM messages WHERE sessionId = :sessionId")
    suspend fun getTotalTokens(sessionId: String): Int?

    @Query("SELECT SUM(promptTokens) FROM messages WHERE sessionId = :sessionId AND role = 'USER'")
    suspend fun getTotalPromptTokensByUser(sessionId: String): Int?

    @Query("SELECT SUM(completionTokens) FROM messages WHERE sessionId = :sessionId AND role = 'ASSISTANT'")
    suspend fun getTotalCompletionTokensByAssistant(sessionId: String): Int?

    @Query("SELECT SUM(promptTokens) AS promptTokens, SUM(completionTokens) AS completionTokens, SUM(totalTokens) AS totalTokens FROM messages WHERE modelConfigId = :modelConfigId")
    suspend fun getTokenTotalsByModelConfig(modelConfigId: String): TokenTotals?

    @Query("SELECT SUM(promptTokens) AS promptTokens, SUM(completionTokens) AS completionTokens, SUM(totalTokens) AS totalTokens FROM messages WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTokenTotalsByTimeRange(startTime: Long, endTime: Long): TokenTotals?
}
