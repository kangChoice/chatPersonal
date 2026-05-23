package com.needai.chat.di

import android.content.Context
import androidx.room.Room
import com.needai.chat.data.local.db.AppDatabase
import com.needai.chat.data.local.db.Migrations
import com.needai.chat.data.local.db.dao.MessageDao
import com.needai.chat.data.local.db.dao.ModelConfigDao
import com.needai.chat.data.local.db.dao.NotificationTemplateDao
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
            // 注册所有历史迁移，确保任意旧版本都能平滑升级
            .addMigrations(Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6, Migrations.MIGRATION_6_7, Migrations.MIGRATION_7_8, Migrations.MIGRATION_8_9, Migrations.MIGRATION_9_10)
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

    @Provides
    fun provideNotificationTemplateDao(database: AppDatabase): NotificationTemplateDao = database.notificationTemplateDao()
}
