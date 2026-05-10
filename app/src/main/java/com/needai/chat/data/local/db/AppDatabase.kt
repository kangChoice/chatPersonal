package com.needai.chat.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.ModelConfigDao
import com.needai.chat.data.local.db.dao.SessionDao
import com.needai.chat.data.local.db.dao.SkillDao
import com.needai.chat.data.local.db.entity.MessageEntity
import com.needai.chat.data.local.db.entity.ModelConfigEntity
import com.needai.chat.data.local.db.entity.SessionEntity
import com.needai.chat.data.local.db.entity.SkillEntity

@Database(
    entities = [SkillEntity::class, MessageEntity::class, SessionEntity::class, ModelConfigEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
    abstract fun messageDao(): MessageDao
    abstract fun sessionDao(): SessionDao
    abstract fun modelConfigDao(): ModelConfigDao
}
