# 聊天对象安卓应用 — 详细技术规格说明书

## 1. 项目概述

### 1.1 产品定位
一款 Android 聊天应用，用户可以通过选择不同的 **Skill（技能/人格模板）** 与 AI 模型进行角色化聊天。每个 Skill 定义了一种特定的聊天风格和人格设定，模型（本地或远程）根据 Skill 的设定生成对应风格的回复。

### 1.2 技术栈
| 层级 | 技术选型 |
|------|----------|
| 语言 | Kotlin 1.9+ |
| UI | Jetpack Compose (Material 3) |
| 架构 | MVVM + Clean Architecture |
| 异步 | Kotlin Coroutines + Flow |
| DI | Hilt |
| 网络 | OkHttp + Retrofit |
| 本地存储 | Room + DataStore |
| 流式解析 | Kotlin Flow + okhttp 流式响应 |
| 本地模型客户端 | Ollama REST API 客户端 |


---

## 2. 功能需求详情

### 2.1 Skill 系统（核心功能）

#### 2.1.1 Skill 定义
每个 Skill 是一个 JSON 结构，包含以下字段：

```json
{
  "id": "string (唯一标识)",
  "name": "string (展示名称)",
  "description": "string (简短描述)",
  "avatar": "string (头像资源标识或emoji)",
  "systemPrompt": "string (系统提示词，定义人格和聊天风格)",
  "greeting": "string (初始问候语)",
  "temperature": "double (可选，默认 0.7)",
  "tags": ["string"] (分类标签),
  "isBuiltin": "boolean (是否为内置 Skill)"
}
```

#### 2.1.2 内置 Skill（至少包含 3 个）
1. **默认助手** — 通用助手风格，礼貌、专业
2. **知心朋友** — 亲切、温暖的朋友风格
3. **专业导师** — 结构化、教导式风格

#### 2.1.3 Skill 管理
- 内置 Skill 不可删除，不可修改
- 用户可创建自定义 Skill（设置名称、描述、系统提示词等）
- 支持编辑和删除自定义 Skill
- 支持在聊天界面中**自由切换**当前使用的 Skill
- Skill 切换后，聊天上下文的 system prompt 跟随切换，已存在的对话历史保留

#### 2.1.4 Skill 存储
- 内置 Skill：打包在 APK 中，首次启动写入 Room 数据库
- 自定义 Skill：存储在 Room 数据库中
- 使用 DataStore 存储当前选中的 Skill ID
- 可以使用本地的mysql去存储数据，当前开发环境的mysql的密码是 123456
- 自行创建数据库和数据库表进行存储
- 自行选择数据存储方式

### 2.2 聊天功能

#### 2.2.1 聊天界面
- 顶部栏：显示当前 Skill 名称 + 头像，以及 Skill 切换按钮
- 消息列表：用户消息和 AI 回复交替显示
- 输入框：底部固定，支持多行输入和发送
- 消息气泡：用户消息右对齐（蓝色），AI 消息左对齐（灰色）
- 流式渲染：AI 回复逐 token 显示，打字机效果

#### 2.2.2 聊天上下文
- 一次会话内保留完整上下文（消息列表）
- 上下文包含：system prompt + 历史消息
- 切换 Skill 时不清除历史消息，但后续回复遵循新 Skill 的风格
- 提供"清空上下文"/"新建对话"功能

#### 2.2.3 流式输出
- 所有模型回复必须通过流式接口实时渲染
- 使用 Kotlin Flow 封装流式数据
- UI 层通过 collect 流式数据更新 Compose 状态
- 流式渲染时显示光标闪烁动画
- 支持中断生成（停止按钮）

### 2.3 模型配置

#### 2.3.1 远程模型配置
- Base URL 配置（可编辑）
- API Key 配置（可编辑，使用 DataStore 加密存储）
- 模型名称选择/输入（下拉选择 + 自定义输入）
- 可配置参数：Temperature, Max Tokens, Top P
- **预填充配置**（来自需求文档）：
  - Base URL: `https://api.deepseek.com/anthropic`
  - API Key: `sk-043367a2c3e64df9977d0d3062bfuck7`
  - 模型: `DeepSeek-V4-Flash`

#### 2.3.2 本地模型配置（预留）
- Ollama Base URL 配置（默认 `http://localhost:11434`）
- 模型名称选择（通过 Ollama API 拉取可用模型列表）
- 可配置参数：Temperature, Context Length
- **注意：本地模型功能暂不实现，UI 配置入口保留但标注"开发中"**

