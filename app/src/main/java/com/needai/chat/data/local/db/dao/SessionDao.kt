package com.needai.chat.data.local.db.dao

import androidx.room.*
import com.needai.chat.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE type = :type ORDER BY updatedAt DESC")
    fun getSessionsByType(type: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE skillId = :skillId")
    suspend fun getSessionsBySkillId(skillId: String): List<SessionEntity>

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("DELETE FROM sessions WHERE skillId = :skillId")
    suspend fun deleteSessionsBySkillId(skillId: String)

    @Query("SELECT COUNT(*) FROM sessions WHERE id = :id")
    suspend fun sessionExists(id: String): Int

    @Query("UPDATE sessions SET summaryText = :summaryText, summaryEndMessageId = :summaryEndMessageId WHERE id = :sessionId")
    suspend fun updateSummary(sessionId: String, summaryText: String?, summaryEndMessageId: Long?)
}
