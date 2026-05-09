package com.needai.chat.data.local.db.dao

import androidx.room.*
import com.needai.chat.data.local.db.entity.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY isBuiltin DESC, createdAt ASC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillById(id: String): SkillEntity?

    @Upsert
    suspend fun upsertSkill(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id AND isBuiltin = 0")
    suspend fun deleteCustomSkill(id: Int)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("SELECT COUNT(*) FROM skills")
    suspend fun getCount(): Int
}
