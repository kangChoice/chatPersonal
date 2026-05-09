package com.needai.chat.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.SkillDao
import com.needai.chat.data.local.db.entity.MessageEntity
import com.needai.chat.data.local.db.entity.SkillEntity

@Database(
    entities = [SkillEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
    abstract fun messageDao(): MessageDao
}
