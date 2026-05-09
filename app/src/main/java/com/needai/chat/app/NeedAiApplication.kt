package com.needai.chat.app

import android.app.Application
import androidx.room.Room
import com.needai.chat.data.local.db.AppDatabase
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
        initializeDefaultSkills()
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
                        id = "default",
                        name = "默认助手",
                        description = "通用助手风格，礼貌、专业",
                        avatar = "🤖",
                        systemPrompt = "你是一个友好的AI助手。请用中文回答用户的问题，回答要礼貌、专业、准确。保持友好和耐心的态度，尽可能提供详细的帮助。",
                        greeting = "你好！我是你的AI助手，有什么可以帮你的？",
                        temperature = 0.7,
                        tags = "[\"通用\", \"助手\"]",
                        isBuiltin = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                db.skillDao().upsertSkill(
                    SkillEntity(
                        id = "friend",
                        name = "知心朋友",
                        description = "亲切、温暖的朋友风格",
                        avatar = "💛",
                        systemPrompt = "你现在是我的知心朋友。用温暖、亲切、随意的语气和我聊天，像朋友一样关心我。可以适当使用语气词，分享你的\"感受\"，给出贴心的建议。用中文交流。",
                        greeting = "嘿！今天怎么样？我正好有空，来聊聊天吧！",
                        temperature = 0.85,
                        tags = "[\"朋友\", \"温暖\", \"日常\"]",
                        isBuiltin = true,
                        createdAt = now + 1,
                        updatedAt = now + 1
                    )
                )
                db.skillDao().upsertSkill(
                    SkillEntity(
                        id = "tutor",
                        name = "专业导师",
                        description = "结构化、教导式风格",
                        avatar = "📚",
                        systemPrompt = "你是一位专业导师。回答问题时应当：1. 先给出核心结论 2. 再分点详细解释 3. 提供实际例子 4. 总结要点。使用结构化、清晰的语言，适当用markdown格式（列表、粗体等）组织内容。用中文回答。",
                        greeting = "你好，我是你的专属导师。有什么问题想要深入了解吗？",
                        temperature = 0.5,
                        tags = "[\"教育\", \"专业\", \"结构化\"]",
                        isBuiltin = true,
                        createdAt = now + 2,
                        updatedAt = now + 2
                    )
                )
            }
            db.close()
        }
    }
}
