package com.needai.chat.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isSyncing: Boolean = false,
    val isCreating: Boolean = false,
    val creatingStatus: String? = null
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

    /**
     * 从 Voice Design API 同步远程音色列表到本地
     */
    fun syncFromRemote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            val result = voiceRepository.listRemoteVoices()
            result.onSuccess { remoteVoices ->
                // 合并到本地：远程音色覆盖同 voiceId 的本地音色，新增不存在的
                val local = voiceRepository.getVoices()
                val merged = remoteVoices.map { remote ->
                    val existing = local.find { it.voiceId == remote.voiceId }
                    existing?.copy(
                        status = remote.status,
                        voicePrompt = remote.voicePrompt,
                        previewText = remote.previewText
                    ) ?: remote
                }
                // 保留不在远程中的本地自定义音色
                val remoteIds = remoteVoices.map { it.voiceId }.toSet()
                val localCustom = local.filter { it.voiceId !in remoteIds }
                (merged + localCustom).forEach { voiceRepository.saveVoice(it) }
                _uiState.update {
                    it.copy(
                        voices = merged + localCustom,
                        isSyncing = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isSyncing = false, error = "同步失败: ${e.localizedMessage}") }
            }
        }
    }

    fun addVoice(voice: VoiceInfo) {
        viewModelScope.launch {
            voiceRepository.saveVoice(voice)
            loadVoices()
        }
    }

    fun updateVoice(oldId: String, voice: VoiceInfo) {
        viewModelScope.launch {
            voiceRepository.deleteVoice(oldId)
            voiceRepository.saveVoice(voice)
            loadVoices()
        }
    }

    fun deleteVoice(voiceId: String) {
        viewModelScope.launch {
            voiceRepository.deleteVoice(voiceId)
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
                _uiState.update { it.copy(creatingStatus = "音色创建中 (${createResult.status})，正在等待部署...") }
                val voiceInfo = VoiceInfo(
                    voiceId = createResult.voiceId,
                    displayName = voicePrompt,
                    voicePrompt = voicePrompt,
                    targetModel = targetModel,
                    status = createResult.status,
                    previewText = previewText
                )
                voiceRepository.saveVoice(voiceInfo)
                pollVoiceStatus(createResult.voiceId)
            }.onFailure { e ->
                _uiState.update { it.copy(isCreating = false, error = "创建失败: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun pollVoiceStatus(voiceId: String) {
        var retries = 0
        val maxRetries = 30
        while (retries < maxRetries) {
            kotlinx.coroutines.delay(2000)
            val result = voiceRepository.queryVoice(voiceId)
            result.onSuccess { detail ->
                if (detail.status == "OK") {
                    val current = _uiState.value.voices.find { it.voiceId == voiceId }
                    if (current != null) {
                        voiceRepository.saveVoice(current.copy(status = "OK"))
                    }
                    _uiState.update { it.copy(isCreating = false, creatingStatus = "音色已就绪！") }
                    loadVoices()
                    return
                }
                _uiState.update { it.copy(creatingStatus = "部署中 (${detail.status})...") }
            }
            retries++
        }
        _uiState.update { it.copy(isCreating = false, creatingStatus = null, error = "部署超时，请稍后刷新") }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissCreating() {
        _uiState.update { it.copy(isCreating = false, creatingStatus = null) }
    }
}
