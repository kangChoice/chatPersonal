package com.needai.chat.data.remote.client

import com.google.gson.Gson
import com.needai.chat.data.remote.dto.ChatMessageDto
import com.needai.chat.data.remote.dto.ChatRequest
import com.needai.chat.data.remote.dto.ChatStreamChunk
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.usecase.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
    ): Flow<String> = flow {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val chatRequest = ChatRequest(
            model = config.remoteModelName,
            messages = messages.map { ChatMessageDto(role = it.role, content = it.content) },
            stream = true,
            temperature = if (skill.temperature != 0.7) skill.temperature else config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP
        )

        val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/chat/completions"
        val jsonBody = gson.toJson(chatRequest)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.remoteApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                emit("[错误] 服务器返回 ${response.code}: $errorBody")
                return@flow
            }

            val body = response.body ?: return@flow
            val reader = BufferedReader(InputStreamReader(body.byteStream(), "UTF-8"))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val data = currentLine.removePrefix("data: ")
                    if (data == "[DONE]") break

                    try {
                        val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunks
                    }
                }
            }
        } catch (e: Exception) {
            emit("[错误] 网络请求失败: ${e.localizedMessage ?: "未知错误"}")
        } finally {
            client.dispatcher.executorService.shutdown()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun validateConfig(config: ModelConfig): Result<Boolean> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val url = "${config.remoteBaseUrl.trimEnd('/')}/v1/chat/completions"
            val minimalRequest = ChatRequest(
                model = config.remoteModelName,
                messages = listOf(ChatMessageDto(role = "user", content = "hi")),
                stream = false,
                maxTokens = 1
            )
            val jsonBody = gson.toJson(minimalRequest)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.remoteApiKey}")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("配置验证失败: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