#### 2.3.3 模型切换
- 在聊天界面或设置中切换当前使用的模型（本地/远程）
- 切换模型不影响聊天历史
- 当前模型不可用时（如 Ollama 未运行）显示错误提示

### 2.4 设置页面

#### 2.4.1 设置项
- 模型配置（远程模型配置表单）
- 本地模型配置（预留，显示"即将推出"）
- 默认 Temperature
- 聊天字体大小
- 关于页面

---

## 3. 架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  Screens / ViewModels / States / Effects │
├─────────────────────────────────────────┤
│           Domain Layer (纯 Kotlin)       │
│  UseCases / Repository 接口 / Model      │
├─────────────────────────────────────────┤
│            Data Layer                    │
│  RepositoryImpl / ApiService / LocalDB   │
│  DataStore / DTO / Mapper               │
└─────────────────────────────────────────┘
```

### 3.2 关键组件

#### 3.2.1 Skill 相关
- `SkillRepository` — Skill 的 CRUD 操作
- `SkillManager` — 当前 Skill 状态管理（单例）
- `SkillSelectorSheet` — Skill 切换底部弹窗

#### 3.2.2 聊天相关
- `ChatRepository` — 消息持久化和检索
- `ChatViewModel` — 聊天状态管理，消息发送/接收逻辑
- `ChatScreen` — 聊天界面 Composable
- `MessageBubble` — 消息气泡 Composable

#### 3.2.3 模型相关
- `ModelClient` (接口) — 统一模型调用抽象
  - `RemoteModelClient` — DeepSeek API 调用
  - `LocalModelClient` — Ollama API 调用（预留）
- `ModelConfigRepository` — 模型配置存储
- `StreamingParser` — 流式响应解析

### 3.3 包结构

```
com.needai.chat/
├── app/
│   └── NeedAiApplication.kt
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── DatabaseModule.kt
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── dao/
│   │   │   │   ├── SkillDao.kt
│   │   │   │   └── MessageDao.kt
│   │   │   └── entity/
│   │   │       ├── SkillEntity.kt
│   │   │       └── MessageEntity.kt
│   │   └── datastore/
│   │       ├── SettingsDataStore.kt
│   │       └── ModelConfigDataStore.kt
│   ├── remote/
│   │   ├── api/
│   │   │   ├── DeepSeekApi.kt (Retrofit 接口)
│   │   │   └── OllamaApi.kt (预留)
│   │   ├── dto/
│   │   │   ├── ChatRequest.kt
│   │   │   └── ChatResponse.kt
│   │   └── client/
│   │       ├── ModelClient.kt (接口)
│   │       ├── RemoteModelClient.kt
│   │       └── LocalModelClient.kt (预留)
│   ├── repository/
│   │   ├── SkillRepositoryImpl.kt
│   │   ├── ChatRepositoryImpl.kt
│   │   └── ModelConfigRepositoryImpl.kt
│   └── mapper/
│       ├── SkillMapper.kt
│       └── MessageMapper.kt
├── domain/
│   ├── model/
│   │   ├── Skill.kt
│   │   ├── Message.kt
│   │   ├── ModelConfig.kt
│   │   └── ModelType.kt
│   ├── repository/
│   │   ├── SkillRepository.kt (接口)
│   │   ├── ChatRepository.kt (接口)
│   │   └── ModelConfigRepository.kt (接口)
│   └── usecase/
│       ├── SendMessageUseCase.kt
│       ├── GetSkillsUseCase.kt
│       ├── SwitchSkillUseCase.kt
│       └── GetChatHistoryUseCase.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   ├── ChatViewModel.kt
│   │   ├── components/
│   │   │   ├── MessageBubble.kt
│   │   │   ├── ChatInputBar.kt
│   │   │   ├── SkillSelectorSheet.kt
│   │   │   └── StreamingText.kt
│   │   └── state/
│   │       └── ChatUiState.kt
│   ├── skills/
│   │   ├── SkillListScreen.kt
│   │   ├── SkillEditScreen.kt
│   │   ├── SkillViewModel.kt
│   │   └── components/
│   │       └── SkillCard.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       ├── SettingsViewModel.kt
│       └── components/
│           └── ModelConfigForm.kt
└── util/
    ├── EncryptUtil.kt
    └── Constants.kt
```

---

## 4. 数据模型

### 4.1 Room Entity

#### SkillEntity
```kotlin
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val systemPrompt: String,
    val greeting: String,
    val temperature: Double,
    val tags: String, // JSON array
    val isBuiltin: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### MessageEntity
