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
import kotlinx.coroutines.CancellationException
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
    val errorStep: String? = null,
    val skillName: String = "语音助手",
    val skillAvatar: String = "🎙️",
    val allSkills: List<Skill> = emptyList(),
    val selectedSkillId: String? = null,
    val currentVoiceDisplayName: String = "",
    val currentModelDisplayName: String = "",
    /** 用户正在说话（ASR 有中间结果） */
    val isSpeaking: Boolean = false,
    /** 正在播放 TTS */
    val isTtsPlaying: Boolean = false
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

    /** ASR 实时音量振幅 0-255，用于 UI 波形显示 */
    private val _voiceAmplitude = MutableStateFlow(0)
    val voiceAmplitude: StateFlow<Int> = _voiceAmplitude.asStateFlow()

    private var voiceChatManager: VoiceChatManager? = null
    private var currentConfig: ModelConfig? = null
    private var ttsManager: ITtsManager? = null
    private var allVoices: List<VoiceInfo> = emptyList()
    private var ttsApiKey: String = ""

    // TTS 队列追踪
    private var pendingTtsCount = 0
    private var ttsStreamDone = false

    // 回声抑制：冷却期 + 内容匹配
    private var lastTtsEndMs = 0L
    private val recentTtsTexts = mutableListOf<String>()
    private companion object {
        private const val ECHO_COOLDOWN_MS = 800L
        private const val MAX_TTS_CACHE = 3
        private const val TAG = "VoiceChat"
        private val PUNCT_REGEX = Regex("[\\s，。！？、；：,.!?;：、]")
    }

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

        // 收集 ASR 状态
        viewModelScope.launch {
            manager.state.collect { state ->
                when (state) {
                    is VoiceChatManager.State.Connecting -> {
                        _uiState.update { it.copy(status = "连接中...") }
                    }
                    is VoiceChatManager.State.Listening -> {
                        _uiState.update {
                            it.copy(
                                status = if (it.isTtsPlaying) "播放中..." else "聆听中...",
                                isSpeaking = it.isSpeaking
                            )
                        }
                    }
                    is VoiceChatManager.State.Error -> {
                        _uiState.update { it.copy(error = state.msg, errorStep = "ASR", status = "ASR失败") }
                    }
                    is VoiceChatManager.State.Stopped -> {
                        _uiState.update { it.copy(status = "已结束") }
                    }
                    is VoiceChatManager.State.Idle -> { }
                }
            }
        }

        // 收集音量振幅（用于波形显示）
        viewModelScope.launch {
            manager.amplitude.collect { amp ->
                _voiceAmplitude.value = amp
            }
        }

        // ASR 中间结果 → 用户正在说话
        manager.setOnPartial { text ->
            _uiState.update {
                it.copy(partialText = text, isSpeaking = text.isNotBlank())
            }
        }

        // ASR 完整句子 → 回声过滤/打断/LLM
        manager.setOnText { text ->
            // TTS 刚结束的冷却期内，先检查是否回声
            val ttsRecentlyActive = pendingTtsCount > 0 ||
                System.currentTimeMillis() - lastTtsEndMs < ECHO_COOLDOWN_MS

            if (ttsRecentlyActive && isLikelyEcho(text)) {
                FileLogger.d(TAG, "回声内容匹配，忽略 ASR: ${text.take(30)}")
                return@setOnText
            }
            handleUserText(text)
        }

        manager.setOnError { msg ->
            setError("ASR", msg)
        }
    }

    /**
     * 处理用户输入的文本（来自 ASR）。
     * - 回声冷却期内忽略（TTS 刚结束的残留音频）
     * - 如果在播放或 LLM 流式输出中，执行打断
     */
    private fun handleUserText(text: String) {
        // 回声冷却期检查
        if (System.currentTimeMillis() - lastTtsEndMs < ECHO_COOLDOWN_MS) {
            FileLogger.d(TAG, "忽略 ASR 结果（回声冷却期内）: $text")
            return
        }

        val isBusy = pendingTtsCount > 0 || streamingJob?.isActive == true

        if (isBusy) {
            FileLogger.i(TAG, "打断当前输出，处理新输入: ${text.take(30)}")
            // 停 TTS（先出声音，用户立刻感知到打断）
            ttsManager?.stop()
            // 取消 LLM 流
            streamingJob?.cancel()
            streamingJob = null
            // 清空 TTS 缓存
            ttsSentenceBuffer.clear()
            pendingTtsCount = 0
            ttsStreamDone = false
            _uiState.update { it.copy(assistantText = "", isTtsPlaying = false) }
        }

        _uiState.update {
            it.copy(
                lastUserText = text,
                partialText = "",
                isSpeaking = false,
                conversationHistory = it.conversationHistory + ChatEntry("user", text)
            )
        }

        sendToLLM(text)
    }

    fun toggleCall() {
        if (_uiState.value.isCallActive) {
            stopCall()
        } else {
            startCall()
        }
    }

    fun updateError(msg: String) {
        _uiState.update { it.copy(error = msg, errorStep = "权限") }
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
            setError("TTS", "API Key 未配置")
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
                skillAvatar = selectedSkill?.avatar ?: "🎙️",
                isSpeaking = false,
                isTtsPlaying = false
            )
        }

        if (selectedSkill != null) {
            updateVoiceModelDisplay(selectedSkill)
        }

        voiceChatManager?.start()
    }

    /**
     * 挂断操作：先停 TTS（立即静音）→ 取消 LLM → 最后停 ASR
     */
    private fun stopCall() {
        // 1. 立即停止 TTS 播放
        ttsManager?.stop()
        ttsSentenceBuffer.clear()
        pendingTtsCount = 0
        ttsStreamDone = false

        // 2. 取消 LLM 流
        streamingJob?.cancel()
        streamingJob = null

        // 3. 停 ASR
        voiceChatManager?.stop()

        _uiState.update {
            it.copy(
                isCallActive = false,
                status = "已结束",
                partialText = "",
                lastUserText = "",
                assistantText = "",
                error = null,
                isSpeaking = false,
                isTtsPlaying = false
            )
        }
    }

    private var streamingJob: kotlinx.coroutines.Job? = null

    private fun setError(step: String, message: String) {
        FileLogger.e(TAG, "[$step] $message")
        _uiState.update { it.copy(error = message, errorStep = step, status = "${step}失败") }
    }

    private fun sendToLLM(userText: String) {
        val config = currentConfig ?: run {
            setError("LLM", "未选择模型配置")
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
                                setError("LLM", errorMsg)
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
                            FileLogger.i(TAG, "LLM 完成: ${fullText.length} 字符")
                            _uiState.update {
                                it.copy(
                                    status = "播放中...",
                                    isTtsPlaying = true,
                                    conversationHistory = it.conversationHistory + ChatEntry("assistant", fullText.toString())
                                )
                            }
                            flushTtsText()
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 被打断（用户说了新的话），不报错
                FileLogger.d(TAG, "LLM 流被取消（用户打断）")
            } catch (e: Exception) {
                FileLogger.e(TAG, "LLM 异常", e)
                setError("LLM", e.localizedMessage ?: "LLM 请求失败")
            }
        }
    }

    /**
     * 检查 ASR 结果是否可能是 TTS 回声。
     * 归一化后与最近 TTS 输出做子串匹配，高度重叠则判定为回声。
     */
    private fun isLikelyEcho(asrText: String): Boolean {
        if (asrText.length < 3) return false
        val asrNorm = asrText.replace(PUNCT_REGEX, "")
        if (asrNorm.length < 3) return false

        synchronized(recentTtsTexts) {
            return recentTtsTexts.any { tts ->
                val ttsNorm = tts.replace(PUNCT_REGEX, "")
                ttsNorm.length >= 3 && (ttsNorm.contains(asrNorm) || asrNorm.contains(ttsNorm))
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
        _uiState.update { it.copy(isTtsPlaying = true) }

        // 记录最近 TTS 输出，用于回声内容匹配
        synchronized(recentTtsTexts) {
            recentTtsTexts.add(sentence)
            if (recentTtsTexts.size > MAX_TTS_CACHE) {
                recentTtsTexts.removeAt(0)
            }
        }
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
            lastTtsEndMs = System.currentTimeMillis()
            _uiState.update { it.copy(status = "聆听中...", isTtsPlaying = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceChatManager?.stop()
        streamingJob?.cancel()
        ttsManager?.shutdown()
    }
}
