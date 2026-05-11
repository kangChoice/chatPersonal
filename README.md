# Need AI Chat

<div align="center">

**一个让你忍不住想和 AI 谈恋爱的 Android 聊天应用**

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![API](https://img.shields.io/badge/API%2026-36-brightgreen?style=flat-square)

</div>

---

## 这玩意儿能干啥？

你有没有过这样的时刻——想找个人聊天，但身边的人都忙着刷短视频？或者想找个知心姐姐/哥哥，但又不好意思开口？再或者，单纯想体验一下被 AI 怼到怀疑人生的快感？

**Need AI Chat** 就是为你准备的。

这是一个基于 Kotlin + Jetpack Compose 构建的 Android 聊天应用，支持 OpenAI 和 Anthropic 双协议，内置 11 家主流 AI 供应商一键配置，还有各种奇奇怪怪的"技能"（角色预设），让你和 AI 的对话不再枯燥乏味。

---

## 功能特性

### 🎭 多角色技能系统

内置角色预设（我们叫它"技能"），你可以：

- **使用内置技能** — 比如"作者本人"这个高冷角色，不管你聊什么，回复永远只有一句话：_"无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）"_
- **创建自定义角色** — 设置名字、头像、招呼语、系统提示词，甚至还能调温度（Temperature），放飞想象力吧
- **批量导出/导入** — 多选技能一键导出，或者从 JSON 文件批量导入，跟兄弟们分享你的调教成果
- **技能级联删除** — 删技能时会顺便把相关的会话和消息也干掉，不留痕迹

### 🤖 多模型支持

- **双协议**：支持 OpenAI 协议和 Anthropic 协议的各种大模型
- **一键配置**：内置了 11 家常见供应商的预设（OpenAI、DeepSeek、Anthropic、月之暗面、百度千帆、阿里通义千问、智谱 GLM、Google Gemini、xAI Grok、硅基流动），不用记那些该死的 API 地址
- **快速创建**：两步完成——选供应商，填 API Key，完事
- **本地模型预留**：Ollama 等本地部署接口已预留（虽然目前还是个占位符，"功能开发中，敬请期待"）
- **内置体验模型**：内置了一个阿里通义千问 qwen-max 的体验配置，开箱即用（虽然内置的 API Key 已经失效了，但你可以用自己的）

### 🔐 API Key 安全存储

API Key 使用 Android Keystore 加密存储（AES/GCM），硬件级加密，比你存便签里安全一万倍。

### 🎨 暗黑模式

- 支持明亮/暗黑模式切换
- 暗黑模式下也能看清聊天内容（是的，我们修好了这个 bug）

### 💬 会话管理

- 多会话支持，随时切换（底部抽屉随意浏览历史会话）
- 导出/导入会话记录（Markdown 格式）
- 清空上下文，让 AI 失忆
- 删除会话记录（删前会问你"确定吗？"）

### 🔧 模型配置管理

- 支持多个模型配置，一键切换
- 支持自定义 API 地址（适合那些搞反向代理的骚操作）
- 生成参数调节：温度、最大 Token 数、Top P
- 导出/导入配置（JSON 格式）
- 内置配置不可编辑/删除（防止你手残）

### 🤖 AI 提示词润色

- 输入一段描述，AI 帮你扩写成详细的系统提示词（500-1200 字）
- 滚动到两端都有 FAB（浮动按钮），贴心吧
- 一键创建对应的技能角色
- 妈妈再也不用担心我不会写 prompt 了

### 📊 统计功能

- 统计你的聊天 Token 消耗量
- 按会话、模型、时间范围（7天/30天/全部）过滤
- 大数字自动格式化（K/M 单位）
- **入口已隐藏** — 你找不到它，但它确实存在（不信你翻代码）

### 🆕 新手指引

第一次用？不知道这些按钮是干嘛的？内置了 4 步引导教程：

1. **聊天页面** — 教你切换技能，认认那个警告图标
2. **提示词润色** — 展示 AI 写 prompt 的魔力
3. **技能管理** — 怎么创建、编辑、导入导出
4. **设置与模型配置** — 教你怎么接上 AI 模型

从设置页面可以随时重新打开引导。

---

## 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 主要开发语言，不会 Kotlin 的出门左转 Java |
| **Jetpack Compose** | UI 框架，写界面像写诗一样优雅（并不） |
| **Material 3** | 设计语言，你懂的那个 Material You |
| **Hilt** | 依赖注入，让对象之间的暧昧关系变得清晰 |
| **Room** | 本地数据库，你的聊天记录永远属于你 |
| **DataStore** | 配置存储，比 SharedPreferences 体面多了 |
| **OkHttp + SSE** | 网络请求 + 流式响应，AI 打字效果拉满 |
| **Navigation Compose** | 页面导航，不会迷路 |
| **Android Keystore** | API Key 加密，妈妈再也不用担心我的 key 泄露 |
| **Gson** | JSON 解析，朴实无华且枯燥 |

---

## 项目结构

```
app/src/main/java/com/needai/chat/
├── app/                    # Application 类（启动初始化 + Hilt 入口）
├── data/
│   ├── export/             # 导出工具（Markdown 会话 / JSON 配置 + 技能）
│   ├── import/             # 导入工具（解析 Markdown 和 JSON 文件）
│   ├── local/
│   │   ├── db/             # Room 数据库（skills/messages/sessions/model_configs）
│   │   └── datastore/      # DataStore（设置 + 模型配置持久化）
│   ├── mapper/             # 实体 ↔ 领域模型映射器
│   ├── remote/
│   │   ├── client/         # ModelClient（RemoteModelClient SSE 流式 + LocalModelClient 占位符）
│   │   ├── dto/            # OpenAI & Anthropic 协议请求/响应 DTO
│   │   └── api/            # Retrofit 接口（虽然写了但没用，主打一个写了等于没写）
│   └── repository/         # 仓库实现
├── di/                     # Hilt 依赖注入模块（AppModule / DatabaseModule / NetworkModule）
├── domain/
│   ├── model/              # 领域模型（Message / ChatSession / Skill / ModelConfig ...）
│   ├── repository/         # 仓库接口（抽象的艺术）
│   └── usecase/            # 业务用例（发消息、查历史、切换技能...）
├── ui/
│   ├── chat/               # 聊天页面（核心！核心！核心！）
│   │   └── components/     # ChatInputBar / MessageBubble / StreamingText / SkillSelectorSheet / HistorySessionSheet
│   ├── skills/             # 技能管理页面（CRUD + 批量导出 + 导入）
│   ├── prompt/             # 提示词润色页面（AI 帮你写 prompt）
│   ├── settings/           # 设置页面（模型配置 + 暗黑模式 + 新手指引）
│   │   └── components/     # ModelConfigEditDialog / QuickCreateProviderDialog / GenerationParamsDialog
│   ├── stats/              # 统计页面（入口已隐藏，但代码还在）
│   ├── onboarding/         # 新手指引覆盖层（4 步引导教程）
│   ├── navigation/         # 导航图 + 底部导航栏
│   └── theme/              # 主题配置（Color / Type / Theme）
└── util/                   # 工具类（Constants / EncryptUtil）
```

---

## 快速开始

1. 克隆本仓库：
   ```bash
   git clone https://github.com/kangChoice/chatPersonal.git
   ```

2. 用 Android Studio 打开项目（建议用最新版，旧版本可能会哭）

3. 等 Gradle 同步完（去泡杯咖啡，或者泡面，看你）

4. 点击 Run，然后就完事了

> **注意**：首次启动会自动初始化内置技能和体验模型配置。如果你之前装过旧版本，可能需要清除应用数据（因为数据库版本升级了，我们懒得写迁移脚本，直接毁灭性迁移，舒服）。

---

## 常见问题

**Q：内置的体验模型为什么用不了？**

A：因为内置的 API Key 已经失效了。你需要自己去阿里云申请一个 Key，或者在设置里换成你自己的供应商。

**Q：本地模型能用吗？**

A：不能。我们留了接口，但还没实现。如果你实在想用，欢迎提 PR。

**Q：统计功能在哪里？**

A：不告诉你。自己找。

---

## 截图

> 截图？自己跑一下看看不就知道了，我又不会贴图。

---

## 贡献

欢迎提 Issue 和 PR，只要你写的代码比我好（应该不难）。

---

## License

MIT License — 你想干嘛都行，但别说是你写的。

---

<div align="center">

**哥只是个传说~，叫哥哥**

[给作者 Star](https://github.com/kangChoice/chatPersonal) ⭐ — 你不点也没关系，反正作者会偷偷看谁点了的。

</div>
