package com.needai.chat.data.repository

import com.needai.chat.data.remote.tts.VoiceDesignClient
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.VoiceRepository
import com.needai.chat.util.EncryptUtil
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        val json = settingsDataStore.ttsVoiceList.first()
        val voices: List<VoiceInfo> = if (json.isNotBlank()) {
            try {
                gson.fromJson(json, object : TypeToken<List<VoiceInfo>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
        voiceCache = voices
        cacheTime = System.currentTimeMillis()
        return voices
    }

    override suspend fun getVoiceById(voiceId: String): VoiceInfo? {
        return getVoices().find { it.voiceId == voiceId }
    }

    override suspend fun saveVoice(voice: VoiceInfo) {
        val voices = getVoices().toMutableList()
        val idx = voices.indexOfFirst { it.voiceId == voice.voiceId }
        if (idx >= 0) voices[idx] = voice
        else voices.add(voice)
        saveVoiceList(voices)
    }

    override suspend fun deleteVoice(voiceId: String) {
        val voices = getVoices().toMutableList()
        voices.removeAll { it.voiceId == voiceId }
        voiceCache = voices
        settingsDataStore.setTtsVoiceList(gson.toJson(voices))
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
        val model = targetModel.ifBlank { settingsDataStore.ttsModel.first() }
        return client.createVoice(model, prefix, voicePrompt, previewText)
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

    private suspend fun saveVoiceList(voices: List<VoiceInfo>) {
        voiceCache = voices
        cacheTime = System.currentTimeMillis()
        settingsDataStore.setTtsVoiceList(gson.toJson(voices))
    }
}
