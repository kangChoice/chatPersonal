package com.needai.chat.ui.voicechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.needai.chat.data.local.datastore.SettingsDataStore
import com.needai.chat.data.remote.asr.VoiceChatManager
import com.needai.chat.data.remote.tts.SystemVoiceProvider
import com.needai.chat.data.remote.client.ModelClient
import com.needai.chat.domain.model.ModelConfig
import com.needai.chat.domain.model.Skill
import com.needai.chat.domain.model.StreamEvent
import com.needai.chat.domain.model.VoiceInfo
import com.needai.chat.domain.repository.ModelConfigRepository
import com.needai.chat.domain.repository.SkillRepository
import com.needai.chat.domain.repository.VoiceRepository
import com.needai.chat.domain.usecase.ChatMessage
import com.needai.chat.util.FileLogger
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceChatUiState(
    val status: String = "点击开始通话",
    val isCallActive: Boolean = false,
    val partialText: String = "",
    val lastUserText: String = "",
    val assistantText: String = "",
    val conversationHistory: List<ChatEntry> = emptyList(),
    val error: String? = null,
    val skillName: String = "语音助手",
    val skillAvatar: String = "🎙️",
    val allSkills: List<Skill> = emptyList(),
    val selectedSkillId: String? = null,
    val currentVoiceDisplayName: String = "",
    val currentModelDisplayName: String = ""
)

data class ChatEntry(
    val role: String,
    val text: String
)

