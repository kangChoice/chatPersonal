package com.needai.chat.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.data.remote.tts.VoiceDesignClient
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceListUiState(
    val voices: List<VoiceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
    val creatingStatus: String? = null,
    val previewAudioData: VoiceDesignClient.PreviewAudioData? = null
)

@HiltViewModel
class VoiceListViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceListUiState())
    val uiState: StateFlow<VoiceListUiState> = _uiState.asStateFlow()

    init {
        loadVoices()
    }

    fun loadVoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val voices = voiceRepository.getVoices()
                _uiState.update { it.copy(voices = voices, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun refreshVoices() {
        voiceRepository.clearCache()
        loadVoices()
    }

    fun deleteVoice(voiceId: String) {
        viewModelScope.launch {
            val result = voiceRepository.deleteRemoteVoice(voiceId)
            result.onSuccess {
                voiceRepository.clearCache()
                loadVoices()
            }.onFailure { e ->
                _uiState.update { it.copy(error = "删除失败: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteAllVoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val currentVoices = _uiState.value.voices.toList()
            for (voice in currentVoices) {
                val result = voiceRepository.deleteRemoteVoice(voice.voiceId)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(error = "删除「${voice.displayName}」失败: ${result.exceptionOrNull()?.localizedMessage}")
                    }
                    return@launch
                }
            }
            voiceRepository.clearCache()
            loadVoices()
        }
    }

    fun createCustomVoice(
        targetModel: String,
        prefix: String,
        voicePrompt: String,
        previewText: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, creatingStatus = "正在创建音色...") }
            val result = voiceRepository.createVoice(targetModel, prefix, voicePrompt, previewText)
            result.onSuccess { createResult ->
                _uiState.update {
                    it.copy(
                        creatingStatus = "音色创建中 (${createResult.status})，正在等待部署...",
                        previewAudioData = createResult.previewAudio
                    )
                }
                pollVoiceStatus(createResult.voiceId)
            }.onFailure { e ->
                _uiState.update { it.copy(isCreating = false, error = "创建失败: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun pollVoiceStatus(voiceId: String) {
        var retries = 0
        val maxRetries = 30
        var okRetries = 0
        val requiredOkRetries = 6 // 连续 6 次 (~12s) 查询返回 OK 才确认生效

        while (retries < maxRetries) {
            kotlinx.coroutines.delay(2000)
            val result = voiceRepository.queryVoice(voiceId)
            result.onSuccess { detail ->
                if (detail.status == "OK") {
                    okRetries++
                    if (okRetries == 1) {
                        _uiState.update { it.copy(creatingStatus = "部署完成，配置生效中...") }
                    } else if (okRetries >= requiredOkRetries) {
                        _uiState.update { it.copy(isCreating = false, creatingStatus = "音色已就绪！") }
                        voiceRepository.clearCache()
                        loadVoices()
                        return
                    }
                } else {
                    okRetries = 0
                    _uiState.update { it.copy(creatingStatus = "部署中 (${detail.status})...") }
                }
            }
            retries++
        }
        _uiState.update { it.copy(isCreating = false, creatingStatus = null, error = "部署超时，请稍后刷新") }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissPreviewAudio() {
        _uiState.update { it.copy(previewAudioData = null) }
    }

    fun dismissCreating() {
        _uiState.update { it.copy(isCreating = false, creatingStatus = null) }
    }
}
