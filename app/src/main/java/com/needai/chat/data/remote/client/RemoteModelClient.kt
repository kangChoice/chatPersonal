package com.needai.chat.data.remote.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.needai.chat.data.remote.dto.AnthropicMessage
import com.needai.chat.data.remote.dto.AnthropicNonStreamResponse
import com.needai.chat.data.remote.dto.AnthropicRequest
import com.needai.chat.data.remote.dto.AnthropicStreamEvent
import com.needai.chat.data.remote.dto.ChatMessageDto
import com.needai.chat.data.remote.dto.ChatNonStreamResponse
import com.needai.chat.data.remote.dto.ChatRequest
import com.needai.chat.data.remote.dto.ChatStreamChunk
import com.needai.chat.domain.model.ApiProtocol
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.usecase.ChatMessage
import com.needai.chat.util.HttpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteModelClient @Inject constructor(
    private val gson: Gson
) : ModelClient {

    override fun streamChat(
        messages: List<ChatMessage>,
        config: ModelConfig,
        skill: Skill
    ): Flow<StreamEvent> = flow {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLogger)
            .build()

        try {
            val finalTemperature = if (skill.temperature != 0.7) skill.temperature else config.temperature

            val request = when (config.protocol) {
                ApiProtocol.OPENAI -> buildOpenAIRequest(messages, config, finalTemperature)
                ApiProtocol.ANTHROPIC -> buildAnthropicRequest(messages, config, finalTemperature)
            }

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                emit(StreamEvent.Token("[错误] 服务器返回 ${response.code}: $errorBody"))
                return@flow
            }

            val body = response.body ?: return@flow
            val reader = BufferedReader(InputStreamReader(body.byteStream(), "UTF-8"))
            var line: String?
            var currentSseEvent = ""
            var lastUsage: StreamEvent.Done? = null

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                when (config.protocol) {
                    ApiProtocol.OPENAI -> {
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ")
                            if (data == "[DONE]") break
                            try {
                                val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                                val content = chunk.choices?.firstOrNull()?.delta?.content
                                if (!content.isNullOrEmpty()) {
                                    emit(StreamEvent.Token(content))
                                }
                                // Capture usage from the last chunk
                                if (chunk.usage != null) {
                                    lastUsage = StreamEvent.Done(
                                        promptTokens = chunk.usage.promptTokens,
                                        completionTokens = chunk.usage.completionTokens,
                                        totalTokens = chunk.usage.totalTokens
                                    )
                                }
                            } catch (_: Exception) { }
                        }
                    }
                    ApiProtocol.ANTHROPIC -> {
                        when {
                            currentLine.startsWith("event: ") -> {
                                currentSseEvent = currentLine.removePrefix("event: ").trim()
                            }
                            currentLine.startsWith("data: ") -> {
                                val data = currentLine.removePrefix("data: ")
                                try {
                                    when (currentSseEvent) {
                                        "content_block_delta" -> {
                                            val event = gson.fromJson(data, AnthropicStreamEvent::class.java)
                                            val text = event.delta?.text
                                            if (!text.isNullOrEmpty()) {
                                                emit(StreamEvent.Token(text))
                                            }
                                        }
                                        "message_delta" -> {
                                            // Parse usage from message_delta event
                                            val jsonObj = gson.fromJson(data, JsonObject::class.java)
                                            val usage = jsonObj?.getAsJsonObject("usage")
                                            if (usage != null) {
                                                lastUsage = StreamEvent.Done(
                                                    completionTokens = usage.get("output_tokens")?.asInt
                                                )
                                            }
                                        }
                                        "message_start" -> {
                                            // Parse input tokens from message_start
                                            val jsonObj = gson.fromJson(data, JsonObject::class.java)
                                            val message = jsonObj?.getAsJsonObject("message")
                                            val usage = message?.getAsJsonObject("usage")
                                            if (usage != null) {
                                                val inputTokens = usage.get("input_tokens")?.asInt
                                                if (inputTokens != null) {
                                                    lastUsage = StreamEvent.Done(
                                                        promptTokens = inputTokens,
                                                        completionTokens = 0
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } catch (_: Exception) { }
                                currentSseEvent = ""
                            }
                        }
                    }
                }
            }

            // Emit usage data if captured
            if (lastUsage != null) {
                emit(lastUsage!!)
            } else {
                emit(StreamEvent.Done())
            }

        } catch (e: Exception) {
            emit(StreamEvent.Token("[错误] 网络请求失败: ${e.localizedMessage ?: "未知错误"}"))
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildOpenAIRequest(
        messages: List<ChatMessage>,
        config: ModelConfig,
        temperature: Double
    ): Request {
        val chatRequest = ChatRequest(
            model = config.remoteModelName,
            messages = messages.map { ChatMessageDto(role = it.role, content = it.content) },
            stream = true,
            temperature = temperature,
            maxTokens = config.maxTokens,
            topP = config.topP
        )
        val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/chat/completions"
        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.remoteApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(chatRequest).toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun buildAnthropicRequest(
        messages: List<ChatMessage>,
        config: ModelConfig,
        temperature: Double
    ): Request {
        val systemPrompt = messages.firstOrNull { it.role == "system" }?.content
        val anthropicMessages = messages
            .filter { it.role != "system" }
            .map { AnthropicMessage(role = it.role, content = it.content) }
            .ifEmpty { listOf(AnthropicMessage(role = "user", content = "...")) }

        val anthropicRequest = AnthropicRequest(
            model = config.remoteModelName,
            maxTokens = config.maxTokens,
            system = systemPrompt,
            messages = anthropicMessages,
            stream = true,
            temperature = temperature,
            topP = config.topP
        )
        val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/messages"
        return Request.Builder()
            .url(url)
            .addHeader("x-api-key", config.remoteApiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(anthropicRequest).toRequestBody("application/json".toMediaType()))
            .build()
    }

    override suspend fun chatNonStreaming(
        messages: List<ChatMessage>,
        config: ModelConfig,
        skill: Skill
    ): Result<String> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(HttpLogger)
                .build()

            val finalTemperature = if (skill.temperature != 0.7) skill.temperature else config.temperature

            val request = when (config.protocol) {
                ApiProtocol.OPENAI -> {
                    val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/chat/completions"
                    val body = ChatRequest(
                        model = config.remoteModelName,
                        messages = messages.map { ChatMessageDto(role = it.role, content = it.content) },
                        stream = false,
                        temperature = finalTemperature,
                        maxTokens = config.maxTokens,
                        topP = config.topP
                    )
                    Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer ${config.remoteApiKey}")
                        .addHeader("Content-Type", "application/json")
                        .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                        .build()
                }
                ApiProtocol.ANTHROPIC -> {
                    val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/messages"
                    val systemPrompt = messages.firstOrNull { it.role == "system" }?.content
                    val anthropicMessages = messages
                        .filter { it.role != "system" }
                        .map { AnthropicMessage(role = it.role, content = it.content) }
                    val body = AnthropicRequest(
                        model = config.remoteModelName,
                        maxTokens = config.maxTokens,
                        system = systemPrompt,
                        messages = anthropicMessages,
                        stream = false,
                        temperature = finalTemperature,
                        topP = config.topP
                    )
                    Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", config.remoteApiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                        .build()
                }
            }

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: ${body.take(200)}"))
            }

            val text = when (config.protocol) {
                ApiProtocol.OPENAI -> {
                    val parsed = gson.fromJson(body, ChatNonStreamResponse::class.java)
                    parsed.choices?.firstOrNull()?.message?.content.orEmpty()
                }
                ApiProtocol.ANTHROPIC -> {
                    val parsed = gson.fromJson(body, AnthropicNonStreamResponse::class.java)
                    parsed.content?.firstOrNull()?.text.orEmpty()
                }
            }

            client.dispatcher.executorService.shutdown()
            if (text.isBlank()) Result.failure(Exception("空响应"))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateConfig(config: ModelConfig): Result<Boolean> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(HttpLogger)
                .build()

            val request = when (config.protocol) {
                ApiProtocol.OPENAI -> {
                    val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/chat/completions"
                    val body = ChatRequest(
                        model = config.remoteModelName,
                        messages = listOf(ChatMessageDto(role = "user", content = "hi")),
                        stream = false,
                        maxTokens = 1
                    )
                    Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer ${config.remoteApiKey}")
                        .addHeader("Content-Type", "application/json")
                        .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                        .build()
                }
                ApiProtocol.ANTHROPIC -> {
                    val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/messages"
                    val body = AnthropicRequest(
                        model = config.remoteModelName,
                        maxTokens = 1,
                        messages = listOf(AnthropicMessage(role = "user", content = "hi")),
                        stream = false
                    )
                    Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", config.remoteApiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
                        .build()
                }
            }

            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(true)
            else Result.failure(Exception("配置验证失败: HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
