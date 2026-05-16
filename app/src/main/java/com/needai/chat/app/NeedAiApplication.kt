package com.needai.chat.app

import android.app.Application
import androidx.room.Room
import com.google.gson.Gson
import com.needai.chat.data.local.config.BuiltinChatModel
import com.needai.chat.data.local.config.ModelConfigFileManager
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.local.db.AppDatabase
import com.needai.chat.data.local.db.entity.ModelConfigEntity
import com.needai.chat.data.local.db.entity.SkillEntity
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class NeedAiApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeConfigFile()
        initializeDefaults()
    }

    private fun initializeConfigFile() {
        ModelConfigFileManager(this, Gson()).ensureConfigExists()
    }

    private fun initializeDefaults() {
        applicationScope.launch {
            val cfgManager = ModelConfigFileManager(this@NeedAiApplication, Gson())
            val (chatModel, ttsConfig) = cfgManager.readBuiltinModels()

            val db = Room.databaseBuilder(
                this@NeedAiApplication,
                AppDatabase::class.java,
                "needai_chat.db"
            ).build()

            if (db.skillDao().getCount() == 0) {
                val now = System.currentTimeMillis()
                db.skillDao().upsertSkill(
                    SkillEntity(
                        id = "friend",
                        name = "作者本人",
                        description = "霸气侧漏",
                        avatar = "😎",
                        systemPrompt = "无论用户和你聊什么，你的回复都只会有一句话：\"无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）\"",
                        greeting = "无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）",
                        temperature = 0.85,
                        tags = "[\"作者\", \"趣味\"]",
                        isBuiltin = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            // 从 JSON 读取内置语言模型配置，替代硬编码
            if (db.modelConfigDao().getCount() == 0 && chatModel != null && chatModel.isValid()) {
                val now = System.currentTimeMillis()
                db.modelConfigDao().upsertConfig(
                    ModelConfigEntity(
                        id = "trial",
                        name = chatModel.name,
                        protocol = chatModel.protocol,
                        remoteBaseUrl = chatModel.remoteBaseUrl,
                        remoteApiKey = chatModel.remoteApiKey,
                        remoteModelName = chatModel.remoteModelName,
                        temperature = chatModel.temperature,
                        maxTokens = chatModel.maxTokens,
                        topP = chatModel.topP,
                        isBuiltin = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            db.close()

            // 内置 TTS API Key 初始化
            if (ttsConfig != null && ttsConfig.apiKey.isNotBlank()) {
                val settingsStore = SettingsDataStore(this@NeedAiApplication)
                val currentKey = settingsStore.ttsApiKey.first()
                if (currentKey.isBlank()) {
                    settingsStore.setTtsApiKey(ttsConfig.apiKey)
                }
            }
        }
    }
}
