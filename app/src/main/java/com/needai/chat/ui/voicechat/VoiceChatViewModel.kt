package com.needai.chat.ui.voicechat

import android.content.Context
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
import com.needai.chat.util.ContextCompressor
import com.needai.chat.util.FileLogger
import com.needai.chat.util.ITtsManager
import com.needai.chat.util.TtsManagerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

private data class TtsSettings(
    val key: String,
    val volume: Int,
    val rate: Float,
    val pitch: Float
)

@HiltViewModel
class VoiceChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    private var ttsVolume: Int = 50
    private var ttsRate: Float = 1.0f
    private var ttsPitch: Float = 1.0f

    // TTS 队列追踪
    private var pendingTtsCount = 0
    private var ttsStreamDone = false

    // 记忆摘要（in-memory，通话结束即丢弃）
    private var voiceSummaryText: String? = null
    private var voiceSummaryEndIndex: Int = 0

    private companion object {
        private const val TAG = "VoiceChat"
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

        // 监听 TTS API Key 和语速/音量/音高设置
        viewModelScope.launch {
            combine(
                settingsDataStore.ttsApiKey,
                settingsDataStore.ttsVolume,
                settingsDataStore.ttsRate,
                settingsDataStore.ttsPitch
            ) { key, volume, rate, pitch ->
                TtsSettings(key, volume, rate, pitch)
            }.collect { settings ->
                ttsApiKey = settings.key
                ttsVolume = settings.volume
                ttsRate = settings.rate
                ttsPitch = settings.pitch
                if (settings.key.isNotBlank()) {
                    voiceChatManager = VoiceChatManager(context, settings.key)
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
            parameters = com.needai.chat.data.remote.tts.CosyVoiceParameters(
                volume = ttsVolume,
                rate = ttsRate,
                pitch = ttsPitch
            ),
            voiceModelResolver = voiceModelResolver
        )
    }

    fun selectSkill(skillId: String) {
        resetMemory()
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

        // 收集 VAD 说话状态（驱动波形动效）
        viewModelScope.launch {
            manager.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }

        // ASR 中间结果 → 用户正在说话
        manager.setOnPartial { text ->
            _uiState.update {
                it.copy(partialText = text)
            }
        }

        // ASR 完整句子 → 回声过滤/打断/LLM
        manager.setOnText { text ->
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

        resetMemory()
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
                // ★ 记忆压缩检查
                var summaryText = voiceSummaryText
                var summaryEndIdx = voiceSummaryEndIndex
                if (skill.enableMemory) {
                    val history = _uiState.value.conversationHistory
                    val uncompressedEntries = if (summaryEndIdx > 0) {
                        history.drop(summaryEndIdx)
                    } else {
                        history
                    }
                    val estimatedTokens = estimateConversationTokens(
                        systemPrompt = skill.systemPrompt,
                        summary = summaryText,
                        entries = uncompressedEntries,
                        currentInput = userText
                    )
                    if (ContextCompressor.shouldCompress(
                            estimatedTokens, config.contextWindow, uncompressedEntries.size
                        )) {
                        _uiState.update { it.copy(status = "整理记忆中...") }
                        try {
                            val newSummary = compressConversation(history, summaryText)
                            if (newSummary != null) {
                                summaryText = newSummary
                                summaryEndIdx = (history.size * 0.5).toInt().coerceAtLeast(1)
                                voiceSummaryText = summaryText
                                voiceSummaryEndIndex = summaryEndIdx
                            }
                        } catch (_: Exception) { }
                    }
                }

                // 构建消息列表
                val messages = mutableListOf(
                    ChatMessage(role = "system", content = skill.systemPrompt)
                )
                // 用 user 角色注入摘要，避免与 system prompt 抢指令权
                if (summaryText != null) {
                    messages.add(
                        ChatMessage(
                            role = "user",
                            content = "[对话历史摘要]\n$summaryText\n---\n请基于以上上下文继续对话"
                        )
                    )
                }
                val history = _uiState.value.conversationHistory
                for (entry in history.drop(summaryEndIdx)) {
                    messages.add(ChatMessage(role = entry.role, content = entry.text))
                }
                messages.add(ChatMessage(role = "user", content = userText))

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
                FileLogger.d(TAG, "LLM 流被取消（用户打断）")
            } catch (e: Exception) {
                FileLogger.e(TAG, "LLM 异常", e)
                setError("LLM", e.localizedMessage ?: "LLM 请求失败")
            }
        }
    }

    // ===== TTS 处理 =====

    private val ttsSentenceBuffer = StringBuilder()

    /** 找到不在括号内的句子结束位置。右括号 `)` `）` 本身也视为断句点。 */
    private fun findSentenceEnd(text: String): Int {
        var depth = 0
        for (i in text.indices) {
            when (text[i]) {
                '（', '(', '「', '『', '{', '[' -> depth++
                '）', ')' -> {
                    depth--
                    if (depth <= 0) return i  // 闭合括号视为断句
                }
                '」', '』', '}', ']' -> depth--
                '。', '！', '？', '\n', '.', '!', '?' -> if (depth <= 0) return i
            }
        }
        return -1
    }

    private fun appendTtsText(token: String) {
        ttsSentenceBuffer.append(token)
        val text = ttsSentenceBuffer.toString()

        // 括号未闭合时等待，不进行任何切分，避免括号内文本被截断发送给 TTS
        if (hasUnmatchedParen(text)) return

        // 括号已闭合，正常提取完整句子
        var remaining = text
        while (true) {
            val sentenceEnd = findSentenceEnd(remaining)
            if (sentenceEnd > 0) {
                val sentence = remaining.substring(0, sentenceEnd + 1)
                playTtsSentence(sentence)
                remaining = remaining.substring(sentenceEnd + 1)
            } else break
        }
        ttsSentenceBuffer.clear()
        ttsSentenceBuffer.append(remaining)

        // 超过 80 字仍无断句，找最后的逗号/分号切分
        if (remaining.length > 80 && !hasUnmatchedParen(remaining)) {
            val lastBreak = remaining.indexOfLast { it in "，、；,;" }
            val splitAt = if (lastBreak >= remaining.length / 2) lastBreak + 1 else remaining.length
            val overflow = remaining.substring(0, splitAt)
            playTtsSentence(overflow)
            ttsSentenceBuffer.delete(0, splitAt)
        }
    }

    /** 去除 （）() 内的动作/场景描述，只保留正文 */
    private fun stripParenthetical(text: String): String {
        val result = StringBuilder()
        var depth = 0
        for (c in text) {
            when (c) {
                '(', '（' -> depth++
                ')', '）' -> if (depth > 0) depth--
                else -> if (depth == 0) result.append(c)
            }
        }
        return result.toString().trim()
    }

    /** 文本中是否存在未闭合的左括号 */
    private fun hasUnmatchedParen(text: String): Boolean {
        var depth = 0
        for (c in text) {
            when (c) {
                '（', '(' -> depth++
                '）', ')' -> if (depth > 0) depth--
            }
        }
        return depth > 0
    }

    private fun playTtsSentence(sentence: String) {
        val cleanText = stripParenthetical(sentence)
        if (cleanText.isBlank()) return
        val skill = getSelectedSkill()
        val voiceId = skill?.voiceId ?: ""
        pendingTtsCount++
        _uiState.update { it.copy(isTtsPlaying = true) }

        val mgr = ttsManager
        if (mgr is TtsManagerImpl) {
            mgr.speakQueued(cleanText, voiceId = voiceId, onDone = {
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
            val remaining = ttsSentenceBuffer.toString()
            // 只有满足以下任一条件才 flush：
            // 1. 有句尾标点（包括 )）— 是一个完整句子
            // 2. 超过 15 字且末尾是逗号/语气词 — 可能是完整的半句
            // 3. 超过 30 字 — 够长直接读，比丢失好
            val hasEnding = remaining.any { it in "。！？.!?)）" }
            val endsWell = remaining.length >= 15 && remaining.last() in "，、，；"
            if (hasEnding || endsWell || remaining.length >= 30) {
                playTtsSentence(remaining)
            }
            ttsSentenceBuffer.clear()
        }
        ttsStreamDone = true
        checkTtsDone()
    }

    private fun checkTtsDone() {
        if (ttsStreamDone && pendingTtsCount <= 0) {
            ttsStreamDone = false
            _uiState.update { it.copy(status = "聆听中...", isTtsPlaying = false) }
        }
    }

    // ======================================================================
    // 记忆压缩
    // ======================================================================

    /** 估算对话上下文的 token 数（同 ContextCompressor.estimateInputTokens，但适配 ChatEntry） */
    private fun estimateConversationTokens(
        systemPrompt: String,
        summary: String?,
        entries: List<ChatEntry>,
        currentInput: String
    ): Int {
        var total = 0
        total += tokenCount(systemPrompt) + 4
        if (summary != null) total += tokenCount(summary) + 4
        for (entry in entries) total += tokenCount(entry.text) + 4
        total += tokenCount(currentInput) + 4
        return total
    }

    private fun tokenCount(text: String): Int {
        var chinese = 0
        var other = 0
        for (c in text) {
            if (c.code in 0x4E00..0x9FFF || c.code in 0x3400..0x4DBF) {
                chinese++
            } else {
                other++
            }
        }
        return (chinese * 1.5 + other * 0.3).toInt()
    }

    /** 压缩早期对话历史，返回摘要文本 */
    private suspend fun compressConversation(
        history: List<ChatEntry>,
        existingSummary: String?
    ): String? {
        val config = currentConfig ?: return null
        val splitIndex = (history.size * 0.5).toInt().coerceAtLeast(1)
        val toCompress = history.take(splitIndex)

        val input = buildString {
            if (existingSummary != null) {
                appendLine("[上一轮对话摘要]")
                appendLine(existingSummary)
                appendLine()
            }
            appendLine("[待压缩的对话]")
            for (entry in toCompress) {
                val roleLabel = if (entry.role == "user") "用户" else "AI"
                appendLine("$roleLabel: ${entry.text}")
            }
        }

        val compressMessages = listOf(
            ChatMessage("system", ContextCompressor.COMPRESSION_PROMPT),
            ChatMessage("user", input)
        )

        val compressSkill = Skill(
            id = "_compressor",
            name = "Compressor",
            description = "",
            avatar = "",
            systemPrompt = "",
            greeting = "",
            isBuiltin = true
        )

        return try {
            val summary = StringBuilder()
            modelClient.streamChat(compressMessages, config, compressSkill).collect { event ->
                when (event) {
                    is StreamEvent.Token -> summary.append(event.text)
                    is StreamEvent.Done -> { }
                }
            }
            val result = summary.toString().trim()
            if (result.length < 10) null else result.take(500)
        } catch (e: Exception) {
            FileLogger.e("VoiceChat", "记忆压缩失败", e)
            null
        }
    }

    private fun resetMemory() {
        voiceSummaryText = null
        voiceSummaryEndIndex = 0
    }

    override fun onCleared() {
        super.onCleared()
        voiceChatManager?.stop()
        streamingJob?.cancel()
        ttsManager?.shutdown()
    }
}
