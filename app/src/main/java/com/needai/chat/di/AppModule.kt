package com.needai.chat.di

import android.content.Context
import com.needai.chat.data.local.datastore.ModelConfigDataStore
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.client.ModelClient
import com.needai.chat.data.remote.client.RemoteModelClient
import com.needai.chat.data.repository.ChatRepositoryImpl
import com.needai.chat.data.repository.ModelConfigRepositoryImpl
import com.needai.chat.data.repository.SkillRepositoryImpl
import com.needai.chat.domain.repository.ChatRepository
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSkillRepository(impl: SkillRepositoryImpl): SkillRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindModelConfigRepository(impl: ModelConfigRepositoryImpl): ModelConfigRepository

    @Binds
    @Singleton
    abstract fun bindModelClient(client: RemoteModelClient): ModelClient
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideModelConfigDataStore(@ApplicationContext context: Context): ModelConfigDataStore {
        return ModelConfigDataStore(context)
    }
}
