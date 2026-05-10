package com.needai.chat.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.needai.chat.data.local.db.entity.ModelConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelConfigDao {
    @Query("SELECT * FROM model_configs ORDER BY updatedAt DESC")
    fun getAllConfigs(): Flow<List<ModelConfigEntity>>

    @Query("SELECT * FROM model_configs WHERE id = :id")
    suspend fun getConfigById(id: String): ModelConfigEntity?

    @Upsert
    suspend fun upsertConfig(config: ModelConfigEntity)

    @Query("DELETE FROM model_configs WHERE id = :id")
    suspend fun deleteConfig(id: String)

    @Query("SELECT COUNT(*) FROM model_configs")
    suspend fun getCount(): Int
}
