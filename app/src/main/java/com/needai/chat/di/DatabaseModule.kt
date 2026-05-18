package com.needai.chat.di

import android.content.Context
import androidx.room.Room
import com.needai.chat.data.local.db.AppDatabase
import com.needai.chat.data.local.db.Migrations
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.ModelConfigDao
import com.needai.chat.data.local.db.dao.SessionDao
import com.needai.chat.data.local.db.dao.SkillDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "needai_chat.db"
        )
            // 先前的版本未写 Migration，fallbackToDestructiveMigration 兜底旧版本
            .addMigrations() // 在此注册 Migration：Migrations.MIGRATION_8_9, ...
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideSkillDao(database: AppDatabase): SkillDao = database.skillDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideModelConfigDao(database: AppDatabase): ModelConfigDao = database.modelConfigDao()
}
