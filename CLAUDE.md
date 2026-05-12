# CLAUDE.md

未来 Claude 实例请按此文档行事，别像个刚毕业的产品经理一样瞎搞。

## 构建 & 测试

```bash
./gradlew assembleDebug          # 搓一个 debug 包
./gradlew test                   # 跑所有单元测试
./gradlew test --tests "com.needai.chat.ui.chat.ChatViewModelTest"  # 跑单个测试
./gradlew lintDebug              # 看看 lint 有没有意见
./gradlew clean assembleDebug    # 从头再来
```

需要 JDK 17+ 和 Android SDK 36。`local.properties` 里配 SDK 路径，别来问我。

## 这项目啥结构

就一个 `:app` 模块，别指望啥 `:core` `:feature` 多模块优雅架构，不存在的。按包名分层：

- **`domain/`** — 纯 Kotlin，没有 Android 依赖。放了 Model（Message/ChatSession/Skill/ModelConfig）、仓库接口、用例。理想很丰满。
- **`data/`** — Room 本地数据库（4 张表，version 4，毁灭性迁移）、DataStore 存配置、OkHttp SSE 流式怼 AI 接口、仓库实现、导出导入工具。
- **`ui/`** — Jetpack Compose + Material 3。每个页面一个 `@HiltViewModel`，`StateFlow<UiState>` 怼到 UI，Navigation Compose 切页，底部 4 个 tab。
- **`di/`** — Hilt 三板斧: AppModule（`@Binds` 绑接口）、DatabaseModule（提供 DAO）、NetworkModule（提供 Gson）。
- **`util/`** — Constants、EncryptUtil（下面会讲这个坑）。

## 代码里的坑（记好了）

### EncryptUtil 的 IV 持久化 bug
`encrypt()` 每次随机生成 IV 但没跟密文一起存，`decrypt()` 根本解不出来。API Key 加密存了等于没存。谁要有空修一下。

### ChatViewModel 不用 DI
明明 Hilt 注入了一个 `ModelClient`，`sendMessage()` 里却自己 `new RemoteModelClient(Gson())`。属于是脱裤子放屁。

### SendMessageUseCase 写了没用
`domain/usecase/` 里定义得好好的，但没有任何 ViewModel 用 — 发消息逻辑直接写在 ViewModel 里了。

### DeepSeekApi（Retrofit 接口）是 dead code
`data/remote/api/DeepSeekApi.kt` — 定义了一个 Retrofit 接口，但实际请求用的是 OkHttp 直连。主打一个写了等于没写。

### 统计页面入口隐藏
StatsScreen 有路由有页面，但不在底部导航里。你要导航过去直接 `navController.navigate("stats")` 就行，别问为什么隐藏，问就是故意。

## 数据库（Room v4，毁灭性迁移）

4 张表，升级版本号就直接删数据，别指望写 migration：

- **skills** — 技能/角色预设。字段：id、name、description、avatar、systemPrompt、greeting、temperature、tags（JSON 字符串存数组）、isBuiltin、createdAt、updatedAt
- **messages** — 聊天消息。字段：自增 id、sessionId、role（USER/ASSISTANT/SYSTEM）、content、skillId（可空）、timestamp、isStreaming、promptTokens/completionTokens/totalTokens（都可空）、modelConfigId（可空）
- **sessions** — 会话。字段：id、skillId、title、createdAt、updatedAt
- **model_configs** — 模型配置。字段：id、name、protocol（OPENAI/ANTHROPIC）、remoteBaseUrl、remoteApiKey、remoteModelName、temperature、maxTokens、topP、isBuiltin、createdAt、updatedAt

## 支持的 AI 协议

- **OpenAI 兼容** — 标准的 `/v1/chat/completions` + SSE data 块
- **Anthropic** — Messages API + SSE event 流
- 内置 11 家供应商预设，在 `KnownProvider` 里

## 测试

- JUnit 4 + kotlinx-coroutines-test，没用 mock 框架，全是 Fake 仓库
- Turbine 在 dependencies 里声明了但还没用上
- 纯 JVM 测试，不用模拟器
- 跑测试前确认 FakeRepository 们跟实际接口对得上，经常改了接口忘了改 Fake

## 其他你该知道的

- **全 UI 中文**，别手贱加英文
- **Release 包？不存在的。** 没有签名配置，没有混淆，没有 CI
- `NeedAiApplication` 里硬编码了一个过期的阿里云 API Key，**别拿这个去提 Issue 说连不上**
- 首次启动会从 assets 复制 `model_config.json` 并初始化内置技能和体验模型配置
- 内置技能（isBuiltin=true）不可编辑不可删除