```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val skillId: String?,
    val timestamp: Long,
    val isStreaming: Boolean // 标记是否正在流式输出
)
```

### 4.2 Domain Model

```kotlin
data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val avatar: String,
    val systemPrompt: String,
    val greeting: String,
    val temperature: Double = 0.7,
    val tags: List<String> = emptyList(),
    val isBuiltin: Boolean = false
)

data class Message(
    val id: Long = 0,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val skillId: String? = null,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

enum class ModelType { REMOTE, LOCAL }

data class ModelConfig(
    val modelType: ModelType = ModelType.REMOTE,
    val remoteBaseUrl: String = "https://api.deepseek.com/anthropic",
    val remoteApiKey: String = "sk-043367a2c3e64df9977d0d3062b08887",
    val remoteModelName: String = "DeepSeek-V4-Flash",
    val localBaseUrl: String = "http://localhost:11434",
    val localModelName: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val topP: Double = 1.0
)
```

---

## 5. API 接口设计

### 5.1 远程模型接口（OpenAI 兼容格式）

DeepSeek API 兼容 OpenAI 的 chat completions 接口格式：

```
POST {baseUrl}/v1/chat/completions
Headers:
  Authorization: Bearer {apiKey}
  Content-Type: application/json

Request Body:
{
  "model": "DeepSeek-V4-Flash",
  "messages": [
    {"role": "system", "content": "你是一个友好的助手..."},
    {"role": "user", "content": "你好"},
    {"role": "assistant", "content": "你好！有什么可以帮你的？"},
    {"role": "user", "content": "今天天气怎么样？"}
  ],
  "stream": true,
  "temperature": 0.7,
  "max_tokens": 4096,
  "top_p": 1.0
}
```

流式响应格式（SSE / `text/event-stream`）：
```
data: {"choices":[{"delta":{"role":"assistant","content":"你"},"index":0}]}

data: {"choices":[{"delta":{"content":"好"},"index":0}]}

data: {"choices":[{"delta":{"content":"的"},"index":0}]}

data: [DONE]
```

### 5.2 Ollama 接口（预留）

```
POST {baseUrl}/api/chat
{
  "model": "llama3",
  "messages": [...],
  "stream": true
}
```

流式响应：
```
{"message":{"role":"assistant","content":"你"},"done":false}
{"message":{"role":"assistant","content":"好"},"done":false}
{"message":{"role":"assistant","content":"的"},"done":false}
{"done":true}
```

### 5.3 ModelClient 接口

```kotlin
interface ModelClient {
    fun streamChat(
        messages: List<ChatMessage>,
        config: ModelConfig,
        skill: Skill
    ): Flow<String> // 每个 emit 为一个增量 token
    
    suspend fun validateConfig(config: ModelConfig): Result<Boolean>
}
```

---

## 6. 实现方案 — 让模型遵循 Skill

### 6.1 核心机制：System Prompt

将 Skill 的 `systemPrompt` 字段作为 messages 数组的第一条 system 消息发送：

```
messages: [
  {"role": "system", "content": "<Skill.systemPrompt>"},
  ...历史对话
]
```

### 6.2 远程模型（DeepSeek）
- DeepSeek API（兼容 OpenAI 格式）原生支持 `system` role
- 将 Skill 的 systemPrompt 直接映射到 `messages[0].content`
- 无需额外处理

### 6.3 本地模型（Ollama — 预留）
- Ollama `/api/chat` 也支持 `system` role 或 `system` 字段
- 兼容方式：在消息列表头部插入 system 消息
- 对于不支持 system role 的模型，将 systemPrompt 拼接到第一条 user 消息前作为指令

### 6.4 结论
- **推荐方案**：利用 API 原生 system role 支持，无需额外封装
- 切换 Skill 时，更新 system prompt，后续请求带上新的 system prompt + 完整历史
- 无需修改模型本身，完全通过 prompt engineering 实现

---

## 7. 流式渲染方案

### 7.1 数据流

```
ModelClient.streamChat()
  → Flow<String> (增量 token)
  → ChatViewModel 中 collect，拼接完整回复
  → 更新 UiState 中的 currentStreamingMessage
  → Compose 重组渲染
```

### 7.2 UiState 设计

```kotlin
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentStreamingMessage: String = "", // 当前正在流式输出的内容
    val isStreaming: Boolean = false,
    val currentSkill: Skill = defaultSkill,
    val availableSkills: List<Skill> = emptyList(),
    val currentModel: ModelType = ModelType.REMOTE,
    val error: String? = null,
    val isLoading: Boolean = false
)
```

