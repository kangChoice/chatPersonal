package com.needai.chat.domain.repository

import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.data.remote.tts.VoiceDesignClient

interface VoiceRepository {
    suspend fun getVoices(): List<VoiceInfo>
    fun clearCache()
    suspend fun createVoice(
        targetModel: String,
        prefix: String,
        voicePrompt: String,
        previewText: String
    ): Result<VoiceDesignClient.CreateVoiceResult>
    suspend fun listRemoteVoices(prefix: String? = null): Result<List<VoiceInfo>>
    suspend fun queryVoice(voiceId: String): Result<VoiceDesignClient.VoiceDetail>
    suspend fun deleteRemoteVoice(voiceId: String): Result<Unit>
}
