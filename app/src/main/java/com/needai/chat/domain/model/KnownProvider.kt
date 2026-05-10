package com.needai.chat.domain.model

data class KnownProvider(
    val name: String,
    val displayName: String,
    val protocol: ApiProtocol,
    val defaultBaseUrl: String,
    val defaultModelName: String = ""
)

val knownProviders = listOf(
    KnownProvider("openai", "OpenAI", ApiProtocol.OPENAI, "https://api.openai.com", "gpt-4o"),
    KnownProvider("deepseek", "DeepSeek", ApiProtocol.OPENAI, "https://api.deepseek.com", "deepseek-chat"),
    KnownProvider("anthropic", "Anthropic", ApiProtocol.ANTHROPIC, "https://api.anthropic.com", "claude-sonnet-4-20250514"),
    KnownProvider("moonshot", "Moonshot (月之暗面)", ApiProtocol.OPENAI, "https://api.moonshot.cn", "moonshot-v1-8k"),
    KnownProvider("baidu", "百度千帆", ApiProtocol.OPENAI, "https://qianfan.baidubce.com", "ernie-4.0"),
    KnownProvider("aliyun", "阿里通义千问", ApiProtocol.OPENAI, "https://dashscope.aliyuncs.com/compatible-mode", "qwen-turbo"),
    KnownProvider("zhipu", "智谱 GLM", ApiProtocol.OPENAI, "https://open.bigmodel.cn/api/paas/v4", "glm-4"),
    KnownProvider("google", "Google Gemini", ApiProtocol.OPENAI, "https://generativelanguage.googleapis.com", "gemini-2.0-flash"),
    KnownProvider("xai", "xAI Grok", ApiProtocol.OPENAI, "https://api.x.ai", "grok-2"),
    KnownProvider("siliconflow", "SiliconFlow (硅基流动)", ApiProtocol.OPENAI, "https://api.siliconflow.cn", "deepseek-ai/DeepSeek-V3"),
)
