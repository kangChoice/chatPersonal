package com.needai.chat.domain.repository

import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.data.remote.tts.VoiceDesignClient

interface VoiceRepository {
    suspend fun getVoices(): List<VoiceInfo>
    suspend fun getVoiceById(voiceId: String): VoiceInfo?
    suspend fun saveVoice(voice: VoiceInfo)
    suspend fun deleteVoice(voiceId: String)
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
