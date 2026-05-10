# Need AI Chat

<div align="center">

**一个让你忍不住想和 AI 谈恋爱的 Android 聊天应用**

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

</div>

---

## 这玩意儿能干啥？

你有没有过这样的时刻——想找个人聊天，但身边的人都忙着刷短视频？或者想找个知心姐姐/哥哥，但又不好意思开口？再或者，单纯想体验一下被 AI 怼到怀疑人生的快感？

**Need AI Chat** 就是为你准备的。

这是一个基于 Kotlin + Jetpack Compose 构建的 Android 聊天应用，支持多种 AI 模型接入，内置了各种奇奇怪怪的 "技能"（角色预设），让你和 AI 的对话不再枯燥乏味。

## ✨ 功能特性

### 🎭 多角色技能系统
内置了角色预设（我们叫它"技能"），目前有：

- **作者本人** 😎 — 无论你聊什么，回复只有一句话：_"无趣的人，你的风趣不及作者的万分之一。处吗~~~（气泡音）"_。是的，就是这么高冷。
- 还有你自己创建的各种奇怪角色，放飞想象力吧。

### 🤖 多模型支持
- **远程模型**：支持 OpenAI 协议和 Anthropic 协议的各种大模型
- **本地模型**：支持 Ollama 等本地部署的模型
- **快速创建**：内置了 10+ 常见供应商的一键配置，不用记那些该死的 API 地址

### 🎨 暗黑模式
- 支持明亮/暗黑模式切换
- 暗黑模式下也能看清聊天内容（是的，我们修好了这个 bug）

### 💬 会话管理
- 多会话支持，随时切换
- 导出/导入会话记录（Markdown 格式）
- 清空上下文，让 AI 失忆

### 🔧 模型配置管理
- 支持多个模型配置，一键切换
- 导出/导入配置（JSON 格式）
- **内置体验模型**：内置了一个通义千问的体验配置，开箱即用（感谢阿里云爸爸的兼容接口）

### 🤖 AI 提示词润色
- 输入一段描述，AI 帮你扩写成详细的系统提示词
- 一键创建对应的技能角色
- 妈妈再也不用担心我不会写 prompt 了

### 📊 统计功能
- 统计你的聊天数据（虽然我也不知道统计了啥，但它就是有）

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 主要开发语言 |
| **Jetpack Compose** | UI 框架，写界面像写诗一样优雅 |
| **Material 3** | 设计语言，你懂的那个 Material You |
| **Hilt** | 依赖注入，让对象之间的暧昧关系变得清晰 |
| **Room** | 本地数据库，你的聊天记录永远属于你 |
| **DataStore** | 配置存储，比 SharedPreferences 体面多了 |
| **OkHttp + SSE** | 网络请求 + 流式响应，AI 打字效果拉满 |
| **Navigation Compose** | 页面导航，不会迷路 |

---

## 🚀 快速开始

1. 克隆本仓库：
   ```bash
   git clone https://github.com/kangChoice/chatPersonal.git
   ```

2. 用 Android Studio 打开项目（建议用最新版，旧版本可能会哭）

3. 等 Gradle 同步完（去泡杯咖啡，或者泡面，看你）

4. 点击 Run，然后就完事了

> **注意**：首次启动会自动初始化内置技能和体验模型配置。如果你之前装过旧版本，可能需要清除应用数据（因为数据库版本升级了，我们懒得写迁移脚本，直接毁灭性迁移，舒服）。

---

## 📁 项目结构

```
app/
├── src/main/
│   ├── assets/
│   │   ├── app.png              # 应用图标（我长这样）
│   │   └── model_config.json    # 默认模型配置
│   └── java/com/needai/chat/
│       ├── app/                 # Application 类（启动初始化）
│       ├── data/
│       │   ├── export/          # 导出工具（把你的聊天记录变成文件）
│       │   ├── import/          # 导入工具（把文件变成你的聊天记录）
│       │   ├── local/           # 本地数据层（Room + DataStore）
│       │   ├── mapper/          # 数据映射器（实体 ↔ 领域模型）
│       │   ├── remote/          # 远程数据层（OkHttp + SSE）
│       │   └── repository/      # 仓库实现（数据来源的中间人）
│       ├── di/                  # 依赖注入模块（Hilt 的魔法）
│       ├── domain/
│       │   ├── model/           # 领域模型（灵魂所在）
│       │   ├── repository/      # 仓库接口（抽象的艺术）
│       │   └── usecase/         # 业务用例（具体能干啥）
│       ├── ui/
│       │   ├── chat/            # 聊天页面（核心功能区）
│       │   ├── prompt/          # 提示词润色页面（AI 帮你写 prompt）
│       │   ├── settings/        # 设置页面（啥都能调）
│       │   ├── skills/          # 技能管理页面（角色扮演中心）
│       │   ├── stats/           # 统计页面（数据控的最爱）
│       │   └── theme/           # 主题配置（好看就完事了）
│       └── util/                # 工具类（啥都有）
```

---

## 📸 截图

> 截图？自己跑一下看看不就知道了，我又不会贴图。

---

## 🤝 贡献

欢迎提 Issue 和 PR，只要你写的代码比我好（应该不难）。

---

## 📄 License

MIT License — 你想干嘛都行，但别说是你写的。

---

<div align="center">

**哥只是个传说~，叫哥哥**

[给作者 Star](https://github.com/kangChoice/chatPersonal) ⭐ — 你不点也没关系，反正作者会偷偷看谁点了的。

</div>
