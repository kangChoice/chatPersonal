package com.needai.chat.data.repository

import com.needai.chat.data.remote.tts.VoiceDesignClient
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.VoiceRepository

class FakeVoiceRepository : VoiceRepository {

    private var voices = listOf<VoiceInfo>()
    private var cachedVoices: List<VoiceInfo>? = null

    fun setVoices(voices: List<VoiceInfo>) {
        this.voices = voices
        this.cachedVoices = voices
    }

    override suspend fun getVoices(): List<VoiceInfo> = cachedVoices ?: voices

    override fun clearCache() { cachedVoices = null }

    override suspend fun createVoice(
        targetModel: String,
        prefix: String,
        voicePrompt: String,
        previewText: String
    ): Result<VoiceDesignClient.CreateVoiceResult> {
        return Result.success(VoiceDesignClient.CreateVoiceResult("test-voice-id", ""))
    }

    override suspend fun listRemoteVoices(prefix: String?): Result<List<VoiceInfo>> {
        return Result.success(voices)
    }

    override suspend fun queryVoice(voiceId: String): Result<VoiceDesignClient.VoiceDetail> {
        return Result.success(VoiceDesignClient.VoiceDetail("", "", "", ""))
    }

    override suspend fun deleteRemoteVoice(voiceId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
