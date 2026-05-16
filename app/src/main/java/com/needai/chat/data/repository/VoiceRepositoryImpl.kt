package com.needai.chat.data.repository

import android.util.Log
import com.needai.chat.data.remote.tts.VoiceDesignClient
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.VoiceRepository
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val gson: Gson
) : VoiceRepository {

    private var voiceCache: List<VoiceInfo>? = null
    private var cacheTime: Long = 0

    override suspend fun getVoices(): List<VoiceInfo> {
        if (voiceCache != null && System.currentTimeMillis() - cacheTime < 300_000) {
            return voiceCache!!
        }
        val prefix = settingsDataStore.ttsPrefix.first()
        Log.d("VoiceRepository", "Fetching voices with prefix='$prefix'")
        val result = listRemoteVoices(prefix.ifBlank { null })
        result.onSuccess { voices ->
            Log.d("VoiceRepository", "Fetched ${voices.size} voices")
            voiceCache = voices
            cacheTime = System.currentTimeMillis()
            return voices
        }.onFailure { e ->
            Log.e("VoiceRepository", "Failed to fetch voices: ${e.message}")
        }
        return voiceCache ?: emptyList()
    }

    override fun clearCache() {
        voiceCache = null
        cacheTime = 0
    }

    override suspend fun createVoice(
        targetModel: String,
        prefix: String,
        voicePrompt: String,
        previewText: String
    ): Result<VoiceDesignClient.CreateVoiceResult> {
        val apiKey = settingsDataStore.ttsApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("未配置 API Key"))
        val client = VoiceDesignClient(apiKey, gson)
        return client.createVoice(targetModel, prefix, voicePrompt, previewText)
    }

    override suspend fun listRemoteVoices(prefix: String?): Result<List<VoiceInfo>> {
        val apiKey = settingsDataStore.ttsApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("未配置 API Key"))
        val client = VoiceDesignClient(apiKey, gson)
        return client.listVoices(prefix)
    }

    override suspend fun queryVoice(voiceId: String): Result<VoiceDesignClient.VoiceDetail> {
        val apiKey = settingsDataStore.ttsApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("未配置 API Key"))
        val client = VoiceDesignClient(apiKey, gson)
        return client.queryVoice(voiceId)
    }

    override suspend fun deleteRemoteVoice(voiceId: String): Result<Unit> {
        val apiKey = settingsDataStore.ttsApiKey.first()
        if (apiKey.isBlank()) return Result.failure(Exception("未配置 API Key"))
        val client = VoiceDesignClient(apiKey, gson)
        return client.deleteVoice(voiceId)
    }
}
