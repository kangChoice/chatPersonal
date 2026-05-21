# CLAUDE.md

未来 Claude 实例请按此文档行事，别像个刚毕业的产品经理一样瞎搞。

## 构建 & 测试

```bash
./gradlew assembleDebug          # 搓一个 debug 包
./gradlew test                   # 跑所有单元测试
./gradlew test --tests "com.needai.chat.ui.chat.ChatViewModelTest"  # 跑单个测试
./gradlew test --tests "com.needai.chat.ui.skills.SkillViewModelTest" # 跑技能 VM 测试
./gradlew lintDebug              # 看看 lint 有没有意见
./gradlew clean assembleDebug    # 从头再来
```

需要 JDK 17+ 和 Android SDK 36。`local.properties` 里配 SDK 路径，别来问我。

**重要：** 项目依赖阿里云 NUI SDK（`libs/nuisdk-release.aar`），是一个本地 aar 文件。如果你要 CI 构建，得自己处理这个文件的托管。

## 项目结构

就一个 `:app` 模块，别指望啥 `:core` `:feature` 多模块优雅架构，不存在的。按包名分层：

```
app/src/main/java/com/needai/chat/
├── app/               # Application（Hilt + 启动初始化，含内置技能/模型种子数据）
├── domain/
│   ├── model/         # 领域模型（Message / ChatSession / Skill / ModelConfig / VoiceInfo / BackgroundConfig）
│   ├── repository/    # 仓库接口
│   └── usecase/       # 用例（SendMessageUseCase、GetChatHistoryUseCase、SwitchSkillUseCase 等）
├── data/
│   ├── local/
│   │   ├── db/        # Room 数据库（4 张表，version 8，毁灭性迁移）
│   │   ├── datastore/ # DataStore 存设置 + 模型配置
│   │   └── config/    # model_config.json 管理（从 assets 复制到文件）
│   ├── remote/
│   │   ├── client/    # ModelClient 接口 + RemoteModelClient（OkHttp SSE 流式）+ LocalModelClient（占位符）
│   │   ├── dto/       # OpenAI & Anthropic 协议请求/响应 DTO
│   │   ├── api/       # DeepSeekApi（Retrofit 接口 — dead code，实际是 OkHttp 直连）
│   │   ├── tts/       # CosyVoice TTS（NUI SDK）：合成、音色管理、PCM 播放、系统音色列表
│   │   └── asr/       # WebSocket ASR（DashScope）、VAD（Silero DNN）、AEC（平台 AEC）、回声过滤
│   ├── ilink/         # iLink 桥接（认证、客户端、定时任务、微信消息处理、前台通知）
│   ├── mapper/        # Entity ↔ Domain 映射器
│   ├── repository/    # 仓库实现
│   ├── export/        # Markdown 会话导出、JSON 配置+技能导出
│   └── import/        # 导入工具
├── ui/
│   ├── chat/          # 单聊页面（核心）
│   │   └── components/ # ChatInputBar / MessageBubble / StreamingText / SkillSelectorSheet / HistorySessionSheet / TtsSpeakButton
│   ├── multichat/     # 群聊页面（多角色 AI 同时参与对话）
│   ├── skills/        # 技能管理 + 音色管理（合并到一个页面 SkillAndVoiceScreen）
│   ├── voice/         # 音色独立管理页（增删改查 + 试听）
│   ├── voicechat/     # 语音通话页（ASR → LLM → TTS 全双工对话）
│   ├── prompt/        # 提示词润色（AI 帮你写 system prompt）
│   ├── settings/      # 设置页、模型配置管理、TTS 配置
│   ├── ilink/         # iLink 桥接管理页（扫码连接、状态监控、调试）
│   ├── schedule/      # 定时任务配置页（固定/随机消息、时间范围、频率）
│   ├── stats/         # Token 统计（入口隐藏）
│   ├── onboarding/    # 新手指引覆盖层（4 步引导）
│   ├── navigation/    # NavGraph + 底部导航（6 个 tab）
│   └── theme/         # Material 3 主题
├── di/                # Hilt 注入（AppModule / DatabaseModule / NetworkModule / VoiceModule）
└── util/              # Constants / EncryptUtil / FileLogger / DevicePrefixManager / TtsManager / AppToast
```

### 6 个底部 Tab

聊天 → 群聊 → 技能管理（含音色） → 提示词优化 → 定时任务 → 设置

## 数据库（Room v8）

**核心原则：数据库版本升级必须写兼容性 Migration，禁止使用 `fallbackToDestructiveMigration()`。** 用户聊天记录和技能配置是有价值的数据，每次升级都删数据不可接受。

### 4 张表

