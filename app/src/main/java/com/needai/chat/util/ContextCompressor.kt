package com.needai.chat.util

import com.needai.chat.data.remote.client.RemoteModelClient
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.Message
import com.needai.chat.domain.model.MessageRole
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.usecase.ChatMessage
import kotlinx.coroutines.flow.collect

/**
 * 上下文压缩器。
 * 在对话过长时将早期消息压缩为摘要，释放上下文窗口空间。
 *
 * 使用方式：
 *   ContextCompressor.compress(modelClient, config, messages, existingSummary)
 *   → 返回摘要文本
 */
object ContextCompressor {

    private const val TAG = "ContextCompressor"

    /** 摘要最大字符数 */
    private const val SUMMARY_MAX_CHARS = 500

    /** 压缩触发：消息达到此数量才压缩 */
    const val MIN_UNCOMPRESSED_COUNT = 10

    /** 输出预留：留 30% 窗口给模型输出 */
    private const val OUTPUT_RESERVE_RATIO = 0.3

    /** 保留原文比例：压缩后保留多少比例的最近消息 */
    private const val KEEP_RATIO = 0.5

    // ======================================================================
    // Token 估算
    // ======================================================================

    /**
     * 估算请求输入部分的 token 数。
     * 中文字符 ≈ 1.5 token，英文字符 ≈ 0.3 token，每条消息 +4 token 格式开销。
     */
    fun estimateInputTokens(
        systemPrompt: String,
        summary: String?,
        messages: List<Message>,
        currentInput: String
    ): Int {
        var total = 0

        // system prompt
        total += tokenCount(systemPrompt) + 4

        // summary
        if (summary != null) {
            total += tokenCount(summary) + 4
        }

        // messages
        for (msg in messages) {
            total += tokenCount(msg.content) + 4
        }

        // current input
        total += tokenCount(currentInput) + 4

        return total
    }

    /**
     * 估算 ChatMessage 列表的 token 数（用于构建请求前的检查）。
     */
    fun estimateTokenCountForMessages(messages: List<ChatMessage>): Int {
        var total = 0
        for (msg in messages) {
            total += tokenCount(msg.content) + 4
        }
        return total
    }

    private fun tokenCount(text: String): Int {
        var chinese = 0
        var other = 0
        for (c in text) {
            if (c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF) {
                chinese++
            } else {
                other++
            }
        }
        return (chinese * 1.5 + other * 0.3).toInt()
    }

    // ======================================================================
    // 压缩触发判断
    // ======================================================================

    /**
     * 判断是否需要压缩。
     * @param estimatedInputTokens 估算的输入 token 数
     * @param contextWindow 模型上下文窗口大小
     * @param uncompressedCount 未压缩的消息数（最近的消息）
     */
    fun shouldCompress(
        estimatedInputTokens: Int,
        contextWindow: Int,
        uncompressedCount: Int
    ): Boolean {
        if (uncompressedCount < MIN_UNCOMPRESSED_COUNT) return false
        val outputReserve = (contextWindow * OUTPUT_RESERVE_RATIO).toInt()
        return estimatedInputTokens > contextWindow - outputReserve
    }

    // ======================================================================
    // 压缩范围选择
    // ======================================================================

    /**
     * 选择压缩范围，返回待压缩的文本列表和保留的消息列表。
     *
     * 保留最新的 KEEP_RATIO 比例的消息原文，
     * 压缩最早的部分（如果已有摘要则与摘要一起压缩）。
     */
    fun selectCompressionRange(
        messages: List<Message>,
        existingSummary: String?
    ): CompressionSelection {
        val splitIndex = (messages.size * KEEP_RATIO).toInt().coerceAtLeast(1)
        val toCompress = messages.take(splitIndex)
        val toKeep = messages.drop(splitIndex)

        val input = buildString {
            if (existingSummary != null) {
                appendLine("[上一轮对话摘要]")
                appendLine(existingSummary)
                appendLine()
            }
            appendLine("[待压缩的对话]")
            for (msg in toCompress) {
                val roleLabel = when (msg.role) {
                    MessageRole.USER -> "用户"
                    MessageRole.ASSISTANT -> "AI"
                    MessageRole.SYSTEM -> "系统"
                }
                appendLine("$roleLabel: ${msg.content}")
            }
        }

        return CompressionSelection(
            compressInput = input,
            toKeep = toKeep,
            summaryEndMessageId = toKeep.firstOrNull()?.id?.minus(1)
                ?: messages.lastOrNull()?.id ?: -1L
        )
    }

    data class CompressionSelection(
        val compressInput: String,
        val toKeep: List<Message>,
        val summaryEndMessageId: Long
    )

    // ======================================================================
    // 压缩 prompt
    // ======================================================================

    /** 压缩 prompt，发给模型让模型输出摘要 */
    private val COMPRESSION_PROMPT = buildString {
        appendLine("你是对话摘要助手。请将以下对话内容压缩为一段简洁的中文摘要（不超过300字）。")
        appendLine("请保留以下信息：")
        appendLine("- 用户告诉你的关于自己的信息（名字、年龄、职业、喜好等）")
        appendLine("- 重要的讨论话题和已得出的结论")
        appendLine("- 任何可能在后续对话中被用户引用的内容")
        appendLine()
        appendLine("不要添加解释或评价，不要添加「以下是摘要」之类的前缀，只输出简洁的摘要文本。")
    }

    // ======================================================================
    // 执行压缩
    // ======================================================================

    /**
     * 执行上下文压缩。
     * 使用与聊天相同的 ModelClient 请求模型生成摘要。
     *
     * @param modelClient 模型客户端
     * @param config 当前模型配置（决定请求哪个模型）
     * @param messages 当前 session 的所有消息
     * @param existingSummary 已有的摘要文本（可选）
     * @return 新生成的摘要文本，失败时返回 null
     */
    suspend fun compress(
        modelClient: RemoteModelClient,
        config: ModelConfig,
        messages: List<Message>,
        existingSummary: String?
    ): String? {
        return try {
            val selection = selectCompressionRange(messages, existingSummary)

            // 压缩也用当前模型，但使用一个低温度的默认 skill 配置
            val compressSkill = Skill(
                id = "_compressor",
                name = "Compressor",
                description = "",
                avatar = "",
                systemPrompt = "",
                greeting = "",
                isBuiltin = true
            )

            val compressMessages = listOf(
                ChatMessage("system", COMPRESSION_PROMPT),
                ChatMessage("user", selection.compressInput)
            )

            val summary = StringBuilder()

            modelClient.streamChat(compressMessages, config, compressSkill)
                .collect { event ->
                    when (event) {
                        is StreamEvent.Token -> summary.append(event.text)
                        is StreamEvent.Done -> { /* done */ }
                    }
                }

            val result = summary.toString().trim()
            if (result.length < 10) {
                // 摘要太短可能失败了
                return null
            }
            result.take(SUMMARY_MAX_CHARS)
        } catch (e: Exception) {
            FileLogger.e(TAG, "上下文压缩失败", e)
            null
        }
    }
}
