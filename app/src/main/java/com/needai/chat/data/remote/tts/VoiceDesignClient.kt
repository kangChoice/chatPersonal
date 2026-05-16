package com.needai.chat.data.remote.tts

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.util.HttpLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class VoiceDesignClient(
    private val apiKey: String,
    private val gson: Gson = Gson()
) {
    /**
     * API 返回的字段是 snake_case，用独立的 Gson 实例做反序列化
     */
    private val responseGson = GsonBuilder()
        .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLogger)
        .build()

    /**
     * 所有接口共用同一个 endpoint，通过 input.action 区分操作
     */
    private val baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/customization"

    suspend fun createVoice(
        targetModel: String,
        prefix: String,
        voicePrompt: String,
        previewText: String
    ): Result<CreateVoiceResult> {
        return try {
            val requestBody = gson.toJson(mapOf(
                "model" to "voice-enrollment",
                "input" to mapOf(
                    "action" to "create_voice",
                    "target_model" to targetModel,
                    "voice_prompt" to voicePrompt,
                    "preview_text" to previewText,
                    "prefix" to prefix,
                    "language_hints" to listOf("zh")
                ),
                "parameters" to mapOf(
                    "sample_rate" to 24000,
                    "response_format" to "wav"
                )
            )).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val result = responseGson.fromJson(body, CreateVoiceResponse::class.java)
            Result.success(CreateVoiceResult(
                voiceId = result.output?.voiceId ?: "",
                status = "DEPLOYING",
                previewAudio = result.output?.previewAudio?.let {
                    PreviewAudioData(
                        data = it.data ?: "",
                        sampleRate = it.sampleRate ?: 24000,
                        responseFormat = it.responseFormat ?: "wav"
                    )
                }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listVoices(
        prefix: String? = null,
        pageIndex: Int = 0,
        pageSize: Int = 10
    ): Result<List<VoiceInfo>> {
        return try {
            val inputMap = mutableMapOf<String, Any>(
                "action" to "list_voice",
                "page_index" to pageIndex,
                "page_size" to pageSize
            )
            if (prefix != null) inputMap["prefix"] = prefix

            val requestBody = gson.toJson(mapOf(
                "model" to "voice-enrollment",
                "input" to inputMap
            )).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val result = responseGson.fromJson(body, ListVoiceResponse::class.java)
            if (result.output == null) {
                return Result.failure(Exception("反序列化失败: output 为 null, body=$body"))
            }
            val voiceList = result.output.voiceList ?: emptyList()
            val voices = voiceList.map { voice ->
                val id = voice.voiceId ?: ""
                val prompt = voice.voicePrompt ?: ""
                VoiceInfo(
                    voiceId = id,
                    displayName = prompt.ifBlank { id },
                    voicePrompt = prompt,
                    previewText = voice.previewText ?: "",
                    status = voice.status ?: "",
                    targetModel = voice.targetModel ?: "",
                    gmtCreate = voice.gmtCreate ?: "",
                    gmtModified = voice.gmtModified ?: ""
                )
            }
            Result.success(voices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun queryVoice(voiceId: String): Result<VoiceDetail> {
        return try {
            val requestBody = gson.toJson(mapOf(
                "model" to "voice-enrollment",
                "input" to mapOf(
                    "action" to "query_voice",
                    "voice_id" to voiceId
                )
            )).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val result = responseGson.fromJson(body, QueryVoiceResponse::class.java)
            Result.success(VoiceDetail(
                voiceId = result.output?.voiceId ?: voiceId,
                status = result.output?.status ?: "",
                voicePrompt = result.output?.voicePrompt ?: "",
                previewText = result.output?.previewText ?: ""
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVoice(voiceId: String): Result<Unit> {
        return try {
            val requestBody = gson.toJson(mapOf(
                "model" to "voice-enrollment",
                "input" to mapOf(
                    "action" to "delete_voice",
                    "voice_id" to voiceId
                )
            )).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== DTOs ==========

    data class CreateVoiceResult(
        val voiceId: String,
        val status: String,
        val previewAudio: PreviewAudioData? = null
    )

    data class PreviewAudioData(
        val data: String?,
        val sampleRate: Int?,
        val responseFormat: String?
    )

    data class VoiceDetail(
        val voiceId: String,
        val status: String,
        val voicePrompt: String,
        val previewText: String
    )

    // ---- Response DTOs ----

    private data class CreateVoiceResponse(
        val output: CreateVoiceOutput? = null
    )
    private data class CreateVoiceOutput(
        val voiceId: String? = null,
        val status: String? = null,
        val targetModel: String? = null,
        val previewAudio: PreviewAudioOutput? = null
    )
    private data class PreviewAudioOutput(
        val data: String? = null,
        val sampleRate: Int? = null,
        val responseFormat: String? = null
    )

    private data class ListVoiceResponse(
        val output: ListVoiceOutput? = null
    )
    private data class ListVoiceOutput(
        val voiceList: List<VoiceItem>? = null,
        val total: Int? = null
    )
    private data class VoiceItem(
        val voiceId: String? = null,
        val targetModel: String? = null,
        val voicePrompt: String? = null,
        val previewText: String? = null,
        val status: String? = null,
        val gmtCreate: String? = null,
        val gmtModified: String? = null
    )

    private data class QueryVoiceResponse(
        val output: QueryVoiceOutput? = null
    )
    private data class QueryVoiceOutput(
        val voiceId: String? = null,
        val status: String? = null,
        val voicePrompt: String? = null,
        val previewText: String? = null
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