- **skills** — 技能/角色预设。字段：id、name、description、avatar、systemPrompt、greeting、temperature、tags（JSON 字符串存数组）、isBuiltin、createdAt、updatedAt
- **messages** — 聊天消息。字段：自增 id、sessionId、role（USER/ASSISTANT/SYSTEM）、content、skillId（可空）、timestamp、isStreaming、promptTokens/completionTokens/totalTokens（都可空）、modelConfigId（可空）
- **sessions** — 会话。字段：id、skillId、title、createdAt、updatedAt
- **model_configs** — 模型配置。字段：id、name、protocol（OPENAI/ANTHROPIC）、remoteBaseUrl、remoteApiKey、remoteModelName、temperature、maxTokens、topP、isBuiltin、createdAt、updatedAt

### Migration 写法规范

1. 在 `DatabaseModule.kt` 中用 `addMigrations()`，禁止 `fallbackToDestructiveMigration()`
2. 每条迁移写一个 `object : Migration(oldVersion, newVersion)`，实现 `migrate(db)` 方法
3. 用 `ALTER TABLE` 加字段/加索引，`CREATE TABLE` 建新表，不要删改已有列
4. 新增表或新增可空字段不需要复杂数据迁移
5. 迁移写好后加到 `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)` 链上

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN modelConfigId TEXT DEFAULT NULL")
    }
}
```

## 支持的 AI 协议

- **OpenAI 兼容** — 标准的 `/v1/chat/completions` + SSE data 块
- **Anthropic** — Messages API + SSE event 流
- 内置 11 家供应商预设，在 `KnownProvider` 里

## TTS & ASR（语音功能）

### TTS（阿里云 CosyVoice）

- 依赖 `nuisdk-release.aar`（NUI SDK），通过 `NativeNui` 实现流式 TTS
- 封装在 `CosyVoiceClient`，支持点播朗读和流式朗读
- `TtsManagerImpl` 实现了长文本分段、流式轮换 session 策略（解决 CosyVoice 长文本跳段/变声问题）
- 支持系统预置音色（`SystemVoiceProvider` 有 200+ 音色，覆盖 v1/v2/v3-flash/v3-plus）和自定义设计音色（声音克隆）
- PCM 播放器 `PcmAudioPlayer` 使用 `AudioTrack` 播放
- 音色管理：通过 `VoiceDesignClient`（DashScope API）实现远程音色的创建、查询、删除

### ASR（语音识别 + VAD + AEC）

- `AsrEngine`：通过阿里云 DashScope WebSocket API 实现实时语音识别，完全绕过 NUI SDK 避免 singleton 冲突
- `AudioProcessor`：封装 Android 平台 AEC（AcousticEchoCanceler）+ Silero VAD（ONNX Runtime DNN 模型）
- `TtsReferenceBuffer`：TTS PCM 环形缓冲区，供 AEC 作为参考信号
- 之前在 `docs/echo-cancellation-strategy.md` 记录了从纯文本匹配到物理 AEC + VAD 的演进方案

### 语音通话页（VoiceChatScreen）

全双工语音对话：**ASR → LLM → TTS** 编排在 `VoiceChatViewModel` + `VoiceChatManager` 中，支持打断、波形显示、音量振幅可视化。

## 测试

- JUnit 4 + kotlinx-coroutines-test，没用 mock 框架，全是 Fake 仓库（FakeChatRepository / FakeSkillRepository / FakeSessionRepository 等）
- Turbine 在 dependencies 里声明了但还没用上
- 纯 JVM 测试，不用模拟器
- 跑测试前确认 FakeRepository 们跟实际接口对得上，经常改了接口忘了改 Fake

## 代码里的坑（记好了）

### EncryptUtil 的 IV 持久化 bug
`encrypt()` 用 AES/GCM 每次随机生成 IV，但 `ciphertext` 和 `iv` 是分开返回的，调用方只存了 `ciphertext` 没存 `iv`，`decrypt()` 需要传 iv 所以根本解不出来。API Key 加密存了等于没存。谁要有空修一下。

### ChatViewModel 不用 DI
明明 Hilt 注入了一个 `ModelClient`，`sendMessage()` 里却自己 `new RemoteModelClient(Gson())`。属于是脱裤子放屁。

### MultiChatViewModel 用 DI 了，但 ChatViewModel 没用
`MultiChatViewModel` 正确地通过 Hilt 构造函数注入了 `ModelClient`，但 `ChatViewModel` 就是不跟进。双标现场。

### SendMessageUseCase 写了没用
`domain/usecase/` 里定义得好好的，但没有任何 ViewModel 用 — 发消息逻辑直接写在 ViewModel 里了。

### DeepSeekApi（Retrofit 接口）是 dead code
`data/remote/api/DeepSeekApi.kt` — 定义了一个 Retrofit 接口，但实际请求用的是 OkHttp 直连。主打一个写了等于没写。

### 统计页面入口隐藏
StatsScreen 有路由有页面，但不在底部导航里。你要导航过去直接 `navController.navigate("stats")` 就行，别问为什么隐藏，问就是故意。

### AsrEngine 依赖 DashScope API Key 硬编码
`VoiceChatViewModel` 中 ASR 的 API Key 从 `SettingsDataStore` 获取（TTS API Key 复用），但内置阿里云 Key 已过期。首次启动后用户必须自行设置有效的 TTS API Key 才能使用语音通话功能。

### 数据库 Migration 基础设施已就绪，但历史 Migration 未补
`Migrations.kt` 已创建、`DatabaseModule.kt` 已配置 `addMigrations()` + `fallbackToDestructiveMigration()` 兜底。但目前没有定义任何 Migration 对象，需要升版本时在 `Migrations.kt` 中添加 MIGRATION_X_Y 再注册到 `DatabaseModule.kt`。

### 音频打包体积
`build.gradle.kts` 中配置了 `abiFilters = ["arm64-v8a", "armeabi-v7a"]` 排除 x86 架构的 so 文件。VAD 的 ONNX Runtime 强制 ≥1.25.0 以确保 16 KB 对齐。

## 其他你该知道的

- 当前版本：**v1.4.0**（versionCode=4），minSdk=26，targetSdk=36
- **全 UI 中文**，别手贱加英文
- **Release 包？不存在的。** 没有签名配置，没有混淆，没有 CI
- 内置 API Key 已从 `assets/model_config.json` 剥离，改用 BuildConfig 注入（见密钥安全章节）。首次启动前在 `local.properties` 中配置有效 Key 才能使用体验模型和 TTS。
- 首次启动会从 assets 复制 `model_config.json` 并初始化内置技能 + 体验模型配置 + TTS 配置
- 内置技能（isBuiltin=true）不可编辑不可删除
- 支持背景图片自定义（`BackgroundConfig`，通过 DataStore 存储）
- 设备隔离前缀（`DevicePrefixManager`）：基于 Android ID 生成 10 位 Base62 前缀，用于远程音色管理中的设备隔离
- `TtsManager` 中的 `voiceModelResolver` 回调根据 voiceId 返回对应模型名，系统音色 → `cosyvoice-v3-flash`，自定义音色 → 其 `targetModel`
- 所有的语音相关日志走 `FileLogger`，文件日志保留 3 天、单文件最大 5MB

## AppToast 通用弹窗组件

`ui/util/AppToast.kt` — 替代散落各处的 `Toast.makeText` 和 `Snackbar` 的通用提示组件。

- **`ToastState`**：持有 `SnapshotStateList<ToastItem>`，`show(message, type)` 添加提示，自动在 2500ms 后移除
- **`LocalToast`**：`staticCompositionLocalOf`，在 `MainActivity` 中通过 `CompositionLocalProvider` 注入全局实例
- **`AppToastHost`**：放在最外层 `Box` 的 overlay 层，顶部堆叠显示，支持同时多条提示
- **`ToastType`**：`Success`（绿点）、`Error`（红点）、`Info`（蓝点）
- 使用方式：任意 Composable 中 `val toast = LocalToast.current`，然后 `toast.show("消息", ToastType.Success)`
- 已替换的地方：TTS 自动朗读弹窗、技能头像更新、提示词优化页创建成功、iLink 桥接状态变更

## iLink 桥接 & 定时任务配置

### IlinkScheduleManager — 定时消息

`data/ilink/IlinkScheduleManager.kt` 管理 WeChat ClawBot 定时消息的发送编排。

- **`ScheduleConfig`**：持久化配置（DataStore JSON），包含 `fixedMessages: List<FixedScheduleItem>`（固定时间+内容）、`randomMessage`（随机消息内容）、`randomStartTime`/`randomEndTime`（随机时间范围，HH:mm 格式）、`randomCount`（每日随机次数）
- **`FixedScheduleItem`**：`time: String`（HH:mm）+ `message: String`
- 配置通过 `IlinkScheduleViewModel` → `IlinkScheduleScreen` 在"定时任务" tab 中可视化编辑
- `checkAndSend()` 每分钟触发一次，读取内存中的 `scheduleConfig`，无需重启 Service
- `context_token` 有微信服务端 10 次限制，`MAX_USAGE=8` 留 margin；`ret=-2` 是 iLink 频率限制，非 token 耗尽
- 随机时间生成为分段均匀分布（`generateRandomTimes(startHour, endHour, exclude)`），首次连接从当前小时开始避免过期时间点

## 密钥安全：BuildConfig 注入机制

Assets 中的 `model_config.json` 不再包含内置 API Key（已清空）。构建时通过 Gradle 从 `local.properties` 读取密钥，注入 `BuildConfig` 字段：

```properties
# local.properties（已 gitignore）
builtin.chat.api.key=sk-your-chat-api-key-here
builtin.tts.api.key=sk-your-tts-api-key-here
```

首次运行 `NeedAiApplication` 时，如果 assets 读取到的 Key 为空，会自动回退到 `BuildConfig.BUILTIN_CHAT_API_KEY` / `BuildConfig.BUILTIN_TTS_API_KEY`。

这个方案仍然不能完全防止密钥从 APK 中被提取（BuildConfig 字段编译在 DEX 中，反编译仍可见），但避免了**直接解压 APK 读取 `assets/model_config.json` 即可获取密钥**的暴露风险。
