package com.needai.chat.app

import android.app.Application
import androidx.room.Room
import com.google.gson.Gson
import com.needai.chat.data.local.config.ModelConfigFileManager
import com.needai.chat.data.local.db.AppDatabase
import com.needai.chat.data.local.db.entity.ModelConfigEntity
import com.needai.chat.data.local.db.entity.SkillEntity
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class NeedAiApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeConfigFile()
        initializeDefaultSkills()
    }

    private fun initializeConfigFile() {
        ModelConfigFileManager(this, Gson()).ensureConfigExists()
    }

    private fun initializeDefaultSkills() {
        applicationScope.launch {
            val db = Room.databaseBuilder(
                this@NeedAiApplication,
                AppDatabase::class.java,
                "needai_chat.db"
            ).build()
            val count = db.skillDao().getCount()
            if (count == 0) {
                val now = System.currentTimeMillis()
                db.skillDao().upsertSkill(
                    SkillEntity(
                        id = "friend",
                        name = "作者本人",
                        description = "风趣幽默，傲娇毒舌",
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
            if (db.modelConfigDao().getCount() == 0) {
                val now = System.currentTimeMillis()
                db.modelConfigDao().upsertConfig(
                    ModelConfigEntity(
                        id = "trial",
                        name = "霸气侧漏",
                        protocol = "openai",
                        remoteBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode",
                        remoteApiKey = "sk-a04d6071373448c0ac3c2df46b827a00",
                        remoteModelName = "qwen-max",
                        temperature = 0.7,
                        maxTokens = 4096,
                        topP = 1.0,
                        isBuiltin = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            db.close()
        }
    }
}