@HiltViewModel
class VoiceChatViewModel @Inject constructor(
    private val modelClient: ModelClient,
    private val modelConfigRepository: ModelConfigRepository,
    private val skillRepository: SkillRepository,
    private val voiceRepository: VoiceRepository,
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceChatUiState())
    val uiState: StateFlow<VoiceChatUiState> = _uiState.asStateFlow()

    private var voiceChatManager: VoiceChatManager? = null
    private var currentConfig: ModelConfig? = null
    private var ttsManager: ITtsManager? = null
    private var allVoices: List<VoiceInfo> = emptyList()
    private var ttsApiKey: String = ""

    // TTS 队列追踪
    private var pendingTtsCount = 0
    private var ttsStreamDone = false

    init {
        viewModelScope.launch {
            currentConfig = modelConfigRepository.getModelConfig().first()
        }
        viewModelScope.launch {
            skillRepository.getAllSkills().first().let { skills ->
                val firstSkill = skills.firstOrNull()
                _uiState.update {
                    it.copy(
                        allSkills = skills,
                        skillName = firstSkill?.name ?: "语音助手",
                        skillAvatar = firstSkill?.avatar ?: "🎙️",
                        selectedSkillId = firstSkill?.id
                    )
                }
                if (firstSkill != null) {
                    updateVoiceModelDisplay(firstSkill)
                }
            }
        }
        viewModelScope.launch {
            allVoices = voiceRepository.getVoices()
        }

        // 监听 TTS API Key
        viewModelScope.launch {
            settingsDataStore.ttsApiKey.collect { key ->
                ttsApiKey = key
                if (key.isNotBlank()) {
                    voiceChatManager = VoiceChatManager(key)
                    setupVoiceChatCallbacks()
                    reinitTtsManager()
                }
            }
        }
    }

    private fun reinitTtsManager() {
        if (ttsApiKey.isBlank()) return
        ttsManager?.shutdown()
        val customVoiceModelMap = allVoices.filter { it.targetModel.isNotBlank() }
            .associate { it.voiceId to it.targetModel }
        val voiceModelResolver: (String) -> String? = { voiceId ->
            SystemVoiceProvider.getModelForVoice(voiceId) ?: customVoiceModelMap[voiceId]
        }
        ttsManager = TtsManagerImpl(
            apiKey = ttsApiKey,
            voiceModelResolver = voiceModelResolver
        )
    }

    fun selectSkill(skillId: String) {
        val skill = _uiState.value.allSkills.find { it.id == skillId } ?: return
        _uiState.update {
            it.copy(
                selectedSkillId = skillId,
                skillName = skill.name,
                skillAvatar = skill.avatar
            )
        }
        updateVoiceModelDisplay(skill)
    }

    private fun updateVoiceModelDisplay(skill: Skill) {
        val voiceId = skill.voiceId
        if (voiceId.isBlank()) {
            _uiState.update {
                it.copy(
                    currentVoiceDisplayName = "默认音色",
                    currentModelDisplayName = "cosyvoice-v3-flash"
                )
            }
            return
        }

        // 尝试从系统音色中查找
        val systemVoice = SystemVoiceProvider.findSystemVoice(voiceId)
        if (systemVoice != null) {
            _uiState.update {
                it.copy(
                    currentVoiceDisplayName = systemVoice.displayName,
                    currentModelDisplayName = "cosyvoice-v3-flash"
                )
            }
            return
        }

        // 从自定义音色中查找
        val customVoice = allVoices.find { it.voiceId == voiceId }
        if (customVoice != null) {
            _uiState.update {
                it.copy(
                    currentVoiceDisplayName = customVoice.displayName.ifEmpty { voiceId },
                    currentModelDisplayName = customVoice.targetModel.ifEmpty { "cosyvoice-v3-flash" }
                )
            }
            return
        }

        // 未找到，显示 voiceId
        val resolvedModel = SystemVoiceProvider.getModelForVoice(voiceId) ?: "cosyvoice-v3-flash"
        _uiState.update {
            it.copy(
                currentVoiceDisplayName = voiceId,
                currentModelDisplayName = resolvedModel
            )
        }
    }

    private fun setupVoiceChatCallbacks() {
        val manager = voiceChatManager ?: return

        viewModelScope.launch {
            manager.state.collect { state ->
                when (state) {
                    is VoiceChatManager.State.Connecting -> {
                        _uiState.update { it.copy(status = "连接中...") }
                    }
                    is VoiceChatManager.State.Listening -> {
                        _uiState.update { it.copy(status = "聆听中...") }
                    }
                    is VoiceChatManager.State.Processing -> {
                    }
                    is VoiceChatManager.State.Speaking -> {
                        _uiState.update { it.copy(status = "播放中...") }
                    }
                    is VoiceChatManager.State.Error -> {
                        _uiState.update { it.copy(error = state.msg, status = "连接失败") }
                    }
                    is VoiceChatManager.State.Stopped -> {
                        _uiState.update { it.copy(status = "已结束") }
                    }
                    is VoiceChatManager.State.Idle -> { }
                }
            }
        }

        manager.setOnText { text ->
            _uiState.update {
                it.copy(
                    lastUserText = text,
                    partialText = "",
                    conversationHistory = it.conversationHistory + ChatEntry("user", text)
                )
            }
            sendToLLM(text)
        }

        manager.setOnPartial { text ->
            _uiState.update { it.copy(partialText = text) }
        }

        manager.setOnError { msg ->
            _uiState.update { it.copy(error = msg, status = "连接失败") }
        }
    }

    fun toggleCall() {
        if (_uiState.value.isCallActive) {
            stopCall()
        } else {
            startCall()
        }
    }

    fun updateError(msg: String) {
        _uiState.update { it.copy(error = msg) }
    }

    private fun getSelectedSkill(): Skill? {
        val skillId = _uiState.value.selectedSkillId
        return if (skillId != null) {
            _uiState.value.allSkills.find { it.id == skillId }
        } else {
            _uiState.value.allSkills.firstOrNull()
        }
    }

    private fun startCall() {
        if (voiceChatManager == null) {
            _uiState.update { it.copy(error = "TTS API Key 未配置") }
            return
        }

        reinitTtsManager()
        val selectedSkill = getSelectedSkill()
        _uiState.update {
            it.copy(
                isCallActive = true,
                error = null,
                partialText = "",
                lastUserText = "",
                assistantText = "",
                status = "连接中...",
                skillName = selectedSkill?.name ?: "语音助手",
                skillAvatar = selectedSkill?.avatar ?: "🎙️"
            )
        }

        if (selectedSkill != null) {
            updateVoiceModelDisplay(selectedSkill)
        }

        voiceChatManager?.start()
    }

    private fun stopCall() {
        voiceChatManager?.stop()
        streamingJob?.cancel()
        streamingJob = null
        ttsManager?.stop()
        ttsSentenceBuffer.clear()
        pendingTtsCount = 0
        ttsStreamDone = false
        _uiState.update {
            it.copy(
                isCallActive = false,
                status = "已结束",
                partialText = "",
                lastUserText = "",
                assistantText = "",
                error = null
            )
        }
    }

    private var streamingJob: kotlinx.coroutines.Job? = null

    private fun sendToLLM(userText: String) {
        val config = currentConfig ?: run {
            _uiState.update { it.copy(error = "未选择模型配置") }
            voiceChatManager?.resumeListening()
            return
        }
        val skill = getSelectedSkill() ?: Skill(
            id = "voicechat",
            name = "语音助手",
            description = "",
            avatar = "🎙️",
            systemPrompt = "你是 voiceChat，一个语音助手。请用简短的口语化方式回答问题，不要使用 markdown 格式，每句话不要太长。",
            greeting = "你好！",
            isBuiltin = false
        )

        _uiState.update { it.copy(status = "思考中...", assistantText = "") }

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            try {
                val messages = listOf(
                    ChatMessage(role = "system", content = skill.systemPrompt),
                    ChatMessage(role = "user", content = userText)
                )

                val fullText = StringBuilder()
                modelClient.streamChat(messages, config, skill).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            val text = event.text
                            if (text.startsWith("[错误]")) {
                                val errorMsg = text.removePrefix("[错误]")
                                FileLogger.e("VoiceChat", "LLM 错误: $errorMsg")
                                _uiState.update { it.copy(error = errorMsg) }
                                voiceChatManager?.resumeListening()
                                return@collect
                            }
                            fullText.append(text)
                            _uiState.update {
                                it.copy(
                                    assistantText = fullText.toString(),
                                    status = "回复中..."
                                )
                            }
                            appendTtsText(text)
                        }
                        is StreamEvent.Done -> {
                            FileLogger.i("VoiceChat", "LLM 完成: ${fullText.length} 字符")
                            _uiState.update {
                                it.copy(
                                    status = "播放中...",
                                    conversationHistory = it.conversationHistory + ChatEntry("assistant", fullText.toString())
                                )
                            }
                            flushTtsText()
                        }
                    }
                }
            } catch (e: Exception) {
                FileLogger.e("VoiceChat", "LLM 异常", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "LLM 请求失败") }
                voiceChatManager?.resumeListening()
            }
        }
    }

    // ===== TTS 处理 =====

    private val ttsSentenceBuffer = StringBuilder()

    private fun appendTtsText(token: String) {
        ttsSentenceBuffer.append(token)
        val text = ttsSentenceBuffer.toString()
        val sentenceEnd = text.indexOfAny(charArrayOf('。', '！', '？', '\n', '.', '!', '?'))
        if (sentenceEnd > 0) {
            val sentence = text.substring(0, sentenceEnd + 1)
            ttsSentenceBuffer.delete(0, sentenceEnd + 1)
            playTtsSentence(sentence)
        } else if (text.length > 50) {
            ttsSentenceBuffer.clear()
            playTtsSentence(text)
        }
    }

    private fun playTtsSentence(sentence: String) {
        val skill = getSelectedSkill()
        val voiceId = skill?.voiceId ?: ""
        pendingTtsCount++
        val mgr = ttsManager
        if (mgr is TtsManagerImpl) {
            mgr.speakQueued(sentence, voiceId = voiceId, onDone = {
                pendingTtsCount--
                checkTtsDone()
            })
        } else {
            pendingTtsCount--
            checkTtsDone()
        }
    }

    private fun flushTtsText() {
        if (ttsSentenceBuffer.isNotEmpty()) {
            playTtsSentence(ttsSentenceBuffer.toString())
            ttsSentenceBuffer.clear()
        }
        ttsStreamDone = true
        checkTtsDone()
    }

    private fun checkTtsDone() {
        if (ttsStreamDone && pendingTtsCount <= 0) {
            ttsStreamDone = false
            voiceChatManager?.resumeListening()
            _uiState.update { it.copy(status = "聆听中...") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceChatManager?.stop()
        streamingJob?.cancel()
        ttsManager?.shutdown()
    }
}
