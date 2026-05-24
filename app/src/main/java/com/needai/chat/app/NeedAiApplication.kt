package com.needai.chat.app

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.needai.chat.BuildConfig
import com.needai.chat.data.ilink.IlinkAuthManager
import com.needai.chat.data.ilink.IlinkBridgeService
import com.needai.chat.data.local.config.ModelConfigFileManager
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.local.db.AppDatabase
import com.needai.chat.data.local.db.entity.ModelConfigEntity
import com.needai.chat.data.local.db.entity.NotificationTemplateEntity
import com.needai.chat.data.local.db.entity.SkillEntity
import com.needai.chat.util.AvatarUtils
import com.needai.chat.util.FileLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class NeedAiApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var authManager: IlinkAuthManager
    @Inject lateinit var aiNotificationScheduler: AiNotificationScheduler
    @Inject lateinit var clawBotScheduleScheduler: ClawBotScheduleScheduler

    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
        setupCrashHandler()
        initializeConfigFile()
        initializeDefaults()
        scheduleAIAgentNotification()
        scheduleClawBotNotifications()
        startIlinkBridgeIfAuthenticated()
    }

    private fun scheduleAIAgentNotification() {
        aiNotificationScheduler.start()
    }

    private fun scheduleClawBotNotifications() {
        clawBotScheduleScheduler.start()
    }

    private fun startIlinkBridgeIfAuthenticated() {
        applicationScope.launch {
            if (!authManager.isAuthenticated()) {
                FileLogger.i("NeedAiApplication", "iLink: 未授权，跳过自动连接")
                return@launch
            }
            FileLogger.i("NeedAiApplication", "iLink: 自动启动桥接")
            val intent = Intent(this@NeedAiApplication, IlinkBridgeService::class.java).apply {
                action = IlinkBridgeService.ACTION_START
            }
            try {
                ContextCompat.startForegroundService(this@NeedAiApplication, intent)
            } catch (e: RuntimeException) {
                FileLogger.w("NeedAiApplication", "iLink: 无法从后台启动前台Service，等待App进入前台后由MainActivity启动: ${e.localizedMessage}")
            }
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            FileLogger.e("CrashHandler", "未捕获异常: thread=${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeConfigFile() {
        ModelConfigFileManager(this, Gson()).ensureConfigExists()
    }

    private fun initializeDefaults() {
        applicationScope.launch {
            val cfgManager = ModelConfigFileManager(this@NeedAiApplication, Gson())
            var (chatModel, ttsConfig) = cfgManager.readBuiltinModels()

            // Assets 中的内置 Key 已清空，优先使用 BuildConfig 注入值（来自 local.properties）
            if (chatModel != null && chatModel.remoteApiKey.isBlank()) {
                chatModel = chatModel.copy(remoteApiKey = BuildConfig.BUILTIN_CHAT_API_KEY)
            }
            if (ttsConfig != null && ttsConfig.apiKey.isBlank()) {
                ttsConfig = ttsConfig.copy(apiKey = BuildConfig.BUILTIN_TTS_API_KEY)
            }

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

            // 种子内置通知模板
            if (db.notificationTemplateDao().getCount() == 0) {
                val now = System.currentTimeMillis()
                listOf(
                    "builtin_miss" to ("想念" to "以你角色的身份，用第一人称直接对我说一句话。表达你想我了，语气温暖。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_sad_miss" to ("伤心的想念" to "以你角色的身份，用第一人称直接对我说一句话。表达你想我了，但我老是不理你，你很委屈。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_pout" to ("撒娇" to "以你角色的身份，用第一人称直接对我说一句撒娇的话，想引起我注意、让我来陪你。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_morning" to ("早安" to "以你角色的身份，用第一人称直接对我说一句早安问候，可以带点关心或撒娇。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_night" to ("晚安" to "以你角色的身份，用第一人称直接对我说一句晚安，语气温柔。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_care" to ("日常关心" to "以你角色的身份，用第一人称直接对我说一句日常关心的话，比如问我在干嘛、有没有好好吃饭。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                    "builtin_tsundere" to ("傲娇" to "以你角色的身份，用第一人称直接对我说一句傲娇的话——明明想了却嘴硬不承认。带上你的角色称呼。不要描述场景和动作，只输出这一句话。"),
                ).forEach { (id, pair) ->
                    db.notificationTemplateDao().upsertTemplate(
                        NotificationTemplateEntity(
                            id = id,
                            label = pair.first,
                            prompt = pair.second,
                            isBuiltin = true,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
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

            // 初始化默认角色头像
            AvatarUtils.initDefaultAvatar(this@NeedAiApplication)

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