### 7.3 StreamingText Composable
- 接收 `streamingText: String` 参数
- 使用 `animateContentSize` 实现平滑更新
- 显示闪烁光标（`|` 动画）表示正在生成
- 流式完成后光标消失，内容转为普通消息气泡

---

## 8. 导航设计

```
NavGraph:
├── ChatScreen (首页/主界面)
│   ├── SkillSelectorSheet (底部弹窗)
│   └── ModelSwitcher (顶部/设置入口)
├── SkillListScreen (技能管理)
│   └── SkillEditScreen (创建/编辑技能)
└── SettingsScreen (设置)
    └── 内嵌 ModelConfigForm
```

底部导航栏（可选）：
| 图标 | 标签 | 页面 |
|------|------|------|
| chat | 聊天 | ChatScreen |
| auto_awesome | 技能 | SkillListScreen |
| settings | 设置 | SettingsScreen |

---

## 9. 开发规范

### 9.1 编码规范
- 遵循 **Kotlin Coding Conventions** (https://kotlinlang.org/docs/coding-conventions.html)
- 使用 `camelCase` 命名变量和函数
- 使用 `PascalCase` 命名类和接口
- Compose 函数命名以 `Screen` / `Component` 结尾
- ViewModel 命名以 `ViewModel` 结尾
- Repository 接口以 `Repository` 结尾，实现以 `Impl` 结尾

### 9.2 Compose 规范
- 每个 Composable 函数必须有 KDoc 注释说明用途
- 使用 `State` / `StateFlow` 管理 UI 状态，避免直接传可变数据
- 使用 `remember` / `derivedStateOf` 优化重组
- 主题色使用 Material 3 的 color scheme
- 深色模式支持（跟随系统）

### 9.3 错误处理
- 网络错误：显示 Snackbar 提示，提供重试按钮
- API Key 无效：明确提示"API Key 配置有误"
- 模型不可用：明确提示"当前模型不可用，请检查配置"
- 所有错误信息使用中文显示

### 9.4 测试要求
- 每个 ViewModel 编写单元测试（使用 Turbine 测试 Flow）
- Repository 编写集成测试（使用 Fake 数据源）
- 关键 Ui 组件编写 Compose 测试
- 完成后需测试的场景清单：
  - [ ] 发送消息并接收流式回复
  - [ ] 切换 Skill 后回复风格改变
  - [ ] 切换模型类型
  - [ ] 修改模型配置
  - [ ] 创建/编辑/删除自定义 Skill
  - [ ] 中断流式输出
  - [ ] 清空聊天历史
  - [ ] 配置错误时显示正确提示
  - [ ] 深色模式切换

---

## 10. 实施优先级

### Phase 1 — 基础框架（必做）
1. 项目初始化（Gradle 配置、Hilt、Room、Retrofit）
2. 主题和导航框架
3. Room 数据库 + DataStore
4. Domain 模型 + Repository 接口
5. 远程模型客户端（DeepSeek API 对接）

### Phase 2 — 核心功能（必做）
6. 聊天界面（ChatScreen + ChatViewModel）
7. 流式渲染（StreamingText）
8. Skill 管理（内置 Skill 加载 + CRUD）
9. Skill 切换功能
10. 模型配置页面

### Phase 3 — 完善（必做）
11. 设置页面
12. 错误处理和 Loading 状态
13. 单元测试

### Phase 4 — 可选
14. 本地 Ollama 模型支持
15. 导出聊天记录
16. 多语言支持

---

## 11. 特殊说明

1. **API Key 安全**：API Key 存储在 DataStore 中，使用 Android Keystore 加密
2. **网络权限**：AndroidManifest 声明 `INTERNET` 权限
3. **本地模型**：Phase 1-3 不实现本地模型功能，所有相关 UI 标注"即将推出"
4. **当前对话**：使用 `sessionId` (UUID) 标识一次对话，切换 Skill 不重置 sessionId
5. **内置 Skill**：必须包含 3 个以上内置 Skill，在 `Application.onCreate` 中初始化到数据库
6. **预填配置**：第一次启动时使用需求文档提供的 DeepSeek 配置作为默认值
7. **所有 UI 文本使用中文**

---

## 12. 验收标准
1. 所有的需求均开发完毕
2. 测试通过
3. 功能正常

---

*本文档由 base.md 自动生成，供 AI 自动开发使用。*
